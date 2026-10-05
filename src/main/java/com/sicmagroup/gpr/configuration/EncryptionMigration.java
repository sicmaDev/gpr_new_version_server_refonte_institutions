package com.sicmagroup.gpr.configuration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.sicmagroup.gpr.utils.crypto.FieldEncryptor;

import jakarta.annotation.PostConstruct;

/**
 * Migration de la base existante vers les colonnes chiffrées (étape 4).
 *
 * TOUJOURS (idempotent) : agrandit les colonnes chiffrées si besoin, ajoute tel_hash et
 * l'index idx_claim_tel_hash s'ils manquent.
 *
 * SEULEMENT si gpr.crypto.migrate-existing=true (GPR_ENCRYPT_EXISTING) : chiffre en JDBC,
 * par lots, les valeurs encore en clair et remplit tel_hash. Une valeur déjà chiffrée
 * ("ENC:v1:") n'est jamais rechiffrée ; chaque valeur est vérifiée par déchiffrement avant
 * d'être écrite ; une colonne absente de la base est ignorée.
 */
@Component
@DependsOn({ "entityManagerFactory", "encryptionConfig" })
public class EncryptionMigration {

    private static final Logger log = LoggerFactory.getLogger(EncryptionMigration.class);

    static final int BATCH_SIZE = 200;
    static final int VARCHAR_LENGTH = 1024;
    static final String CLAIM_TABLE = "gps_claim";
    static final String TEL_HASH_INDEX = "idx_claim_tel_hash";

    /** Colonnes chiffrées : table -> (colonne -> true si texte long / false si VARCHAR). */
    static final Map<String, Map<String, Boolean>> COLUMNS = new LinkedHashMap<>();

    static {
        column("gps_claim", "client_first_and_last_name", false);
        column("gps_claim", "address", false);
        column("gps_claim", "tel", false);
        column("gps_claim", "email", false);
        column("gps_claim", "content", true);
        column("gps_claim", "transmission_comment", true);
        column("gps_claim", "draft_solution", true);
        column("gps_claim", "draft_commentaire", true);
        column("gps_claim", "delete_reason", true);
        column("gps_suggestion", "client_first_and_last_name", false);
        column("gps_suggestion", "address", false);
        column("gps_suggestion", "tel", false);
        column("gps_suggestion", "email", false);
        column("gps_suggestion", "content", true);
        column("gps_suggestion", "commentaire", true);
        column("gps_suggestion", "delete_reason", true);
        column("gps_extra_content", "contenu", true);
        column("gps_solution", "content", true);
        column("gps_solution", "commentaire", true);
        column("gps_solution", "motif_desaprobation", true);
        column("gps_satisfaction_measure", "commentaire", true);
        column("gps_message", "content", true);
        column("gps_vote", "contenu", true);
        column("gps_vote", "commentaire", true);
        column("gps_historique_affectations", "content_mail", true);
        column("gps_historique_transmissions", "commentaire", true);
        column("gps_setting", "value", true);
    }

    private static void column(String table, String column, boolean longText) {
        COLUMNS.computeIfAbsent(table, t -> new LinkedHashMap<>()).put(column, longText);
    }

    private final JdbcTemplate jdbc;
    private final FieldEncryptor encryptor;
    private final boolean migrateExisting;

    public EncryptionMigration(DataSource dataSource, FieldEncryptor encryptor,
            @Value("${gpr.crypto.migrate-existing:false}") boolean migrateExisting) {
        this.jdbc = new JdbcTemplate(dataSource);
        this.encryptor = encryptor;
        this.migrateExisting = migrateExisting;
    }

    @PostConstruct
    void onStartup() {
        ensureSchema();
        if (migrateExisting) {
            log.warn("GPR_ENCRYPT_EXISTING=true : chiffrement des données existantes en cours. "
                    + "Pensez à remettre la variable à false après ce démarrage.");
            migrateExisting();
        }
    }

