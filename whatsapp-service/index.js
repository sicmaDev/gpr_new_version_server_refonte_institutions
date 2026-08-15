require('dotenv').config();
const express = require('express');
const path    = require('path');

const { initTables }             = require('./db');
const { addSseClient }           = require('./events');
const wa = require('./whatsapp');

const app  = express();
const PORT = parseInt(process.env.PORT || '3001');

app.use(express.json({ limit: '50mb' }));
app.use(express.urlencoded({ limit: '50mb', extended: true }));

// CORS permissif — inclut les headers nécessaires au streaming audio (Range requests)
app.use((req, res, next) => {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, PATCH, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Range, Authorization');
  res.setHeader('Access-Control-Expose-Headers', 'Content-Range, Accept-Ranges, Content-Length, Content-Type');
  if (req.method === 'OPTIONS') return res.sendStatus(204);
  next();
});

// ─── Page QR auto-rafraîchissante ─────────────────────────────────────────────
app.get('/qr', (req, res) => {
  res.setHeader('Content-Type', 'text/html; charset=utf-8');
  res.send(`<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
  <title>WhatGPR — Connexion WhatsApp</title>
  <style>
    body { font-family: sans-serif; display: flex; flex-direction: column;
           align-items: center; justify-content: center; min-height: 100vh;
           margin: 0; background: #ECE5DD; }
    .card { background: white; border-radius: 16px; padding: 40px;
            box-shadow: 0 4px 20px rgba(0,0,0,.15); text-align: center; }
    h2 { color: #128C7E; margin-bottom: 8px; }
    p  { color: #555; margin-bottom: 24px; }
    img { width: 280px; height: 280px; border: 3px solid #25D366;
          border-radius: 8px; display: block; }
    .status { margin-top: 20px; padding: 8px 20px; border-radius: 20px;
              font-weight: bold; font-size: 14px; }
    .qr_pending  { background: #FFF3CD; color: #856404; }
    .connected   { background: #D4EDDA; color: #155724; }
    .connecting  { background: #CCE5FF; color: #004085; }
    .disconnected{ background: #F8D7DA; color: #721C24; }
    .hint { font-size: 13px; color: #888; margin-top: 12px; }
  </style>
</head>
<body>
  <div class="card">
    <h2>WhatGPR — Connexion WhatsApp</h2>
    <p>Ouvrez WhatsApp → <b>⋮</b> → <b>Appareils connectés</b> → <b>Connecter un appareil</b></p>
    <img id="qr-img" src="" alt="Chargement du QR...">
    <div id="badge" class="status qr_pending">⏳ En attente du scan...</div>
    <div class="hint" id="hint">Le QR se rafraîchit automatiquement toutes les 20 secondes.</div>
  </div>
  <script>
    async function refresh() {
      // Statut
      const st = await fetch('/internal/status').then(r => r.json()).catch(() => ({status:'disconnected'}));
      const badge = document.getElementById('badge');
      badge.className = 'status ' + st.status;
      const labels = {
        connected: '✅ Connecté !',
        disconnected: '❌ Déconnecté',
        qr_pending: '⏳ En attente du scan...',
        connecting: '🔄 Connexion en cours...'
      };
      badge.textContent = labels[st.status] || st.status;

      if (st.status === 'connected') {
        document.getElementById('qr-img').style.display = 'none';
        document.getElementById('hint').textContent = 'WhatsApp est connecté. Vous pouvez fermer cette page.';
        return;
      }

      // QR
      const data = await fetch('/internal/qr').then(r => r.json()).catch(() => null);
      if (data && data.qr) {
        document.getElementById('qr-img').src = data.qr;
        document.getElementById('qr-img').style.display = 'block';
      }
    }

    refresh();
    setInterval(refresh, 5000); // rafraîchit toutes les 5s pour rester synchro avec le QR
  </script>
</body>
</html>`);
});

