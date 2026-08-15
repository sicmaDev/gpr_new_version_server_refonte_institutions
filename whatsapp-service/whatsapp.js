require('dotenv').config();
const path   = require('path');
const fs     = require('fs');
const pino   = require('pino');
const qrLib  = require('qrcode');

const { db }        = require('./db');
const { broadcast } = require('./events');

// Baileys est un module ESM pur (v7+) — le reste du service est en CommonJS,
// donc on le charge via import() dynamique plutôt que de tout convertir en ESM.
let Baileys = null;
async function loadBaileys() {
  if (!Baileys) Baileys = await import('@whiskeysockets/baileys');
  return Baileys;
}

// Secret partagé avec Spring Boot (X-WhatGPR-Secret) — aucune valeur par défaut :
// on préfère un échec au démarrage à une fuite de secret connu committé dans l'historique Git.
const INTERNAL_SECRET = process.env.INTERNAL_SECRET;
if (!INTERNAL_SECRET) {
  console.error('[WA] INTERNAL_SECRET manquant dans .env — obligatoire pour sécuriser les appels vers Spring Boot.');
  process.exit(1);
}

function normalizeJid(jid) {
  if (!jid) return jid;
  return jid.trim().toLowerCase().replace(/@c\.us$/, '@s.whatsapp.net');
}

// ─── État interne ──────────────────────────────────────────────────────────────
let qrCodeBase64      = null;
let waStatus          = 'disconnected';
let connectedNumber   = null; // JID du numéro GPR actuellement connecté (ex: "22952030745@s.whatsapp.net")
let connectingStartAt = null; // timestamp du début de la connexion
let watchdogTimer     = null; // timer de détection de blocage

const STUCK_TIMEOUT_MS = 3 * 60 * 1000; // 3 minutes → force restart automatique

// Mapping en mémoire, clé = numéro de téléphone (JID) du client
// ⚠ Un seul sondage "en attente de réponse" à la fois par numéro : si une 2e solution
// est envoyée au même client avant qu'il ait répondu à la 1re, elle écrase l'entrée
// mémoire de la 1re (qui reste "pending" en base jusqu'au fallback 24h). Limite assumée
// du modèle texte (pas de quiproquo possible autrement puisque la réponse est un simple
// message, sans identifiant technique reliant la réponse à un sondage précis).
const pendingSurveys       = new Map(); // attend la réponse texte au sondage de satisfaction
const pendingComments      = new Map(); // attend le commentaire après confirmation non-satisfait

// Le sondage WhatsApp natif (type "poll") ne peut plus être utilisé de façon fiable :
// Baileys ne déchiffre plus automatiquement les votes en interne, et l'appel manuel à
// decryptPollVote() échoue systématiquement (bug connu et non résolu de la librairie,
// cf. issues WhiskeySockets/Baileys #2158 et #1344). On remplace donc le sondage natif
// par une question texte à choix numérotés — la réponse du client arrive comme un
// message texte ordinaire, sans aucun chiffrement spécifique à gérer.
const SURVEY_QUESTION_TEXT =
  '━━━━━━━━━━━━━━━\n' +
  '*Êtes-vous satisfait(e) de cette solution ?*\n' +
  '━━━━━━━━━━━━━━━\n\n' +
  '😊  *1* — Satisfait\n' +
  '😐  *2* — Partiellement Satisfait\n' +
  '😕  *3* — Non Satisfait\n\n' +
  '━━━━━━━━━━━━━━━\n' +
  '✍️ _Répondez avec le numéro ou l\'émoji de votre choix._';

/** Analyse la réponse texte du client et retourne SATISFIED / PARTIAL / UNSATISFIED, ou null si non reconnue. */
function matchSurveyReply(rawText) {
  if (!rawText) return null;
  const text = rawText.trim().toLowerCase();
  // Ordre important : les formulations les plus spécifiques d'abord, pour éviter
  // qu'un "non satisfait" ou "partiellement satisfait" ne matche "satisfait" en premier.
  if (/\b3\b/.test(text) || text.includes('😕') || text.includes('non satisfait') || text.includes('pas satisfait') || text.includes('insatisfait')) {
    return 'UNSATISFIED';
  }
  if (/\b2\b/.test(text) || text.includes('😐') || text.includes('partiel')) {
    return 'PARTIAL';
  }
  if (/\b1\b/.test(text) || text.includes('😊') || text.includes('satisfait')) {
    return 'SATISFIED';
  }
  return null;
}

const UPLOADS_DIR  = process.env.UPLOADS_DIR || path.join(__dirname, 'uploads');
const SURVEYS_DIR  = path.join(__dirname, 'surveys');
const COMMENTS_DIR = path.join(__dirname, 'comments');
const QUEUE_DIR    = path.join(__dirname, 'send_queue');
const AUTH_DIR     = path.join(__dirname, 'baileys_auth');
if (!fs.existsSync(UPLOADS_DIR))  fs.mkdirSync(UPLOADS_DIR, { recursive: true });
if (!fs.existsSync(SURVEYS_DIR))  fs.mkdirSync(SURVEYS_DIR, { recursive: true });
if (!fs.existsSync(COMMENTS_DIR)) fs.mkdirSync(COMMENTS_DIR, { recursive: true });
if (!fs.existsSync(QUEUE_DIR))    fs.mkdirSync(QUEUE_DIR, { recursive: true });

// ─── File d'attente des envois — messages texte simples uniquement ───────────
// (accusé de réception, message manuel) : si WhatsApp est déconnecté au moment
// de l'envoi, l'appel échoue immédiatement (le frontend/Spring Boot reçoit bien
// l'erreur), MAIS le message est aussi mis en file et renvoyé automatiquement
// dès la reconnexion — sans ça, un accusé de réception perdu pendant une
// déconnexion transitoire ne partait jamais.
const QUEUE_MAX_ATTEMPTS = 20;
const QUEUE_MAX_AGE_MS   = 48 * 60 * 60 * 1000; // 48h

function enqueueSend(task) {
  try {
    const id = `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
    const file = path.join(QUEUE_DIR, id + '.json');
    fs.writeFileSync(file, JSON.stringify({ ...task, id, attempts: 0, createdAt: Date.now() }));
    console.log(`[WA] Message mis en file d'attente (${task.type}) → ${task.to}`);
  } catch (err) {
    console.error('[WA] Erreur mise en file d\'attente:', err.message);
  }
}

