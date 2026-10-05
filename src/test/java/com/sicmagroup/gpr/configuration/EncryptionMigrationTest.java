package com.sicmagroup.gpr.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import com.sicmagroup.gpr.utils.crypto.FieldEncryptor;

/**
 * Étape 4 : migration des données existantes, testée sur une COPIE locale de la base.
 *
 * Source (jamais modifiée) : la base "gpr_sicma_copie" (restaurée depuis une sauvegarde).
 * Le test recrée à chaque lancement une base de travail "gpr_sicma_copie_travail" avec les
 * tables concernées, et ne travaille que sur celle-ci.
 * Test ignoré si la base source n'existe pas (ex : autre poste, intégration continue).
 */
class EncryptionMigrationTest {

    private static final String SOURCE = System.getProperty("gpr.test.copie", "gpr_sicma_copie");
    private static final String TRAVAIL = SOURCE + "_travail";
    private static final String URL = "jdbc:mysql://localhost:3306/%s?useSSL=false&serverTimezone=UTC"
            + "&useUnicode=true&characterEncoding=UTF-8&allowPublicKeyRetrieval=true";
    private static final String USER = System.getProperty("gpr.test.db.user", "root");
    private static final String PASSWORD = System.getProperty("gpr.test.db.password", "");

    private JdbcTemplate db;
    private FieldEncryptor encryptor;

    private static DriverManagerDataSource dataSource(String database) {
        DriverManagerDataSource ds = new DriverManagerDataSource(String.format(URL, database), USER, PASSWORD);
        ds.setDriverClassName("com.mysql.cj.jdbc.Driver");
        return ds;
    }

    private static byte[] randomKey() {
        byte[] k = new byte[32];
        new SecureRandom().nextBytes(k);
        return k;
    }

    @BeforeEach
    void preparerCopieDeTravail() {
        JdbcTemplate admin;
        try {
            admin = new JdbcTemplate(dataSource("information_schema"));
            Integer n = admin.queryForObject("SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name = ?",
                    Integer.class, SOURCE);
            assumeTrue(n != null && n > 0, "Base " + SOURCE + " absente : test ignoré");
        } catch (org.springframework.jdbc.CannotGetJdbcConnectionException e) {
            assumeTrue(false, "MariaDB local indisponible : test ignoré");
            return;
        }
        admin.execute("DROP DATABASE IF EXISTS " + TRAVAIL);
        admin.execute("CREATE DATABASE " + TRAVAIL + " CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci");
        for (String table : EncryptionMigration.COLUMNS.keySet()) {
            Integer exists = admin.queryForObject("SELECT COUNT(*) FROM information_schema.tables "
                    + "WHERE table_schema = ? AND table_name = ?", Integer.class, SOURCE, table);
            if (exists != null && exists > 0) {
                admin.execute("CREATE TABLE " + TRAVAIL + "." + table + " LIKE " + SOURCE + "." + table);
                admin.execute("INSERT INTO " + TRAVAIL + "." + table + " SELECT * FROM " + SOURCE + "." + table);
            }
        }
        db = new JdbcTemplate(dataSource(TRAVAIL));
        encryptor = new FieldEncryptor(randomKey(), randomKey());
    }

    private EncryptionMigration migration(boolean migrateExisting) {
        return new EncryptionMigration(db.getDataSource(), encryptor, migrateExisting);
    }

    private List<String> presentColumns(String table) {
        return db.queryForList("SELECT column_name FROM information_schema.columns WHERE table_schema = DATABASE() "
                + "AND table_name = ?", String.class, table).stream()
                .filter(c -> EncryptionMigration.COLUMNS.getOrDefault(table, Map.of()).containsKey(c.toLowerCase()))
                .map(String::toLowerCase).toList();
    }

    /** table.colonne -> (id -> valeur brute en base) */
    private Map<String, Map<Long, String>> snapshot() {
        Map<String, Map<Long, String>> snap = new TreeMap<>();
        for (String table : EncryptionMigration.COLUMNS.keySet()) {
            for (String col : presentColumns(table)) {
                Map<Long, String> values = new TreeMap<>();
                db.query("SELECT id, " + col + " FROM " + table, rs -> {
                    values.put(rs.getLong(1), rs.getString(2));
                });
                snap.put(table + "." + col, values);
            }
        }
        return snap;
    }

