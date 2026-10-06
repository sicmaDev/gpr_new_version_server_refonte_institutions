# Chiffrement et sécurité des données — backend GPR

Ce document décrit ce qui est protégé dans le backend GPR, comment gérer les clés, et dans quel ordre
mettre une institution en production. **À lire en entier avant toute mise en production.**

> ⚠️ **Clé perdue = données perdues.** Les données chiffrées ne peuvent être relues qu'avec la clé
> `GPR_ENCRYPTION_KEY` qui a servi à les chiffrer. Il n'existe aucun moyen de les récupérer sans elle.
> Voir [Gestion des clés](#4-gestion-des-clés-obligatoire).

---

## 1. Ce qui est protégé

### 1.1 Colonnes chiffrées en base (AES-256-GCM)

Valeur stockée : `ENC:v1:` + Base64(IV 12 octets + texte chiffré + tag 128 bits).
Une valeur sans ce préfixe (ancienne donnée en clair) est relue telle quelle.

| Table | Colonnes |
|---|---|
| `gps_claim` | `client_first_and_last_name`, `address`, `tel`, `email`, `content`, `transmission_comment`, `draft_solution`, `draft_commentaire`, `delete_reason` |
| `gps_suggestion` | `client_first_and_last_name`, `address`, `tel`, `email`, `content`, `commentaire`, `delete_reason` |
| `gps_extra_content` | `contenu` |
| `gps_solution` | `content`, `commentaire`, `motif_desaprobation` |
| `gps_satisfaction_measure` | `commentaire` |
| `gps_message` | `content` |
| `gps_vote` | `contenu`, `commentaire` |
| `gps_historique_affectations` | `content_mail` |
| `gps_historique_transmissions` | `commentaire` |
| `gps_setting` | `value` (contient les mots de passe SMTP et SMS) |

Les VARCHAR chiffrés passent à `VARCHAR(1024)`, les TEXT à `MEDIUMTEXT`.

**Recherche par téléphone** : `gps_claim.tel_hash` (VARCHAR(64), index `idx_claim_tel_hash`) contient une
empreinte HMAC-SHA256 du téléphone (espaces ignorés), calculée avec `GPR_HMAC_KEY`. Elle sert à l'alerte de
doublon à l'enregistrement d'une réclamation et au regroupement des messages du bot. Un téléphone vide
n'a pas d'empreinte.

**Non chiffré (volontairement)** : codes (`code`, `code_client`), statuts, dates, clés étrangères, tables de
configuration et de référence (`gps_service_point`, `gps_objet`, `gps_product`…), colonnes `ai_*`,
module WhatsApp (`wgpr_*`, `gps_inbox*`), `gps_user.password` et `gps_keys.secret` (déjà hachés en BCrypt).

### 1.2 Fichiers chiffrés sur disque (AES-256-GCM, même clé)

| Dossier (production) | Contenu |
|---|---|
| `/app/gpr/preuve/` | Pièces jointes (réclamations, dénonciations, suggestions, compléments, WhatsApp) |
| `/app/gpr/claim_audio/` | Audios (dont commentaires vocaux WhatsApp) |

Format : `GPRENC1` (7 octets) + IV + contenu chiffré + tag. Un fichier sans cet en-tête (ancien fichier)
est renvoyé tel quel. Les fichiers sont déchiffrés à la volée par `/api/v1/media/download/**` et
`/api/v1/claimaudio/download/**` : **ils ne s'ouvrent plus directement depuis le disque.**

Non concernés : `/app/gpr/documents/` (manuels d'aide), fichiers du microservice WhatsApp (Node).

### 1.3 Autres protections

- **API fermée** : toutes les routes exigent une connexion, sauf : connexion, mot de passe oublié,
  licence, vérification de session, inscription publique, websocket (`/ws/**`), routes bot/site web
  (`/api/v1/apikey/**`, `/api/v1/webhook/**`, `/api/v1/bot/claim/**`), routes internes WhatGPR
  (protégées par `X-WhatGPR-Secret`) et médias WhatsApp.
- **`@RolesAllowed` actif** (configuration réservée à `H12`, sauf envoi SMS/e-mail au client et journaux).
- **Clé des jetons de connexion** (JWT) hors du code : `GPR_JWT_SECRET`.
- **Mots de passe SMTP et SMS** jamais renvoyés au navigateur ni au site web (`pwd` et `valMdp` à `null`) ;
  un mot de passe vide à l'enregistrement conserve l'ancien.

---

## 2. Variables d'environnement

| Variable | Obligatoire | Rôle |
|---|---|---|
| `GPR_ENCRYPTION_KEY` | **Oui** | Clé AES-256 (Base64, 32 octets) : chiffre les colonnes et les fichiers |
| `GPR_HMAC_KEY` | **Oui** | Clé HMAC (Base64, 32 octets, **différente** de la précédente) : empreinte du téléphone |
| `GPR_JWT_SECRET` | **Oui** | Clé de signature des jetons de connexion (Base64, ≥ 32 octets) |
| `WHATGPR_INTERNAL_SECRET` | **Oui** | Secret partagé avec le microservice WhatsApp (inchangé) |
| `GPR_ENCRYPT_EXISTING` | Non (`false`) | `true` une seule fois : chiffre les données déjà en base |
| `GPR_ENCRYPT_FILES` | Non (`non`) | `copie` puis `remplacer` une seule fois : chiffre les fichiers existants |
| `SUPERSET_ADMIN_PASSWORD` | Non | Superset n'est plus utilisé (remplacé à terme par Power BI) |

**L'application refuse de démarrer** si `GPR_ENCRYPTION_KEY`, `GPR_HMAC_KEY` ou `GPR_JWT_SECRET` est
absente, invalide ou trop courte, ou si les deux premières sont identiques. Le message indique la
variable en cause (jamais sa valeur).

Aucune clé n'est écrite dans le code ni dans Git. Les tests utilisent des clés jetables
(`src/test/resources/config/application.properties`).

---

## 3. Générer les clés

Une série de clés **par institution et par environnement** (test, production). Ne jamais réutiliser
les clés d'un poste de développement en production.

Linux / macOS / Git Bash :

```bash
openssl rand -base64 32   # GPR_ENCRYPTION_KEY
openssl rand -base64 32   # GPR_HMAC_KEY  (relancer : elle doit être différente)
openssl rand -base64 64   # GPR_JWT_SECRET
```

Windows PowerShell (si `openssl` n'est pas installé) :

```powershell
function NouvelleCle($n) { $b = New-Object byte[] $n; [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); [Convert]::ToBase64String($b) }
NouvelleCle 32   # GPR_ENCRYPTION_KEY
NouvelleCle 32   # GPR_HMAC_KEY
NouvelleCle 64   # GPR_JWT_SECRET
```

Ne jamais coller une clé dans un e-mail, un chat, un ticket ou un fichier du dépôt.

---

## 4. Gestion des clés (obligatoire)

**Règle d'or : une clé de production n'existe jamais en un seul exemplaire.** Aucune institution ne
passe en production avant que ses clés soient rangées comme suit :

1. **Gestionnaire de mots de passe d'équipe** (ex. Bitwarden, KeePass) : un dossier par institution,
   contenant les 3 clés et la date de création.
2. **Copie hors ligne** : clé USB chiffrée ou papier, dans un coffre-fort.
3. **Deux personnes de confiance** ont accès (ex. responsable technique + direction).

**Test annuel** : restaurer une sauvegarde de la base sur un poste isolé, démarrer avec les clés
issues de la copie de secours, et vérifier qu'une réclamation et une pièce jointe s'affichent.
Noter la date du test.

### 4.1 Sauvegardes : les clés ne vont jamais avec les données

Trois choses se sauvegardent, **séparément** :

| Quoi | Fréquence | Où |
|---|---|---|
| Base de données (dump complet) | chaque jour | système de sauvegarde habituel, **hors du serveur** |
| Fichiers (`/app/gpr/preuve`, `/app/gpr/claim_audio`) | chaque jour | idem, **hors du serveur** |
| **Clés** (3 copies) | **une seule fois**, puis à chaque changement ou perte d'une copie | rangement à part (§4, points 1 à 3) |

Règles :

- **Ne jamais mettre les clés dans la sauvegarde de la base ni des fichiers.** Sinon, le vol d'une
  sauvegarde donne accès à toutes les données et le chiffrement ne sert plus à rien. Les personnes qui
  gèrent les sauvegardes de la base ne doivent pas avoir accès aux clés.
- **Les clés ne changent jamais** : inutile de les sauvegarder à chaque sauvegarde de la base.
- La base et les fichiers sont déjà chiffrés : une sauvegarde volée est illisible sans les clés.
  Mais **une sauvegarde de la base sans la sauvegarde des fichiers (ou l'inverse) est incomplète**.

**Forme de la copie des clés.** La copie peut être un simple fichier texte lisible (3 lignes :
`GPR_ENCRYPTION_KEY=...`, `GPR_HMAC_KEY=...`, `GPR_JWT_SECRET=...`, avec la date et le nom de
l'institution). Un fichier lisible est pratique : on s'en sert immédiatement en cas de sinistre.
Ce qui compte n'est pas que le fichier soit codé, c'est **où il est rangé** : uniquement dans les
rangements du §4 (clé USB chiffrée dans un coffre, gestionnaire de mots de passe), jamais dans un
dossier partagé, un e-mail, une messagerie, Git, ni le même disque que les sauvegardes de la base.
Si ce fichier doit être posé sur un support ordinaire, le protéger par un mot de passe long
(archive chiffrée) et ranger ce mot de passe séparément.

**Perte de toutes les copies** : aucune récupération possible. Générer de nouvelles clés ne permet pas
de relire les anciennes données chiffrées (erreur « Impossible de déchiffrer la donnée »). Seules les
données non chiffrées (utilisateurs, codes, statuts, dates, tables de configuration) restent utilisables.

**Serveur détruit** : nouveau serveur (Java, MariaDB, code depuis Git), restauration de la base, des
fichiers, puis fourniture des clés au démarrage. Le code doit donc aussi être sur un dépôt hébergé
ailleurs que sur un poste de travail.

**Changer `GPR_ENCRYPTION_KEY` ou `GPR_HMAC_KEY`** n'est pas prévu : les données existantes
deviendraient illisibles. Une rotation demande une migration dédiée (déchiffrer avec l'ancienne,
rechiffrer avec la nouvelle) à développer le moment venu.

**Changer `GPR_JWT_SECRET`** est possible à tout moment : tous les utilisateurs sont déconnectés
une fois et doivent se reconnecter.

**Évolution possible** : ajouter une « clé de secours » hors serveur capable de tout déchiffrer en cas
de perte de la clé principale (non implémenté).

---

## 5. Mise en production (ordre à respecter)

> Le chiffrement des colonnes (étape 3), la migration (étape 4) et l'agrandissement des colonnes
> forment un tout : **ne pas déployer une version intermédiaire.**

1. **Prévenir les utilisateurs** : ils seront déconnectés une fois (nouvelle clé JWT).
2. **Sauvegarder** la base de production **et** les dossiers `/app/gpr/preuve` et `/app/gpr/claim_audio`.
   Vérifier que les sauvegardes se restaurent.
3. **Générer les 3 clés** (section 3) et **les ranger** (section 4) *avant* d'aller plus loin.
4. **Déclarer les variables** dans l'environnement du service :
   - Docker : `docker run -e GPR_ENCRYPTION_KEY=... -e GPR_HMAC_KEY=... -e GPR_JWT_SECRET=...`
     (ou un fichier `--env-file` non versionné, lisible uniquement par root) ;
   - systemd : lignes `Environment=` dans un fichier de surcharge (`systemctl edit <service>`),
     puis `systemctl daemon-reload`.
5. **Premier démarrage** avec en plus `GPR_ENCRYPT_EXISTING=true` et `GPR_ENCRYPT_FILES=copie`.
   Dans les journaux, vérifier :
   - `Chiffrement gps_claim : N ligne(s) chiffrée(s)` (une ligne par table) et
     `Empreinte téléphone gps_claim : N ligne(s) remplie(s)` ;
   - `Fichiers /app/gpr/preuve (copie) : N chiffré(s), 0 déjà chiffré(s), 0 erreur(s)` (et `claim_audio`).
6. **Contrôler** : aucune valeur en clair restante (ex. `SELECT COUNT(*) FROM gps_claim WHERE content NOT LIKE 'ENC:v1:%'`
   doit valoir 0), une ancienne réclamation s'affiche, l'alerte de doublon par téléphone fonctionne,
   les copies dans `/app/gpr/preuve_chiffre` et `/app/gpr/claim_audio_chiffre` sont complètes.
7. **Redémarrer** avec `GPR_ENCRYPT_FILES=remplacer` (et `GPR_ENCRYPT_EXISTING` retiré) : les fichiers
   d'origine sont chiffrés sur place. Vérifier `0 erreur(s)`, puis télécharger une pièce jointe et
   écouter un audio depuis l'application.
8. **Retirer** `GPR_ENCRYPT_EXISTING` et `GPR_ENCRYPT_FILES` (ou `false` / `non`), redémarrer.
9. **Supprimer** les dossiers `*_chiffre` une fois la validation terminée.
10. **Tester le frontend** avec chaque profil (agent, pilote, DE, administrateur), dont l'envoi
    d'e-mail et de SMS au client et le formulaire du site web.

Relancer les migrations ne fait rien sur des données déjà chiffrées (idempotent).

**Retour arrière** : redéployer la version précédente **et** restaurer la base et les dossiers
sauvegardés à l'étape 2 (la version précédente ne sait pas lire les données chiffrées).

---

## 6. Impacts sur les autres outils

- **Superset / Power BI / scripts SQL** qui lisent la base directement : les **comptages** et
  regroupements (par agence, statut, objet, canal, date, genre) fonctionnent toujours ; les colonnes
  chiffrées (noms, téléphones, textes) apparaissent sous la forme `ENC:v1:...`. Pour afficher ces
  informations dans Power BI, passer par une route de l'application (protégée), jamais par la base.
- **Ancien serveur GPR** (`gpr_server_sicma_new_version`) : il ne peut pas lire une base chiffrée. Il
  ne doit pas pointer vers la base de la refonte.
- **Site web de l'institution** (bot) : passe par l'API, aucun impact sur le chiffrement. Il ne reçoit
  plus les mots de passe SMTP/SMS dans `/api/v1/apikey/setting`.
- **Frontend — à adapter** (étape 7) : les pages `pages/Configurations/Email.js` (ligne ~170) et
  `pages/Configurations/Sms.js` (ligne ~188) exigent un mot de passe non vide. Le champ arrive
  désormais vide : il faut le rendre facultatif quand le paramètre existe déjà et afficher
  « laisser vide pour conserver le mot de passe actuel ». Tant que ce n'est pas fait, l'administrateur
  doit ressaisir le mot de passe pour enregistrer une modification.
- **Export de configuration** (`/api/v1/config/setting/export`) : n'inclut plus les mots de passe
  SMTP/SMS ; ils sont à ressaisir après un import.

---

## 7. Poste de développement

Les clés de développement sont dans les variables d'environnement Windows de l'utilisateur
(*Modifier les variables d'environnement pour votre compte*). VS Code doit être redémarré pour les voir.
Démarrage depuis PowerShell :

```powershell
cd C:\xampp\htdocs\GPR\REFONTE_GPR\gpr_new_version_server_refonte_institutions
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
foreach ($n in 'GPR_ENCRYPTION_KEY','GPR_HMAC_KEY','GPR_JWT_SECRET','WHATGPR_INTERNAL_SECRET') {
  Set-Item "env:$n" ([Environment]::GetEnvironmentVariable($n,'User')) }
.\mvnw.cmd -o spring-boot:run
```

Tests : `./mvnw test`. `EncryptionMigrationTest` utilise une copie locale de la base nommée
`gpr_sicma_copie` (restaurée depuis une sauvegarde) et recrée `gpr_sicma_copie_travail` à chaque
lancement ; il est ignoré si `gpr_sicma_copie` n'existe pas.

Les tests utilisent des **clés jetables**, différentes des clés de développement : ils ne peuvent pas
relire les données déjà chiffrées de la base locale. Un test doit donc créer ses propres données (dans
une transaction annulée) au lieu de lire celles qui existent.

---

## 8. Ce qui reste à faire

| Sujet | Détail |
|---|---|
| Module WhatsApp | `wgpr_*`, `gps_inbox*` et fichiers du microservice Node non chiffrés (écrits aussi par Node) |
| Données du personnel | Noms, e-mails, téléphones des agents (`gps_user`, `gps_historique_affectations`, `gps_historique_transmissions`) non chiffrés |
| Session WhatsApp dans Git | `whatsapp-service/.wwebjs_auth/` est versionné : le retirer de Git, l'ignorer, reconnecter WhatsApp ; `node_modules` est aussi versionné |
| Site web de l'institution | Clé API écrite en dur dans `PlaintController.php` (à déplacer dans `.env` et à changer) ; `withoutVerifying()` désactive la vérification TLS |
| Inscription publique | `/api/v1/config/user/publicRegister` reste ouverte et permet de choisir son poste et son rôle |
| `/api/v1/bot/claim/**` | Ouvert sans authentification ; le site web utilise `/api/v1/apikey/**` : à vérifier puis fermer |
| Code client | `REC-` + 4 caractères hexadécimaux (65 536 possibilités) : devinable ; `/api/v1/apikey/claim/{code}` renvoie la réclamation |
| Bot | `apiSecret` du paramètre bot renvoyé au navigateur et au site web ; `/api/v1/apikey/claim/{code}` renvoie les chemins disque des fichiers |
| Superset | Code à retirer ou remplacer lors du passage à Power BI ; changer le mot de passe admin Superset (présent dans l'historique Git) |
| Démarrage lent | `ddl-auto=update` réécrit ~300 colonnes à chaque démarrage : passer à Flyway pour tout le schéma |
| Rotation des clés | Non prévue (voir section 4) |