async function flushSendQueue() {
  if (waStatus !== 'connected' || !sock) return;
  let files;
  try { files = fs.readdirSync(QUEUE_DIR).filter(f => f.endsWith('.json')); }
  catch (_) { return; }
  if (!files.length) return;

  for (const file of files) {
    const filePath = path.join(QUEUE_DIR, file);
    let task;
    try { task = JSON.parse(fs.readFileSync(filePath, 'utf8')); }
    catch (_) { try { fs.unlinkSync(filePath); } catch (_) {} continue; }

    if (Date.now() - task.createdAt > QUEUE_MAX_AGE_MS || task.attempts >= QUEUE_MAX_ATTEMPTS) {
      console.log(`[WA] Message en file abandonné (trop ancien/tentatives) → ${task.to}`);
      try { fs.unlinkSync(filePath); } catch (_) {}
      continue;
    }

    try {
      await sock.sendMessage(task.to, { text: task.message });
      console.log(`[WA] Message en file envoyé avec succès → ${task.to}`);
      fs.unlinkSync(filePath);
    } catch (err) {
      task.attempts += 1;
      try { fs.writeFileSync(filePath, JSON.stringify(task)); } catch (_) {}
      console.error(`[WA] Échec renvoi message en file (tentative ${task.attempts}) → ${task.to} :`, err.message);
    }
  }
}

// ─── Persistance des sondages sur disque ──────────────────────────────────────
// Survie aux redémarrages PM2 : un fichier par numéro (le sondage étant maintenant
// une simple question texte, plus besoin de clé de chiffrement à conserver).
function _surveyFile(phone) {
  return path.join(SURVEYS_DIR, phone.replace(/[^a-z0-9_-]/gi, '_') + '.json');
}
function saveSurveyToDisk(phone, data) {
  try {
    fs.writeFileSync(_surveyFile(phone), JSON.stringify(data));
  } catch (_) {}
}
function deleteSurveyFromDisk(phone) {
  try {
    const f = _surveyFile(phone);
    if (fs.existsSync(f)) fs.unlinkSync(f);
  } catch (_) {}
}
function loadSurveyFromDisk(phone) {
  try {
    const f = _surveyFile(phone);
    if (!fs.existsSync(f)) return null;
    return JSON.parse(fs.readFileSync(f, 'utf8'));
  } catch (_) { return null; }
}

// ─── Persistance de l'attente de commentaire sur disque ───────────────────────
// Même besoin que pour les sondages : après un vote PARTIAL/UNSATISFIED, le client
// est invité à commenter (texte ou vocal). Si le process redémarre avant sa réponse,
// pendingComments (mémoire) est perdu — ce filet évite que son commentaire arrive
// ensuite comme un message ordinaire, jamais rattaché à sa réclamation.
function _commentFile(phone) {
  return path.join(COMMENTS_DIR, phone.replace(/[^a-z0-9_-]/gi, '_') + '.json');
}
function saveCommentWaitToDisk(phone, data) {
  try {
    fs.writeFileSync(_commentFile(phone), JSON.stringify(data));
  } catch (_) {}
}
function deleteCommentWaitFromDisk(phone) {
  try {
    const f = _commentFile(phone);
    if (fs.existsSync(f)) fs.unlinkSync(f);
  } catch (_) {}
}
function loadCommentWaitFromDisk(phone) {
  try {
    const f = _commentFile(phone);
    if (!fs.existsSync(f)) return null;
    return JSON.parse(fs.readFileSync(f, 'utf8'));
  } catch (_) { return null; }
}

const VALID_TYPES = ['chat', 'image', 'video', 'audio', 'ptt', 'document', 'sticker'];

// ─── Client Baileys (variable réassignable après reconnexion) ────────────────
let sock = null;
let saveCredsFn = null;
const logger = pino({ level: process.env.BAILEYS_LOG_LEVEL || 'silent' });

// ─── Verrou anti-concurrence ──────────────────────────────────────────────────
// Plusieurs chemins de code (reconnexion auto après coupure, watchdog, déconnexion
// manuelle) peuvent chacun vouloir (re)créer le client. Sans verrou, deux
// createClient() concurrents peuvent écrire creds.json en même temps (corruption)
// ou faire tourner deux sockets en parallèle (messages traités en double).
let reconnectTimer = null; // un seul redémarrage programmé à la fois
let clientStarting = false; // verrou : empêche deux createClient() simultanés
let abortStartup   = false; // demande d'annulation reçue pendant un démarrage en cours

/** Programme un (re)démarrage unique — annule automatiquement tout redémarrage déjà en attente. */
function scheduleReconnect(delayMs, label) {
  if (reconnectTimer) clearTimeout(reconnectTimer);
  reconnectTimer = setTimeout(() => {
    reconnectTimer = null;
    connectingStartAt = Date.now();
    startWatchdog();
    createClient().catch(err => console.error(`[WA] Erreur reconnexion (${label}):`, err.message));
  }, delayMs);
}

/** Ferme proprement le socket courant (listeners + connexion) avant d'en ouvrir un nouveau. */
function teardownSocket() {
  if (sock) {
    try { sock.ev.removeAllListeners(); } catch (_) {}
    try { sock.end(undefined); } catch (_) {}
    sock = null;
  }
}

/**
 * Convertit le contenu d'un message Baileys vers le vocabulaire interne
 * (chat | image | video | audio | ptt | document | sticker), plus le texte/légende.
 */
function extractMessageInfo(message, contentType) {
  const m = message[contentType];
  switch (contentType) {
    case 'conversation':
      return { type: 'chat', body: message.conversation, hasMedia: false };
    case 'extendedTextMessage':
      return { type: 'chat', body: m.text, hasMedia: false };
    case 'imageMessage':
      return { type: 'image', body: m.caption || null, hasMedia: true, mimetype: m.mimetype };
    case 'videoMessage':
      return { type: 'video', body: m.caption || null, hasMedia: true, mimetype: m.mimetype };
    case 'audioMessage':
      return { type: m.ptt ? 'ptt' : 'audio', body: null, hasMedia: true, mimetype: m.mimetype };
    case 'documentMessage':
      return { type: 'document', body: m.fileName || null, hasMedia: true, mimetype: m.mimetype };
    case 'stickerMessage':
      return { type: 'sticker', body: null, hasMedia: true, mimetype: m.mimetype };
    case 'locationMessage': {
      const coords = `${m.degreesLatitude}, ${m.degreesLongitude}`;
      const label  = m.name ? `${m.name} — ${coords}` : coords;
      return { type: 'chat', body: `📍 Localisation partagée : ${label}`, hasMedia: false };
    }
    case 'contactMessage':
      return { type: 'chat', body: `👤 Contact partagé : ${m.displayName || 'Contact'}`, hasMedia: false };
    case 'contactsArrayMessage': {
      const names = (m.contacts || []).map(c => c.displayName).filter(Boolean).join(', ');
      return { type: 'chat', body: `👤 Contacts partagés : ${names || m.displayName || ''}`, hasMedia: false };
    }
    default:
      return null; // type non géré (réaction, statut de protocole, etc.) → ignoré
  }
}

