-- ============================================================
-- WhatGPR — Baseline Flyway (module WhatsApp uniquement)
--
-- Reflète l'état actuel des tables telles que créées jusqu'ici par
-- Hibernate (ddl-auto=update, entités Wgpr*). Sur une base existante,
-- Flyway ne rejoue pas ce script (spring.flyway.baseline-version=1) —
-- il sert de point de départ documenté pour les migrations futures
-- (V2, V3, ...). Sur une base neuve (nouvelle installation/institution),
-- il crée réellement les tables.
--
-- Hibernate continue de gérer ces mêmes tables via ddl-auto=update
-- (comportement inchangé pour le reste de l'application) : ce script
-- ne fait que documenter et versionner explicitement le schéma du
-- module WhatsApp, sans changer qui l'exécute au quotidien.
--
-- wgpr_whatsapp_surveys N'EST PAS ici : cette table reste sous la seule
-- responsabilité du microservice Node.js (whatsapp-service/db.js), qui
-- n'a pas d'entité JPA correspondante.
-- ============================================================

CREATE TABLE IF NOT EXISTS wgpr_messages (
  id               BIGINT AUTO_INCREMENT PRIMARY KEY,
  wa_message_id    VARCHAR(255) UNIQUE NOT NULL,
  from_number      VARCHAR(255) NOT NULL,
  from_name        VARCHAR(255),
  body             TEXT,
  media_type       VARCHAR(50),
  media_path       VARCHAR(500),
  timestamp        BIGINT NOT NULL,
  wa_type          VARCHAR(20) DEFAULT 'chat',
  is_read          TINYINT(1) DEFAULT 0,
  status           VARCHAR(20) DEFAULT 'new',
  connected_number VARCHAR(255) DEFAULT NULL,
  created_at       DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_wm_from_number (from_number),
  INDEX idx_wm_status      (status),
  INDEX idx_wm_timestamp   (timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS wgpr_complaints (
  id                BIGINT AUTO_INCREMENT PRIMARY KEY,
  complaint_number  VARCHAR(50) UNIQUE NOT NULL,
  title             VARCHAR(500) NOT NULL,
  description       TEXT,
  from_number       VARCHAR(255) NOT NULL,
  attachments       TEXT,
  status            VARCHAR(20) DEFAULT 'open',
  survey_sent       TINYINT(1) DEFAULT 0,
  survey_message_id VARCHAR(255),
  survey_reply_sent TINYINT(1) DEFAULT 0,
  created_at        DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_wc_from_number (from_number),
  INDEX idx_wc_status      (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS wgpr_complaint_messages (
  complaint_id BIGINT NOT NULL,
  message_id   BIGINT NOT NULL,
  PRIMARY KEY (complaint_id, message_id),
  FOREIGN KEY (complaint_id) REFERENCES wgpr_complaints(id) ON DELETE CASCADE,
  FOREIGN KEY (message_id)   REFERENCES wgpr_messages(id)   ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS wgpr_complaint_replies (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  complaint_id BIGINT NOT NULL,
  body         TEXT NOT NULL,
  sent_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
  wa_status    VARCHAR(50) DEFAULT 'sent',
  FOREIGN KEY (complaint_id) REFERENCES wgpr_complaints(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS wgpr_satisfaction_surveys (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  complaint_id BIGINT NOT NULL,
  score        TINYINT NOT NULL,
  label        VARCHAR(100),
  received_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (complaint_id) REFERENCES wgpr_complaints(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