    // ------------------------------------------------------------------ schéma

    /** Agrandit les colonnes, ajoute tel_hash et son index si besoin. Idempotent. */
    public void ensureSchema() {
        COLUMNS.forEach((table, cols) -> cols.forEach((column, longText) -> enlarge(table, column, longText)));

        if (tableExists(CLAIM_TABLE) && columnInfo(CLAIM_TABLE, "tel_hash") == null) {
            jdbc.execute("ALTER TABLE " + CLAIM_TABLE + " ADD COLUMN tel_hash VARCHAR(64) NULL");
            log.info("Colonne {}.tel_hash ajoutée", CLAIM_TABLE);
        }
        if (tableExists(CLAIM_TABLE) && !indexExists(CLAIM_TABLE, TEL_HASH_INDEX)) {
            jdbc.execute("CREATE INDEX " + TEL_HASH_INDEX + " ON " + CLAIM_TABLE + " (tel_hash)");
            log.info("Index {} créé", TEL_HASH_INDEX);
        }
    }

    private void enlarge(String table, String column, boolean longText) {
        Map<String, Object> info = columnInfo(table, column);
        if (info == null) {
            log.info("Colonne {}.{} absente de la base : ignorée", table, column);
            return;
        }
        String type = String.valueOf(info.get("DATA_TYPE")).toLowerCase();
        Number max = (Number) info.get("CHARACTER_MAXIMUM_LENGTH");
        String target;
        if (longText) {
            if (type.equals("mediumtext") || type.equals("longtext")) {
                return;
            }
            target = "MEDIUMTEXT";
        } else {
            if ((type.equals("varchar") && max != null && max.longValue() >= VARCHAR_LENGTH)
                    || type.endsWith("text")) {
                return;
            }
            target = "VARCHAR(" + VARCHAR_LENGTH + ")";
        }
        String charset = info.get("CHARACTER_SET_NAME") == null ? ""
                : " CHARACTER SET " + info.get("CHARACTER_SET_NAME") + " COLLATE " + info.get("COLLATION_NAME");
        jdbc.execute("ALTER TABLE " + table + " MODIFY COLUMN " + column + " " + target + charset + " NULL");
        log.info("Colonne {}.{} agrandie : {} -> {}", table, column, type, target);
    }