/**
 * Crée une nouvelle instance de socket Baileys et y attache tous les handlers.
 * Appelé au démarrage ET après chaque reconnexion.
 */
async function createClient() {
  if (clientStarting) {
    console.log('[WA] createClient() déjà en cours — appel ignoré (évite un double socket).');
    return;
  }
  clientStarting = true;
  abortStartup   = false;
  try {
    const {
      makeWASocket, useMultiFileAuthState, DisconnectReason,
      getContentType, isJidGroup, isJidStatusBroadcast, isJidNewsletter, downloadMediaMessage,
      normalizeMessageContent,
    } = await loadBaileys();

    teardownSocket(); // ferme un éventuel socket précédent avant d'en créer un nouveau

    const { state, saveCreds } = await useMultiFileAuthState(AUTH_DIR);

    if (abortStartup) {
      console.log('[WA] Démarrage annulé (déconnexion/redémarrage demandé entre-temps).');
      return;
    }

    saveCredsFn = saveCreds;

    sock = makeWASocket({
      auth: state,
      logger,
      browser: ['WhatGPR', 'Chrome', '1.0.0'],
      syncFullHistory: false,
    });

    sock.ev.on('creds.update', saveCreds);

  // ── Connexion / QR / déconnexion ───────────────────────────────────────────
  sock.ev.on('connection.update', async (update) => {
    const { connection, lastDisconnect, qr } = update;

    if (qr) {
      waStatus     = 'qr_pending';
      qrCodeBase64 = await qrLib.toDataURL(qr);
      broadcast('wa_status', { status: waStatus });
      console.log('[WA] QR prêt à scanner.');
    }

    if (connection === 'connecting') {
      if (waStatus !== 'connected') {
        waStatus          = 'connecting';
        connectingStartAt = Date.now();
        broadcast('wa_status', { status: waStatus });
        startWatchdog();
      }
    }

    if (connection === 'open') {
      stopWatchdog();
      startLivenessWatchdog();
      waStatus     = 'connected';
      qrCodeBase64 = null;
      try {
        const jid = sock.user?.id;
        if (jid) {
          const digits = jid.replace(/\D/g, '');
          connectedNumber = digits ? digits + '@s.whatsapp.net' : jid;
          console.log(`[WA] Numéro connecté : ${connectedNumber}`);
        }
      } catch (_) {}
      broadcast('wa_status', { status: waStatus });
      console.log('[WA] Client prêt.');
      setTimeout(flushSendQueue, 5000);
      broadcast('sync_progress', { current: 1, total: 1, messages: 0, done: true });
    }

    if (connection === 'close') {
      stopLivenessWatchdog();
      const statusCode = lastDisconnect?.error?.output?.statusCode;

      waStatus        = 'disconnected';
      qrCodeBase64    = null;
      connectedNumber = null;
      broadcast('wa_status', { status: waStatus, reason: lastDisconnect?.error?.message });

      // Session invalide (identifiants corrompus ou révoqués) : retenter avec les mêmes
      // creds échouerait indéfiniment — il faut effacer la session et rescanner un QR.
      const sessionInvalid = statusCode === DisconnectReason.loggedOut
        || statusCode === DisconnectReason.badSession
        || statusCode === DisconnectReason.multideviceMismatch;

      if (sessionInvalid) {
        console.log('[WA] Déconnecté (code', statusCode, ') → session invalide, QR requis.');
        try { fs.rmSync(AUTH_DIR, { recursive: true, force: true }); } catch (_) {}
        return; // pas de reconnexion auto
      }

      // Une autre session (téléphone, autre appareil) a pris le relais : retenter tout de
      // suite ne ferait que reproduire le conflit en boucle — on attend plus longtemps.
      if (statusCode === DisconnectReason.connectionReplaced) {
        console.log('[WA] Déconnecté (code', statusCode, ') → session remplacée par un autre appareil, nouvelle tentative dans 60s.');
        scheduleReconnect(60000, 'session remplacée');
        return;
      }

      // restartRequired est une étape normale attendue par Baileys (ex. juste après le
      // scan d'un QR) — reconnexion rapide, pas besoin d'attendre.
      const delay = statusCode === DisconnectReason.restartRequired ? 1000 : 5000;
      console.log('[WA] Déconnecté (code', statusCode, ') → reconnexion interne dans', delay / 1000, 's...');
      scheduleReconnect(delay, 'reconnexion interne');
    }
  });

  // ── Messages entrants + votes de sondage ───────────────────────────────────
  sock.ev.on('messages.upsert', async ({ messages, type }) => {
    for (const msg of messages) {
      try {
        if (!msg.message) continue;
        const remoteJid = msg.key.remoteJid || '';
        if (!remoteJid || isJidGroup(remoteJid) || isJidStatusBroadcast(remoteJid) || isJidNewsletter(remoteJid)) continue;

        // Déballe les messages éphémères/vue unique/édités pour accéder au vrai contenu
        // (sans ça, une photo envoyée en "vue unique" par exemple n'était jamais reçue).
        const normalizedMessage = normalizeMessageContent(msg.message) || msg.message;
        const contentType = getContentType(normalizedMessage);

        if (msg.key.fromMe) continue; // on ne garde que les messages reçus
        await handleIncomingMessage({ ...msg, message: normalizedMessage }, contentType, downloadMediaMessage);
      } catch (err) {
        console.error('[WA] Erreur traitement message entrant:', err.message);
      }
    }
  });

    // ── Rattrapage des messages livrés non déchiffrés ───────────────────────
    // Quand un message arrive avant que la clé de session Signal soit prête (typiquement
    // juste après une connexion/reconnexion), Baileys le délivre d'abord via messages.upsert
    // avec message: null (ignoré ci-dessus), puis renvoie le contenu déchiffré via
    // messages.update quelques instants plus tard. Sans ce listener, ces messages étaient
    // perdus définitivement — jamais insérés puisque le premier passage était vide.
    sock.ev.on('messages.update', async (updates) => {
      for (const { key, update } of updates) {
        try {
          if (!update.message) continue; // pas de contenu déchiffré dans cette mise à jour
          if (key.fromMe) continue;
          const remoteJid = key.remoteJid || '';
          if (!remoteJid || isJidGroup(remoteJid) || isJidStatusBroadcast(remoteJid) || isJidNewsletter(remoteJid)) continue;

          const normalizedMessage = normalizeMessageContent(update.message) || update.message;
          const contentType = getContentType(normalizedMessage);

          const reconstructed = {
            key,
            message: normalizedMessage,
            messageTimestamp: update.messageTimestamp || Math.floor(Date.now() / 1000),
            pushName: update.pushName,
            senderPn: update.senderPn,
          };
          console.log('[WA] Message rattrapé via messages.update (déchiffrement tardif) :', key.id);
          await handleIncomingMessage(reconstructed, contentType, downloadMediaMessage);
        } catch (err) {
          console.error('[WA] Erreur traitement messages.update:', err.message);
        }
      }
    });

    return sock;
  } finally {
    clientStarting = false;
  }
}