    /** téléphone normalisé -> ids des réclamations */
    private Map<String, Set<Long>> reclamationsParTelephone() {
        Map<String, Set<Long>> map = new HashMap<>();
        db.query("SELECT id, tel FROM gps_claim WHERE tel IS NOT NULL AND tel <> ''", rs -> {
            String tel = encryptor.decrypt(rs.getString(2)).replaceAll("\\s+", "");
            if (!tel.isEmpty()) {
                map.computeIfAbsent(tel, t -> new TreeSet<>()).add(rs.getLong(1));
            }
        });
        return map;
    }

    @Test
    void sansMigrateExistingRienNEstChiffre() {
        Map<String, Map<Long, String>> avant = snapshot();
        migration(false).onStartup();
        assertThat(snapshot()).isEqualTo(avant);
        // Mais le schéma est prêt
        assertThat(db.queryForObject("SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() "
                + "AND table_name = 'gps_claim' AND index_name = 'idx_claim_tel_hash'", Integer.class)).isPositive();
    }

    @Test
    void migrationCompleteSurLaCopie() {
        Map<String, Map<Long, String>> original = snapshot();
        Map<String, Set<Long>> telAvant = reclamationsParTelephone();
        long valeursEnClair = original.values().stream().flatMap(m -> m.values().stream())
                .filter(v -> v != null && !FieldEncryptor.isEncrypted(v)).count();
        assertThat(valeursEnClair).as("la copie doit contenir des données en clair").isPositive();

        // 1re exécution
        EncryptionMigration m = migration(true);
        m.ensureSchema();
        Map<String, Integer> compte = m.migrateExisting();
        assertThat(compte.values().stream().mapToInt(Integer::intValue).sum()).isPositive();

        // Chaque valeur déchiffrée est identique à l'originale, et plus aucune valeur n'est en clair
        Map<String, Map<Long, String>> apres = snapshot();
        List<String> erreurs = new ArrayList<>();
        original.forEach((colonne, valeurs) -> valeurs.forEach((id, valeur) -> {
            String enBase = apres.get(colonne).get(id);
            if (valeur == null) {
                if (enBase != null) erreurs.add(colonne + " id=" + id + " : null devenu non null");
            } else if (!FieldEncryptor.isEncrypted(enBase)) {
                erreurs.add(colonne + " id=" + id + " : encore en clair");
            } else if (!valeur.equals(encryptor.decrypt(enBase))) {
                erreurs.add(colonne + " id=" + id + " : valeur différente après déchiffrement");
            }
        }));
        assertThat(erreurs).isEmpty();

        // La recherche par téléphone donne les mêmes résultats qu'avant
        Map<String, Set<Long>> telApres = new HashMap<>();
        for (String tel : telAvant.keySet()) {
            telApres.put(tel, new TreeSet<>(db.queryForList("SELECT id FROM gps_claim WHERE tel_hash = ?",
                    Long.class, encryptor.blindIndex(tel))));
        }
        assertThat(telApres).isEqualTo(telAvant);
        // Toute réclamation avec un vrai numéro (une fois déchiffré) a une empreinte.
        // Un téléphone vide ('') est chiffré comme le fait l'application, mais n'a pas d'empreinte.
        List<Long> sansEmpreinte = new ArrayList<>();
        db.query("SELECT id, tel FROM gps_claim WHERE tel IS NOT NULL AND tel_hash IS NULL", rs -> {
            if (!encryptor.decrypt(rs.getString(2)).isBlank()) {
                sansEmpreinte.add(rs.getLong(1));
            }
        });
        assertThat(sansEmpreinte).isEmpty();

        // Colonnes agrandies
        for (Map<String, Object> col : db.queryForList("SELECT table_name, column_name, data_type, character_maximum_length "
                + "FROM information_schema.columns WHERE table_schema = DATABASE() AND column_name IN "
                + "('client_first_and_last_name','address','tel','email') AND table_name IN ('gps_claim','gps_suggestion')")) {
            assertThat(((Number) col.get("character_maximum_length")).longValue())
                    .as(col.get("table_name") + "." + col.get("column_name")).isGreaterThanOrEqualTo(1024);
        }

        // 2e exécution : ne change rien
        Map<String, Integer> compte2 = m.migrateExisting();
        m.ensureSchema();
        assertThat(compte2.values()).allMatch(n -> n == 0);
        assertThat(snapshot()).isEqualTo(apres);
    }
}