    private boolean tableExists(String table) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.tables "
                + "WHERE table_schema = DATABASE() AND table_name = ?", Integer.class, table);
        return n != null && n > 0;
    }

    private Map<String, Object> columnInfo(String table, String column) {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT DATA_TYPE, CHARACTER_MAXIMUM_LENGTH, "
                + "CHARACTER_SET_NAME, COLLATION_NAME FROM information_schema.columns "
                + "WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?", table, column);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private boolean indexExists(String table, String index) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.statistics "
                + "WHERE table_schema = DATABASE() AND table_name = ? AND index_name = ?", Integer.class, table, index);
        return n != null && n > 0;
    }

    // ------------------------------------------------------------- migration

    /** Chiffre les valeurs encore en clair et remplit tel_hash. Renvoie le nombre de lignes modifiées par table. */
    public Map<String, Integer> migrateExisting() {
        Map<String, Integer> result = new LinkedHashMap<>();
        COLUMNS.forEach((table, cols) -> {
            List<String> present = new ArrayList<>();
            for (String c : cols.keySet()) {
                if (columnInfo(table, c) != null) {
                    present.add(c);
                } else {
                    log.info("Colonne {}.{} absente de la base : ignorée", table, c);
                }
            }
            if (!present.isEmpty()) {
                int n = encryptTable(table, present);
                result.put(table, n);
                log.info("Chiffrement {} : {} ligne(s) chiffrée(s)", table, n);
            }
        });
        if (tableExists(CLAIM_TABLE) && columnInfo(CLAIM_TABLE, "tel_hash") != null) {
            int n = fillTelHash();
            result.put(CLAIM_TABLE + ".tel_hash", n);
            log.info("Empreinte téléphone {} : {} ligne(s) remplie(s)", CLAIM_TABLE, n);
        }
        return result;
    }

    private int encryptTable(String table, List<String> columns) {
        String plainCondition = String.join(" OR ", columns.stream()
                .map(c -> "(" + c + " IS NOT NULL AND " + c + " NOT LIKE 'ENC:v1:%')").toList());
        String select = "SELECT id, " + String.join(", ", columns) + " FROM " + table
                + " WHERE id > ? AND (" + plainCondition + ") ORDER BY id LIMIT " + BATCH_SIZE;
        Map<String, Integer> maxLengths = new LinkedHashMap<>();
        for (String c : columns) {
            Number max = (Number) columnInfo(table, c).get("CHARACTER_MAXIMUM_LENGTH");
            maxLengths.put(c, max == null ? Integer.MAX_VALUE : (int) Math.min(Integer.MAX_VALUE, max.longValue()));
        }

        int updated = 0;
        long lastId = 0;
        while (true) {
            List<Map<String, Object>> rows = jdbc.queryForList(select, lastId);
            if (rows.isEmpty()) {
                break;
            }
            for (Map<String, Object> row : rows) {
                long id = ((Number) row.get("id")).longValue();
                lastId = id;
                List<String> sets = new ArrayList<>();
                List<String> guards = new ArrayList<>();
                List<Object> values = new ArrayList<>();
                List<Object> guardValues = new ArrayList<>();
                for (String c : columns) {
                    Object raw = row.get(c);
                    if (raw == null) {
                        continue;
                    }
                    String plain = raw.toString();
                    if (FieldEncryptor.isEncrypted(plain)) {
                        continue;
                    }
                    String encrypted = encryptor.encrypt(plain);
                    if (!plain.equals(encryptor.decrypt(encrypted))) {
                        log.error("Vérification échouée pour {}.{} id={} : valeur non modifiée", table, c, id);
                        continue;
                    }
                    if (encrypted.length() > maxLengths.get(c)) {
                        log.error("Valeur chiffrée trop longue pour {}.{} id={} ({} > {}) : valeur non modifiée",
                                table, c, id, encrypted.length(), maxLengths.get(c));
                        continue;
                    }
                    sets.add(c + " = ?");
                    values.add(encrypted);
                    guards.add(c + " = ?");
                    guardValues.add(plain);
                }
                if (sets.isEmpty()) {
                    continue;
                }
                values.add(id);
                values.addAll(guardValues);
                // La garde "colonne = ancienne valeur" évite d'écraser une valeur modifiée entre-temps
                int n = jdbc.update("UPDATE " + table + " SET " + String.join(", ", sets)
                        + " WHERE id = ? AND " + String.join(" AND ", guards), values.toArray());
                updated += n;
            }
        }
        return updated;
    }

    private int fillTelHash() {
        String select = "SELECT id, tel FROM " + CLAIM_TABLE
                + " WHERE id > ? AND tel_hash IS NULL AND tel IS NOT NULL AND tel <> '' ORDER BY id LIMIT " + BATCH_SIZE;
        int updated = 0;
        long lastId = 0;
        while (true) {
            List<Map<String, Object>> rows = jdbc.queryForList(select, lastId);
            if (rows.isEmpty()) {
                break;
            }
            for (Map<String, Object> row : rows) {
                long id = ((Number) row.get("id")).longValue();
                lastId = id;
                String tel = row.get("tel").toString();
                String hash = encryptor.blindIndex(encryptor.decrypt(tel));
                if (hash == null) {
                    continue;
                }
                updated += jdbc.update("UPDATE " + CLAIM_TABLE + " SET tel_hash = ? WHERE id = ? AND tel_hash IS NULL",
                        hash, id);
            }
        }
        return updated;
    }
}