// ─── Gestion des messages entrants (live) ────────────────────────────────────
async function handleIncomingMessage(msg, contentType, downloadMediaMessage) {
  try {
    const chatId = msg.key.remoteJid || '';
    const info   = extractMessageInfo(msg.message, contentType);
    if (!info || !VALID_TYPES.includes(info.type)) return; // type non géré (réaction, etc.)

    // Extraire le numéro de téléphone réel (JID PN) à la place du LID anonyme
    let fromNumber = chatId;
    if (chatId.endsWith('@lid')) {
      if (msg.senderPn && (msg.senderPn.endsWith('@s.whatsapp.net') || /^\d+$/.test(msg.senderPn.trim()))) {
        fromNumber = msg.senderPn.trim();
      } else if (msg.key.remoteJidAlt && (msg.key.remoteJidAlt.endsWith('@s.whatsapp.net') || /^\d+$/.test(msg.key.remoteJidAlt.trim()))) {
        fromNumber = msg.key.remoteJidAlt.trim();
      }
      if (!fromNumber.endsWith('@s.whatsapp.net') && /^\d+$/.test(fromNumber)) {
        fromNumber += '@s.whatsapp.net';
      }
    }

    const fromName    = msg.pushName || null;

    // ── Étape : réponse au sondage de satisfaction (texte à choix numérotés) ───
    // Remplace l'ancien sondage natif WhatsApp (poll), dont le vote ne peut plus être
    // déchiffré de façon fiable par Baileys (voir SURVEY_QUESTION_TEXT plus haut).
    // pendingSurveys stocke une LISTE par numéro (pas un seul sondage) : un même client
    // peut avoir plusieurs réclamations traitées en parallèle, chacune avec son propre
    // sondage envoyé. Sans liste, le 2e envoi écrasait le suivi du 1er, qui restait
    // bloqué "pending" en base indéfiniment, sans aucun rattrapage possible.
    let surveyList = pendingSurveys.get(fromNumber);
    if (!surveyList) {
      surveyList = loadSurveyFromDisk(fromNumber);
      if (surveyList) {
        console.log('[WA] Sondage(s) en attente retrouvé(s) sur disque après redémarrage — phone:', fromNumber);
      }
    }
    if (surveyList && surveyList.length && info.type === 'chat' && info.body?.trim()) {
      const satisfactionStatus = matchSurveyReply(info.body);
      if (satisfactionStatus) {
        // On résout le sondage le plus récemment envoyé (dernier de la liste) — c'est
        // celui le plus probablement visé par une réponse ambiguë sans référence explicite.
        // Les autres restent en attente, résolus par une prochaine réponse du client.
        const surveyData = surveyList[surveyList.length - 1];
        const remaining  = surveyList.slice(0, -1);
        if (remaining.length) {
          pendingSurveys.set(fromNumber, remaining);
          saveSurveyToDisk(fromNumber, remaining);
        } else {
          pendingSurveys.delete(fromNumber);
          deleteSurveyFromDisk(fromNumber);
        }
        await db.query(
          `UPDATE wgpr_whatsapp_surveys SET status = 'voted', satisfaction = ?, voted_at = NOW()
           WHERE phone = ? AND survey_message_id = ? AND status = 'pending'`,
          [satisfactionStatus, fromNumber, surveyData.surveyMessageId]
        );
        if (satisfactionStatus === 'SATISFIED') {
          await callSpringBootMeasure(surveyData.claimId, surveyData.solutionId, surveyData.measurerId, 'SATISFIED', null);
          await db.query(
            `UPDATE wgpr_whatsapp_surveys SET status = 'completed'
             WHERE phone = ? AND survey_message_id = ? AND satisfaction = 'SATISFIED'`,
            [fromNumber, surveyData.surveyMessageId]
          );
          if (surveyData.audio3) {
            await sendAudioBuffer(fromNumber, surveyData.audio3, surveyData.mimeType);
          } else {
            await simulateTyping(fromNumber, 'composing');
            await sock.sendMessage(fromNumber, { text: '✅ Merci pour votre retour ! Nous sommes heureux d\'avoir pu vous satisfaire. 😊' });
          }
          broadcast('survey_response', { voter: fromNumber, satisfactionStatus: 'SATISFIED' });
        } else {
          const commentWaitData = { ...surveyData, satisfactionStatus };
          const existingComments = pendingComments.get(fromNumber) || [];
          const updatedComments  = [...existingComments, commentWaitData];
          pendingComments.set(fromNumber, updatedComments);
          saveCommentWaitToDisk(fromNumber, updatedComments);
          if (surveyData.audio2) {
            await sendAudioBuffer(fromNumber, surveyData.audio2, surveyData.mimeType);
          } else {
            await simulateTyping(fromNumber, 'composing');
            await sock.sendMessage(fromNumber, {
              text: `Nous en prenons note. 🙏\n\nPourriez-vous nous expliquer la raison de votre insatisfaction ?\n\n🎙 Envoyez un *message vocal*\n✍️ Ou tapez votre *commentaire écrit*`,
            });
          }
        }
        return;
      }
      // Réponse non reconnue (ex: "bonjour") → on relance avec un rappel des options,
      // au lieu de stocker silencieusement ce message comme une conversation normale.
      await simulateTyping(fromNumber, 'composing');
      await sock.sendMessage(fromNumber, {
        text: 'Je n\'ai pas compris votre réponse. 🙏\n\n' +
          'Merci de répondre uniquement avec le numéro ou l\'émoji correspondant :\n\n' +
          '😊 *1* — Satisfait\n' +
          '😐 *2* — Partiellement Satisfait\n' +
          '😕 *3* — Non Satisfait',
      });
      return;
    }

    // ── Étape : commentaire après vote (insatisfaction/partiel) ────────────────
    // Fallback disque si le process a redémarré entre le vote et la réponse du client
    // (pendingComments est vidé à chaque redémarrage/déconnexion, voir forceRestart/disconnect).
    // Liste par numéro, même raison que pour pendingSurveys : plusieurs réclamations du
    // même client peuvent chacune attendre un commentaire en parallèle.
    let commentList = pendingComments.get(fromNumber);
    if (!commentList) {
      commentList = loadCommentWaitFromDisk(fromNumber);
      if (commentList) {
        console.log('[WA] Attente(s) de commentaire retrouvée(s) sur disque après redémarrage — phone:', fromNumber);
      }
    }

    const takeMostRecentComment = () => {
      const commentData = commentList[commentList.length - 1];
      const remaining    = commentList.slice(0, -1);
      if (remaining.length) {
        pendingComments.set(fromNumber, remaining);
        saveCommentWaitToDisk(fromNumber, remaining);
      } else {
        pendingComments.delete(fromNumber);
        deleteCommentWaitFromDisk(fromNumber);
      }
      return commentData;
    };

    if (commentList && commentList.length && ['ptt', 'audio'].includes(info.type) && info.hasMedia) {
      const commentData = takeMostRecentComment();
      try {
        const buffer = await downloadMediaMessage(msg, 'buffer', {}, { logger, reuploadRequest: sock.updateMediaMessage });
        const base64 = buffer.toString('base64');
        await callSpringBootMeasureAudio(
          commentData.claimId, commentData.solutionId, commentData.measurerId,
          commentData.satisfactionStatus, base64, info.mimetype || 'audio/ogg; codecs=opus'
        );
        const audioDate = new Date(Number(msg.messageTimestamp) * 1000)
          .toLocaleString('fr-FR', { dateStyle: 'short', timeStyle: 'short' });
        await db.query(
          `UPDATE wgpr_whatsapp_surveys SET status = 'completed', commentaire = ?
           WHERE phone = ? AND survey_message_id = ? AND satisfaction = ?`,
          [`[audio - ${audioDate}]`, fromNumber, commentData.surveyMessageId, commentData.satisfactionStatus]
        );
        if (commentData.audio4) {
          await sendAudioBuffer(fromNumber, commentData.audio4, commentData.mimeType);
        } else {
          await simulateTyping(fromNumber, 'composing');
          await sock.sendMessage(fromNumber, { text: '✅ Merci pour votre message vocal, il a bien été enregistré. Nous ferons le nécessaire pour nous améliorer.' });
        }
        broadcast('survey_response', { voter: fromNumber, satisfactionStatus: commentData.satisfactionStatus, commentaire: '[audio]' });
      } catch (err) {
        console.error('[WA] Erreur commentaire audio:', err.message);
        await simulateTyping(fromNumber, 'composing');
        await sock.sendMessage(fromNumber, { text: 'Une erreur est survenue lors de l\'enregistrement. Veuillez réessayer.' });
      }
      return;
    }

    if (commentList && commentList.length && info.type === 'chat' && info.body?.trim()) {
      const commentData = takeMostRecentComment();
      // Le préfixe [WhatsApp] est déjà ajouté côté Java (WgprController.internalMeasure)
      // pour la détection front-end — ne pas re-tagger ici, sinon double marquage.
      const commentaire = info.body.trim();
      await callSpringBootMeasure(
        commentData.claimId, commentData.solutionId, commentData.measurerId,
        commentData.satisfactionStatus, commentaire
      );
      await db.query(
        `UPDATE wgpr_whatsapp_surveys SET status = 'completed', commentaire = ?
         WHERE phone = ? AND survey_message_id = ? AND satisfaction = ?`,
        [commentaire, fromNumber, commentData.surveyMessageId, commentData.satisfactionStatus]
      );
      if (commentData.audio4) {
        await sendAudioBuffer(fromNumber, commentData.audio4, commentData.mimeType);
      } else {
        await simulateTyping(fromNumber, 'composing');
        await sock.sendMessage(fromNumber, { text: '✅ Merci pour votre commentaire, il a bien été enregistré. Nous ferons le nécessaire pour nous améliorer.' });
      }
      broadcast('survey_response', { voter: fromNumber, satisfactionStatus: commentData.satisfactionStatus, commentaire });
      return;
    }

    // ── Message normal → stockage + diffusion ──────────────────────────────────
    const waId      = msg.key.id;
    if (!waId) return; // pas d'identifiant exploitable, on ignore plutôt que de planter l'insertion SQL
    const timestamp = Number(msg.messageTimestamp || 0) * 1000;
    let   body      = info.body;
    let   mediaType = null;
    let   mediaPath = null;

    if (info.hasMedia) {
      try {
        const buffer   = await downloadMediaMessage(msg, 'buffer', {}, { logger, reuploadRequest: sock.updateMediaMessage });
        const ext      = (info.mimetype || '').split('/')[1]?.split(';')[0] || 'bin';
        const filename = `${Date.now()}-${waId.replace(/[^a-z0-9]/gi, '_')}.${ext}`;
        fs.writeFileSync(path.join(UPLOADS_DIR, filename), buffer);
        mediaType = info.mimetype;
        mediaPath = filename;
      } catch (e) {
        console.error('[WA] Erreur média:', e.message);
      }
    }

    // ON DUPLICATE KEY UPDATE met à jour tout le contenu (pas seulement connected_number) :
    // wa_message_id n'est garanti unique que par conversation chez Baileys, pas globalement.
    // En cas de collision, on ne veut jamais perdre silencieusement le contenu du nouveau
    // message. status est volontairement exclu de la mise à jour : si un agent a déjà
    // marqué ce message lu/converti, une redélivrance ne doit pas effacer cette progression.
    await db.query(
      `INSERT INTO wgpr_messages
         (wa_message_id, from_number, from_name, body, wa_type, media_type, media_path, timestamp, status, connected_number)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'new', ?)
       ON DUPLICATE KEY UPDATE
         from_number      = VALUES(from_number),
         from_name        = VALUES(from_name),
         body             = VALUES(body),
         wa_type          = VALUES(wa_type),
         media_type       = VALUES(media_type),
         media_path       = VALUES(media_path),
         timestamp        = VALUES(timestamp),
         connected_number = IF(connected_number IS NULL, VALUES(connected_number), connected_number)`,
      [waId, fromNumber, fromName, body, info.type, mediaType, mediaPath, timestamp, connectedNumber]
    );

    const [rows] = await db.query('SELECT id FROM wgpr_messages WHERE wa_message_id = ?', [waId]);
    broadcast('new_message', {
      id: rows[0]?.id, from_number: fromNumber, from_name: fromName,
      message_id: waId, content: body, type: info.type,
      media_type: mediaType, media_path: mediaPath, timestamp, read: false, status: 'new',
      connected_number: connectedNumber,
    });

  } catch (err) {
    console.error('[WA] Erreur handleIncomingMessage:', err.message);
  }
}

