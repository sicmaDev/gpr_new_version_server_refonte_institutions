require('dotenv').config();
const mysql2 = require('mysql2');

const pool = mysql2.createPool({
  host:            process.env.DB_HOST     || 'localhost',
  port:            parseInt(process.env.DB_PORT || '3306'),
  user:            process.env.DB_USER     || 'root',
  password:        process.env.DB_PASSWORD || '',
  database:        process.env.DB_NAME     || 'gpr_sicma',
  waitForConnections: true,
  connectionLimit: 10,
  charset: 'utf8mb4',
});

const db = pool.promise();

// Tables possédées par Spring Boot / Hibernate (ddl-auto=update sur les entités
// WgprMessage, WgprComplaint, WgprComplaintMessage, WgprComplaintReply,
// WgprSatisfactionSurvey) : Node ne les crée/modifie plus pour éviter une double
// gestion de schéma (deux systèmes de migration indépendants sur les mêmes tables).
// Seule wgpr_whatsapp_surveys reste sous la responsabilité de Node (pas d'entité JPA).
async function initTables() {
  await db.query(`
    CREATE TABLE IF NOT EXISTS wgpr_whatsapp_surveys (
      id                BIGINT AUTO_INCREMENT PRIMARY KEY,
      phone             VARCHAR(50) NOT NULL,
      claim_id          BIGINT NOT NULL,
      solution_id       BIGINT NOT NULL,
      measurer_id       BIGINT NOT NULL,
      survey_message_id VARCHAR(255),
      status            ENUM('pending','voted','completed') DEFAULT 'pending',
      solution_type     ENUM('text','audio') DEFAULT 'text',
      satisfaction      VARCHAR(20),
      commentaire       TEXT,
      created_at        DATETIME DEFAULT CURRENT_TIMESTAMP,
      INDEX idx_wws_phone  (phone),
      INDEX idx_wws_status (status)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  `);

  // Migrations : ajout de colonnes si elles n'existent pas encore
  await db.query(`
    ALTER TABLE wgpr_whatsapp_surveys
    ADD COLUMN IF NOT EXISTS solution_type ENUM('text','audio') DEFAULT 'text'
  `).catch(() => {});

  // wgpr_messages.connected_number est géré par Hibernate (entité WgprMessage) ;
  // seul l'index reste à la charge de Node (non déclaré côté JPA).
  await db.query(`
    ALTER TABLE wgpr_messages
    ADD INDEX IF NOT EXISTS idx_wm_connected_number (connected_number)
  `).catch(() => {});

  await db.query(`
    ALTER TABLE wgpr_whatsapp_surveys
    ADD COLUMN IF NOT EXISTS voted_at DATETIME DEFAULT NULL
  `).catch(() => {});

  await db.query(`
    ALTER TABLE wgpr_whatsapp_surveys
    ADD COLUMN IF NOT EXISTS comment_fallback TINYINT(1) DEFAULT 0
  `).catch(() => {});

  await db.query(`
    ALTER TABLE wgpr_whatsapp_surveys
    ADD COLUMN IF NOT EXISTS pilot_deadline DATETIME DEFAULT NULL
  `).catch(() => {});

  // Sans cette contrainte, le ON DUPLICATE KEY UPDATE de l'INSERT (renvoi d'un sondage
  // pour la même réclamation) ne se déclenchait jamais : chaque renvoi créait une ligne
  // en double au lieu de mettre à jour la précédente.
  await db.query(`
    ALTER TABLE wgpr_whatsapp_surveys
    ADD UNIQUE INDEX IF NOT EXISTS uq_wws_phone_claim (phone, claim_id)
  `).catch(() => {});

  console.log('[DB] Tables wgpr_* vérifiées / créées.');
}

module.exports = { db, initTables };