// Servir les fichiers médias uploadés
app.use('/internal/uploads', express.static(
  path.join(__dirname, process.env.UPLOADS_DIR || 'uploads')
));

// ─── QR Code ──────────────────────────────────────────────────────────────────
app.get('/internal/qr', (req, res) => {
  const qr = wa.getQrCode();
  if (!qr) {
    return res.status(404).json({ error: 'QR non disponible', status: wa.getStatus() });
  }
  res.json({ qr });
});

// ─── Statut ───────────────────────────────────────────────────────────────────
app.get('/internal/status', (req, res) => {
  res.json({ status: wa.getStatus() });
});

// ─── Numéro connecté ──────────────────────────────────────────────────────────
app.get('/internal/connected-number', (req, res) => {
  res.json({ connectedNumber: wa.getConnectedNumber() });
});

// ─── Claims nécessitant commentaire pilote ────────────────────────────────────
// Retourne les sondages dès que le client a voté INSATISFAIT/PARTIEL sans commenter
app.get('/internal/claims-needing-comment', async (req, res) => {
  try {
    const { db } = require('./db');
    const [rows] = await db.query(
      `SELECT claim_id, satisfaction, pilot_deadline, comment_fallback, voted_at
       FROM wgpr_whatsapp_surveys
       WHERE status = 'voted'
         AND satisfaction IN ('UNSATISFIED', 'PARTIAL')
         AND comment_fallback = 0
       ORDER BY voted_at ASC`
    );
    res.json(rows);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// ─── Force restart (réveil de veille / connexion bloquée) ────────────────────
app.post('/internal/force-restart', async (req, res) => {
  console.log('[WA] Force restart demandé via API');
  try {
    await wa.forceRestart();
    res.json({ success: true });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// ─── Force restart + suppression session (erreur detached Frame / session corrompue) ─
app.post('/internal/force-restart-clean', async (req, res) => {
  console.log('[WA] Force restart CLEAN demandé via API (session effacée)');
  try {
    await wa.forceRestart(true);
    res.json({ success: true, message: 'Session supprimée. Scannez le QR sur /qr dans 30 secondes.' });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// ─── Déconnexion ──────────────────────────────────────────────────────────────
app.post('/internal/disconnect', async (req, res) => {
  try {
    await wa.disconnect();
    res.json({ success: true });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// ─── Envoyer un message texte ─────────────────────────────────────────────────
app.post('/internal/send', async (req, res) => {
  const { to, message } = req.body || {};
  if (!to || !message) {
    return res.status(400).json({ error: 'Champs "to" et "message" requis' });
  }
  console.log(`[WA] Envoi message → ${to} : "${message.substring(0, 60)}..."`);
  try {
    await wa.sendMessage(to, message);
    console.log(`[WA] Message envoyé avec succès → ${to}`);
    res.json({ success: true });
  } catch (err) {
    console.error(`[WA] Erreur envoi → ${to} :`, err.message);
    res.status(500).json({ error: err.message });
  }
});

// ─── Envoyer une note vocale simple (pas de sondage) ──────────────────────────
app.post('/internal/send-audio', async (req, res) => {
  const { to, audioBase64, mimeType } = req.body || {};
  if (!to || !audioBase64) {
    return res.status(400).json({ error: 'Champs "to" et "audioBase64" requis' });
  }
  console.log(`[WA] Envoi audio → ${to}`);
  try {
    await wa.sendAudio(to, audioBase64, mimeType);
    res.json({ success: true });
  } catch (err) {
    console.error(`[WA] Erreur envoi audio → ${to} :`, err.message);
    res.status(500).json({ error: err.message });
  }
});

// ─── Envoyer solution + poll de satisfaction (mesure par client WhatsApp) ─────
app.post('/internal/send-with-survey', async (req, res) => {
  const { to, message, claimId, solutionId, measurerId, surveyAudioBase64, surveyAudioMimeType } = req.body || {};
  if (!to || !message || !claimId || !solutionId || !measurerId) {
    return res.status(400).json({ error: 'Champs "to", "message", "claimId", "solutionId", "measurerId" requis' });
  }
  console.log(`[WA] Envoi solution+survey → ${to} (claimId=${claimId}, audio sondage=${!!surveyAudioBase64})`);
  try {
    const surveyMessageId = await wa.sendSolutionWithSurvey(
      to, message, claimId, solutionId, measurerId, surveyAudioBase64 || null, surveyAudioMimeType || null
    );
    res.json({ success: true, surveyMessageId });
  } catch (err) {
    console.error(`[WA] Erreur send-with-survey → ${to} :`, err.message);
    res.status(500).json({ error: err.message });
  }
});

// ─── Envoyer note vocale de solution + poll ───────────────────────────────────
app.post('/internal/send-with-survey-audio', async (req, res) => {
  const { to, audioBase64, mimeType, audioBase642, audioBase643, audioBase644, claimId, solutionId, measurerId, surveyAudioBase64 } = req.body || {};
  if (!to || !audioBase64 || !claimId || !solutionId || !measurerId)
    return res.status(400).json({ error: 'Champs "to", "audioBase64", "claimId", "solutionId", "measurerId" requis' });
  console.log(`[WA] Envoi note vocale+survey → ${to} (claimId=${claimId}, audio2=${!!audioBase642}, audio3=${!!audioBase643}, audio4=${!!audioBase644}, audio sondage=${!!surveyAudioBase64})`);
  try {
    const surveyMessageId = await wa.sendSolutionAudioWithSurvey(
      to, audioBase64, mimeType, audioBase642 || null, audioBase643 || null, audioBase644 || null, claimId, solutionId, measurerId, surveyAudioBase64 || null
    );
    res.json({ success: true, surveyMessageId });
  } catch (err) {
    console.error(`[WA] Erreur send-with-survey-audio → ${to} :`, err.message);
    res.status(500).json({ error: err.message });
  }
});

// ─── Accusé de réception au client après enregistrement d'une réclamation ────
app.post('/internal/send-acknowledgment', async (req, res) => {
  const { phone, codeClient, clientName, type } = req.body || {};
  if (!phone) return res.status(400).json({ error: 'Champ "phone" requis' });
  try {
    await wa.sendAcknowledgment(phone, codeClient, clientName, type || 'reclamation');
    res.json({ success: true });
  } catch (err) {
    console.error(`[WA] Erreur send-acknowledgment → ${phone} :`, err.message);
    res.status(500).json({ error: err.message });
  }
});

// ─── Envoyer un poll ──────────────────────────────────────────────────────────
app.post('/internal/sendPoll', async (req, res) => {
  const { to, question, options } = req.body || {};
  if (!to || !question || !Array.isArray(options) || options.length < 2) {
    return res.status(400).json({ error: 'Champs "to", "question" et "options" (≥2) requis' });
  }
  try {
    const messageId = await wa.sendPoll(to, question, options);
    res.json({ success: true, messageId });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// ─── SSE — stream d'événements ────────────────────────────────────────────────
app.get('/internal/events', (req, res) => {
  res.setHeader('Content-Type',  'text/event-stream');
  res.setHeader('Cache-Control', 'no-cache');
  res.setHeader('Connection',    'keep-alive');
  res.flushHeaders();

  // Ping de maintien de connexion toutes les 25 s
  const ping = setInterval(() => {
    try { res.write(': ping\n\n'); } catch (_) { clearInterval(ping); }
  }, 25_000);

  res.on('close', () => clearInterval(ping));
  addSseClient(res);
});

// ─── Démarrage ────────────────────────────────────────────────────────────────
(async () => {
  try {
    await initTables();
    wa.init();
    app.listen(PORT, () => {
      console.log(`[WhatGPR] Microservice démarré sur le port ${PORT}`);
    });
  } catch (err) {
    console.error('[WhatGPR] Erreur de démarrage:', err);
    process.exit(1);
  }
})();