// Simule la présence "en train d'écrire..." / "enregistrement audio..." juste avant un envoi,
// pour donner l'impression que le numéro connecté à GPR rédige réellement le message — comme
// quand on discute avec quelqu'un qui est en ligne. Jamais bloquant : un échec de présence
// (ex: client hors ligne) ne doit jamais empêcher l'envoi du message réel qui suit.
async function simulateTyping(jid, kind = 'composing', durationMs) {
  try {
    const delay = durationMs ?? (2500 + Math.random() * 2000); // ~2.5–4.5s, variable pour paraître naturel
    // Indispensable : sans s'être abonné à la présence du contact au préalable, WhatsApp
    // ignore silencieusement les sendPresenceUpdate("composing"/"recording") suivants —
    // le client ne voit jamais "en train d'écrire..." (piège connu de Baileys).
    await sock.presenceSubscribe(jid);
    await sock.sendPresenceUpdate(kind, jid);
    await new Promise((resolve) => setTimeout(resolve, delay));
    await sock.sendPresenceUpdate('paused', jid);
  } catch (err) {
    console.warn('[WA] simulateTyping a échoué (ignoré) :', err.message);
  }
}

// Envoie un buffer audio base64 en note vocale (helper commun)
async function sendAudioBuffer(to, base64raw, mimeType) {
  const b64 = base64raw.includes(',') ? base64raw.split(',')[1] : base64raw;
  await simulateTyping(to, 'recording');
  await sock.sendMessage(to, {
    audio: Buffer.from(b64, 'base64'),
    mimetype: mimeType || 'audio/ogg; codecs=opus',
    ptt: true,
  });
}

