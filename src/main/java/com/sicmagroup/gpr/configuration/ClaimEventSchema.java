package com.sicmagroup.gpr.configuration;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * La colonne « type d'événement » de l'historique est créée par Hibernate comme une liste fermée (ENUM) :
 * un nouveau type d'événement (ex. DELETED, RESTORED, WHATSAPP_SENT_CLIENT) serait alors refusé par une base
 * déjà existante. Au démarrage, on la remplace par un texte libre : les valeurs déjà enregistrées sont conservées.
 * Idempotent : sans effet si la colonne est déjà du texte.
 */
@Component
@DependsOn("entityManagerFactory")
public class ClaimEventSchema {

    private static final Logger log = LoggerFactory.getLogger(ClaimEventSchema.class);

    private final JdbcTemplate jdbc;

    public ClaimEventSchema(DataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);
    }

    @PostConstruct
    void ensureSchema() {
        try {
            String type = jdbc.query("SELECT DATA_TYPE FROM information_schema.columns WHERE table_schema = DATABASE() "
                    + "AND table_name = 'gps_claim_events' AND column_name = 'event_type'",
                    rs -> rs.next() ? rs.getString(1) : null);
            if ("enum".equalsIgnoreCase(type)) {
                jdbc.execute("ALTER TABLE gps_claim_events MODIFY COLUMN event_type VARCHAR(60) NULL");
                log.info("Colonne gps_claim_events.event_type convertie de ENUM en texte");
            }
        } catch (Exception e) {
            // base non MySQL/MariaDB ou table absente : rien à faire
            log.debug("Schéma de l'historique non modifié : {}", e.getMessage());
        }
    }
}
