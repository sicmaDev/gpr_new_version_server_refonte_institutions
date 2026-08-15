-- ============================================================
-- WhatGPR — Schéma MySQL (ARCHIVE — ne plus modifier)
--
-- Ce fichier n'est plus la source de vérité. Le schéma versionné des
-- tables wgpr_messages / wgpr_complaints / wgpr_complaint_messages /
-- wgpr_complaint_replies / wgpr_satisfaction_surveys vit maintenant dans
-- db/migration/whatgpr/V1__whatgpr_baseline.sql (Flyway). Pour toute
-- évolution de ces tables, ajouter une nouvelle migration Vn__*.sql,
-- pas modifier ce fichier.
--
-- wgpr_whatsapp_surveys reste géré uniquement par whatsapp-service/db.js
-- (pas d'entité JPA, hors périmètre Flyway).
-- ============================================================

CREATE TABLE IF NOT EXISTS wgpr_messages (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  wa_message_id VARCHAR(255) UNIQUE NOT NULL,
  from_number   VARCHAR(255) NOT NULL,
  from_name     VARCHAR(255),
  body          TEXT,
  media_type    VARCHAR(50),
  media_path    VARCHAR(500),
  timestamp     BIGINT NOT NULL,
  is_read       TINYINT(1) DEFAULT 0,
  status        ENUM('new','archived','converted') DEFAULT 'new',
  created_at    DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_from_number (from_number),
  INDEX idx_status      (status),
  INDEX idx_timestamp   (timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS wgpr_complaints (
  id                BIGINT AUTO_INCREMENT PRIMARY KEY,
  complaint_number  VARCHAR(50) UNIQUE NOT NULL,
  title             VARCHAR(500) NOT NULL,
  description       TEXT,
  from_number       VARCHAR(255) NOT NULL,
  attachments       JSON,
  status            ENUM('open','in_progress','closed') DEFAULT 'open',
  survey_sent       TINYINT(1) DEFAULT 0,
  survey_message_id VARCHAR(255),
  survey_reply_sent TINYINT(1) DEFAULT 0,
  created_at        DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_from_number (from_number),
  INDEX idx_status      (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS wgpr_complaint_messages (
  complaint_id BIGINT NOT NULL,
  message_id   BIGINT NOT NULL,
  PRIMARY KEY (complaint_id, message_id),
  FOREIGN KEY (complaint_id) REFERENCES wgpr_complaints(id) ON DELETE CASCADE,
  FOREIGN KEY (message_id)   REFERENCES wgpr_messages(id)   ON DELETE CASCADE
) ENGINE=InnoDB;

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