// ─── Watchdog : détecte les connexions bloquées (ex: réveil de veille) ────────
function startWatchdog() {
  stopWatchdog();
  watchdogTimer = setTimeout(async () => {
    if (waStatus !== 'connected') {
      console.log('[WA] Watchdog : connexion bloquée depuis >3min → force restart');
      broadcast('wa_status', { status: 'disconnected', reason: 'watchdog_timeout' });
      await forceRestart();
    }
  }, STUCK_TIMEOUT_MS);
}

function stopWatchdog() {
  if (watchdogTimer) { clearTimeout(watchdogTimer); watchdogTimer = null; }
  connectingStartAt = null;
}

// ─── Watchdog de vivacité : détecte une connexion "zombie" une fois connecté ──
// Le watchdog ci-dessus ne surveille que la phase de connexion. Une fois waStatus
// === 'connected', rien ne vérifiait plus jamais que le socket répond réellement.
// Si le réseau coupe sans fermeture propre (pas de FIN/RST), la connexion reste
// affichée "connectée" indéfiniment alors que plus aucun message n'arrive.
const LIVENESS_CHECK_INTERVAL_MS = 5 * 60 * 1000; // sonde toutes les 5 min
const LIVENESS_PROBE_TIMEOUT_MS  = 15 * 1000;      // 15s pour répondre
const LIVENESS_MAX_FAILURES      = 2;              // 2 échecs consécutifs → zombie confirmé
let livenessTimer       = null;
let livenessFailCount   = 0;

function startLivenessWatchdog() {
  stopLivenessWatchdog();
  livenessFailCount = 0;
  livenessTimer = setInterval(async () => {
    if (waStatus !== 'connected' || !sock) return;
    try {
      await Promise.race([
        sock.sendPresenceUpdate('available'),
        new Promise((_, reject) => setTimeout(() => reject(new Error('timeout sonde de vie')), LIVENESS_PROBE_TIMEOUT_MS)),
      ]);
      livenessFailCount = 0;
    } catch (err) {
      livenessFailCount += 1;
      console.warn(`[WA] Sonde de vie échouée (${livenessFailCount}/${LIVENESS_MAX_FAILURES}) :`, err.message);
      if (livenessFailCount >= LIVENESS_MAX_FAILURES) {
        console.error('[WA] Connexion zombie détectée (sondes de vie répétées en échec) → force restart.');
        broadcast('wa_status', { status: 'disconnected', reason: 'zombie_connection' });
        livenessFailCount = 0;
        await forceRestart();
      }
    }
  }, LIVENESS_CHECK_INTERVAL_MS);
}

function stopLivenessWatchdog() {
  if (livenessTimer) { clearInterval(livenessTimer); livenessTimer = null; }
  livenessFailCount = 0;
}

// Force la réinitialisation propre (appelé par watchdog ou par l'API)
async function forceRestart(clearSession = false) {
  console.log('[WA] Force restart en cours...' + (clearSession ? ' (session effacée)' : ''));
  abortStartup = true; // annule un éventuel createClient() en cours de démarrage
  stopLivenessWatchdog();
  teardownSocket();

  if (clearSession) {
    try {
      if (fs.existsSync(AUTH_DIR)) {
        fs.rmSync(AUTH_DIR, { recursive: true, force: true });
        console.log('[WA] Session supprimée — QR requis.');
      }
    } catch (_) {}
  }

  waStatus        = 'disconnected';
  qrCodeBase64    = null;
  connectedNumber = null;
  pendingSurveys.clear();
  pendingComments.clear();
  scheduleReconnect(2000, 'force restart');
}

// ─── API publique ──────────────────────────────────────────────────────────────
function getQrCode() { return qrCodeBase64; }
function getStatus() { return waStatus; }
function getConnectedNumber() { return connectedNumber; }

