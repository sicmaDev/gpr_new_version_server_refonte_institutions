package com.sicmagroup.gpr.configuration;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Colonnes IA déjà créées par une ancienne version de GPR avec un type trop court (texte limité à 255 caractères) :
 * Hibernate ajoute les colonnes manquantes mais ne modifie jamais une colonne existante. Au démarrage, on passe
 * ces colonnes en texte long ; les valeurs déjà enregistrées sont conservées. Idempotent.
 */
@Component
@DependsOn("entityManagerFactory")
public class IaSchema {

    private static final Logger log = LoggerFactory.getLogger(IaSchema.class);

    /** table, colonne : colonnes qui doivent pouvoir contenir un texte long. */
    private static final String[][] TEXTES_LONGS = {
            { "gps_suggestion", "ai_summary" },
            { "gps_suggestion", "ai_keywords" },
            { "gps_suggestion", "ai_raisonnement_classification" },
            { "gps_suggestion", "ai_raisonnement_urgence" },
            { "gps_claim", "ai_summary" },
            { "gps_claim", "ai_keywords" },
            { "gps_claim", "ai_raisonnement_classification" },
            { "gps_claim", "ai_raisonnement_urgence" },
    };

    private final JdbcTemplate jdbc;

    public IaSchema(DataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);
    }

    @PostConstruct
    void ensureSchema() {
        for (String[] c : TEXTES_LONGS) {
            try {
                String type = jdbc.query("SELECT DATA_TYPE FROM information_schema.columns WHERE table_schema = DATABASE() "
                        + "AND table_name = ? AND column_name = ?", rs -> rs.next() ? rs.getString(1) : null, c[0], c[1]);
                if ("varchar".equalsIgnoreCase(type) || "char".equalsIgnoreCase(type) || "tinytext".equalsIgnoreCase(type)) {
                    jdbc.execute("ALTER TABLE " + c[0] + " MODIFY COLUMN " + c[1] + " TEXT NULL");
                    log.info("Colonne {}.{} passée en texte long", c[0], c[1]);
                }
            } catch (Exception e) {
                log.debug("Colonne {}.{} non modifiée : {}", c[0], c[1], e.getMessage());
            }
        }
    }
}