async function disconnect() {
  console.log('[WA] Déconnexion manuelle...');
  abortStartup = true; // annule un éventuel createClient() en cours de démarrage
  stopLivenessWatchdog();
  try { if (sock) await sock.logout(); } catch (_) {}
  teardownSocket();
  try { if (fs.existsSync(AUTH_DIR)) fs.rmSync(AUTH_DIR, { recursive: true, force: true }); } catch (_) {}

  waStatus        = 'disconnected';
  qrCodeBase64    = null;
  connectedNumber = null;
  pendingSurveys.clear();
  pendingComments.clear();
  broadcast('wa_status', { status: 'disconnected' });
  console.log('[WA] Déconnecté manuellement.');

  scheduleReconnect(2000, 'réinitialisation après déconnexion manuelle');
}

async function sendMessage(to, message) {
  const jid = normalizeJid(to);
  if (waStatus !== 'connected') {
    enqueueSend({ type: 'message', to: jid, message });
    throw new Error('WhatsApp non connecté — le message sera renvoyé automatiquement à la reconnexion.');
  }
  try {
    await simulateTyping(jid, 'composing');
    await sock.sendMessage(jid, { text: message });
  } catch (err) {
    enqueueSend({ type: 'message', to: jid, message });
    throw err;
  }
}

// Envoie une note vocale simple (pas de sondage) — ex: solution proposée à l'oral au traitement
async function sendAudio(to, audioBase64, mimeType) {
  const jid = normalizeJid(to);
  if (waStatus !== 'connected') throw new Error('WhatsApp non connecté');
  await sendAudioBuffer(jid, audioBase64, mimeType);
}

// Envoie le message de confirmation + la question de satisfaction (texte) au client
// surveyAudioBase64 (facultatif) : note vocale de l'agent présentant le sondage au client
// dans sa langue — utile pour les clients ne pouvant pas lire le texte.
async function sendSolutionWithSurvey(to, solutionMessage, claimId, solutionId, measurerId, surveyAudioBase64, surveyAudioMimeType) {
  const jid = normalizeJid(to);
  if (waStatus !== 'connected') throw new Error('WhatsApp non connecté');

  await simulateTyping(jid, 'composing');
  await sock.sendMessage(jid,
    { text: `✅ *Votre réclamation a été traitée.*\n\n📋 *Solution apportée :*\n${solutionMessage}\n\nMerci de votre confiance.` }
  );

  if (surveyAudioBase64) {
    await sendAudioBuffer(jid, surveyAudioBase64, surveyAudioMimeType);
  }

  await simulateTyping(jid, 'composing');
  const questionMsg = await sock.sendMessage(jid, { text: SURVEY_QUESTION_TEXT });
  const surveyMessageId = questionMsg.key.id;

  const textSurveyData = { claimId, solutionId, measurerId, surveyMessageId, solutionType: 'text', createdAt: Date.now() };
  const existingSurveys = pendingSurveys.get(jid) || [];
  const updatedSurveys   = [...existingSurveys, textSurveyData];
  pendingSurveys.set(jid, updatedSurveys);
  saveSurveyToDisk(jid, updatedSurveys);
  await db.query(
    `INSERT INTO wgpr_whatsapp_surveys
       (phone, claim_id, solution_id, measurer_id, survey_message_id, solution_type, status)
     VALUES (?, ?, ?, ?, ?, 'text', 'pending')
     ON DUPLICATE KEY UPDATE survey_message_id = VALUES(survey_message_id), solution_type = 'text', status = 'pending'`,
    [jid, claimId, solutionId, measurerId, surveyMessageId]
  );

  return surveyMessageId;
}

// Appelle Spring Boot pour enregistrer la mesure avec un commentaire audio
async function callSpringBootMeasureAudio(claimId, solutionId, measurerId, satisfactionStatus, audioBase64, mimeType) {
  const axios  = require('axios');
  const url    = (process.env.SPRING_BOOT_URL || 'http://localhost:8080') + '/api/whatgpr/internal/measure-with-audio';
  try {
    await axios.post(url,
      { claimId, solutionId, measurerId, satisfactionStatus, audioBase64, mimeType },
      { headers: { 'X-WhatGPR-Secret': INTERNAL_SECRET, 'Content-Type': 'application/json' }, timeout: 15000 }
    );
    console.log(`[WA] Mesure audio enregistrée → claimId=${claimId} status=${satisfactionStatus}`);
  } catch (err) {
    console.error('[WA] Erreur appel Spring Boot measure-with-audio:', err.message);
    throw err;
  }
}

// Appelle Spring Boot pour enregistrer la mesure de satisfaction
async function callSpringBootMeasure(claimId, solutionId, measurerId, satisfactionStatus, commentaire) {
  const axios  = require('axios');
  const url    = (process.env.SPRING_BOOT_URL || 'http://localhost:8080') + '/api/whatgpr/internal/measure';
  try {
    await axios.post(url, { claimId, solutionId, measurerId, satisfactionStatus, commentaire },
      { headers: { 'X-WhatGPR-Secret': INTERNAL_SECRET, 'Content-Type': 'application/json' }, timeout: 10000 }
    );
    console.log(`[WA] Mesure satisfaction enregistrée → claimId=${claimId} status=${satisfactionStatus}`);
  } catch (err) {
    console.error('[WA] Erreur appel Spring Boot measure:', err.message);
  }
}

// ─── Fallback automatique — deux timers ──────────────────────────────────────
const STALE_HOURS = parseInt(process.env.SURVEY_STALE_HOURS || '24');

async function closeStaleSurveys() {
  try {
    const [rows] = await db.query(
      `SELECT id, phone, claim_id, solution_id, measurer_id, satisfaction
       FROM wgpr_whatsapp_surveys
       WHERE status = 'voted'
         AND voted_at IS NOT NULL
         AND voted_at < DATE_SUB(NOW(), INTERVAL ? HOUR)`,
      [STALE_HOURS]
    );

    if (!rows.length) return;
    console.log(`[WA] Fallback auto : ${rows.length} sondage(s) sans commentaire depuis ${STALE_HOURS}h → clôture`);

    for (const row of rows) {
      try {
        const commentaire = '[Aucun commentaire]';
        await callSpringBootMeasure(
          row.claim_id, row.solution_id, row.measurer_id,
          row.satisfaction, commentaire
        );
        await db.query(
          `UPDATE wgpr_whatsapp_surveys
           SET status = 'completed', commentaire = ?, comment_fallback = 1
           WHERE id = ?`,
          [commentaire, row.id]
        );
        // Ne retire que l'entrée de CE claim — une autre réclamation du même numéro peut
        // légitimement être encore en attente de commentaire.
        const remainingComments = (pendingComments.get(row.phone) || []).filter(c => c.claimId !== row.claim_id);
        if (remainingComments.length) {
          pendingComments.set(row.phone, remainingComments);
          saveCommentWaitToDisk(row.phone, remainingComments);
        } else {
          pendingComments.delete(row.phone);
          deleteCommentWaitFromDisk(row.phone);
        }
        if (waStatus === 'connected') {
          try {
            await simulateTyping(row.phone, 'composing');
            await sock.sendMessage(row.phone, { text: '✅ Nous avons bien pris note de votre retour. Merci de contribuer à l\'amélioration de nos services.' });
          } catch (_) {}
        }
        console.log(`[WA] claimId=${row.claim_id} clôturé avec "Aucun commentaire".`);
      } catch (err) {
        console.error(`[WA] Erreur clôture auto claimId=${row.claim_id}:`, err.message);
      }
    }
  } catch (err) {
    console.error('[WA] Erreur closeStaleSurveys:', err.message);
  }
}

// Envoie une note vocale de solution + question de satisfaction texte (avec audios 2 et 3 optionnels)
// surveyAudioBase64 (facultatif) : note vocale de l'agent présentant le sondage au client.
async function sendSolutionAudioWithSurvey(to, audioBase64, mimeType, audio2Base64, audio3Base64, audio4Base64, claimId, solutionId, measurerId, surveyAudioBase64) {
  const jid = normalizeJid(to);
  if (waStatus !== 'connected') throw new Error('WhatsApp non connecté');
  if (!audioBase64) throw new Error('Audio 1 (solution) manquant');

  try {
    await sendAudioBuffer(jid, audioBase64, mimeType);
  } catch (err) {
    throw new Error(`Échec envoi solution_vocale.ogg: ${err.message}`);
  }

  if (surveyAudioBase64) {
    await sendAudioBuffer(jid, surveyAudioBase64, mimeType);
  }

  await simulateTyping(jid, 'composing');
  const questionMsg = await sock.sendMessage(jid, { text: SURVEY_QUESTION_TEXT });
  const surveyMessageId = questionMsg.key.id;

  const audioSurveyData = {
    claimId, solutionId, measurerId, surveyMessageId, solutionType: 'audio',
    audio2: audio2Base64 || null,
    audio3: audio3Base64 || null,
    audio4: audio4Base64 || null,
    mimeType: mimeType || 'audio/ogg; codecs=opus',
    createdAt: Date.now(),
  };
  const existingAudioSurveys = pendingSurveys.get(jid) || [];
  const updatedAudioSurveys   = [...existingAudioSurveys, audioSurveyData];
  pendingSurveys.set(jid, updatedAudioSurveys);
  saveSurveyToDisk(jid, updatedAudioSurveys);
  await db.query(
    `INSERT INTO wgpr_whatsapp_surveys
       (phone, claim_id, solution_id, measurer_id, survey_message_id, solution_type, status)
     VALUES (?, ?, ?, ?, ?, 'audio', 'pending')
     ON DUPLICATE KEY UPDATE survey_message_id = VALUES(survey_message_id), solution_type = 'audio', status = 'pending'`,
    [jid, claimId, solutionId, measurerId, surveyMessageId]
  );

  return surveyMessageId;
}

// Envoie un accusé de réception WhatsApp après enregistrement d'une réclamation ou suggestion
async function sendAcknowledgment(phone, codeClient, clientName, type) {
  const digits = phone.replace(/\D/g, '').replace(/^00/, '');
  if (digits.length < 7) throw new Error('Numéro de téléphone invalide : ' + phone);
  const to = digits + '@s.whatsapp.net';

  const MESSAGES = {
    reclamation: `Cher(e) bénéficiaire, votre réclamation a bien été prise en compte. Notre équipe dédiée s'en occupe et vous contactera prochainement. Merci de contribuer à l'amélioration de nos services.`,
    suggestion:  `Cher(e) bénéficiaire, votre suggestion a bien été prise en compte. Notre équipe dédiée s'en occupe et vous contactera prochainement. Merci de contribuer à l'amélioration de nos services.`,
  };
  const text = MESSAGES[type] || MESSAGES.reclamation;

  if (waStatus !== 'connected') {
    enqueueSend({ type: 'message', to, message: text });
    throw new Error('WhatsApp non connecté — l\'accusé de réception sera renvoyé automatiquement à la reconnexion.');
  }

  try {
    await simulateTyping(to, 'composing');
    await sock.sendMessage(to, { text });
    console.log(`[WA] Accusé de réception (${type || 'reclamation'}) envoyé → ${to}`);
  } catch (err) {
    enqueueSend({ type: 'message', to, message: text });
    console.error(`[WA] Erreur send-acknowledgment → ${to} :`, err.message);
    throw new Error(`Échec accusé de réception : ${err.message}`);
  }
}

async function sendPoll(to, question, options) {
  const jid = normalizeJid(to);
  if (waStatus !== 'connected') throw new Error('WhatsApp non connecté');
  await simulateTyping(jid, 'composing');
  const msg = await sock.sendMessage(jid, { poll: { name: question, values: options, selectableCount: 1 } });
  return msg.key.id;
}

function init() {
  connectingStartAt = Date.now();
  startWatchdog();
  // Fallback auto : vérifier au démarrage (30s pour laisser WhatsApp se connecter)
  // puis toutes les heures
  setTimeout(closeStaleSurveys, 30_000);
  setInterval(closeStaleSurveys, 60 * 60 * 1000);
  // Filet de sécurité : retente les envois en file toutes les 2 min, au cas où
  // la reconnexion n'aurait pas suffi à tout vider
  setInterval(flushSendQueue, 2 * 60 * 1000);

  createClient().catch(err => {
    console.error('[WA] Erreur initialize:', err.message);
  });
}

// Log les rejections non gérées sans crasher le processus
process.on('unhandledRejection', (err) => {
  console.error('[WA] Avertissement rejection non gérée:', err?.message || err);
});

// Arrêt propre
async function gracefulShutdown(signal) {
  console.log(`[WA] Signal ${signal} reçu — arrêt propre...`);
  try { if (sock) sock.end(undefined); } catch (_) {}
  process.exit(0);
}
process.on('SIGTERM', () => gracefulShutdown('SIGTERM'));
process.on('SIGINT',  () => gracefulShutdown('SIGINT'));

module.exports = { init, getQrCode, getStatus, getConnectedNumber, disconnect, sendMessage, sendAudio, sendPoll, sendSolutionWithSurvey, sendSolutionAudioWithSurvey, sendAcknowledgment, forceRestart };
