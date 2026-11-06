package com.sicmagroup.gpr.configuration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.sicmagroup.gpr.utils.Constante;
import com.sicmagroup.gpr.utils.crypto.FieldEncryptor;

import jakarta.annotation.PostConstruct;

/**
 * Retrait du chiffrement : remet en clair les colonnes et les fichiers déjà chiffrés.
 *
 * S'exécute seulement si GPR_RETIRER_CHIFFREMENT=true, avec la clé qui a servi à chiffrer et après
 * sauvegarde de la base et des fichiers. Restent chiffrés : les réglages mail et SMS, et le mot de
 * passe temporaire des comptes. Relancer ne change rien sur des données déjà en clair ; une valeur
 * illisible est signalée et laissée telle quelle.
 */
@Component
@DependsOn({ "entityManagerFactory", "encryptionConfig" })
public class RetraitChiffrementMigration {

    private static final Logger log = LoggerFactory.getLogger(RetraitChiffrementMigration.class);

    private static final int BATCH_SIZE = 200;
    private static final byte[] FILE_MAGIC = "GPRENC1".getBytes(StandardCharsets.US_ASCII);

    private static final Map<String, List<String>> COLUMNS = new LinkedHashMap<>();

    static {
        COLUMNS.put("gps_claim", List.of("client_first_and_last_name", "address", "tel", "email", "crew", "content",
                "transmission_comment", "draft_solution", "draft_commentaire", "delete_reason"));
        COLUMNS.put("gps_suggestion", List.of("client_first_and_last_name", "address", "tel", "crew", "email", "content",
                "commentaire", "delete_reason"));
        COLUMNS.put("gps_extra_content", List.of("contenu"));
        COLUMNS.put("gps_solution", List.of("content", "commentaire", "motif_desaprobation"));
        COLUMNS.put("gps_satisfaction_measure", List.of("commentaire"));
        COLUMNS.put("gps_message", List.of("content"));
        COLUMNS.put("gps_vote", List.of("contenu", "commentaire"));
        COLUMNS.put("gps_historique_affectations", List.of("content_mail"));
        COLUMNS.put("gps_historique_transmissions", List.of("commentaire"));
        COLUMNS.put("gps_sms_log", List.of("numero_destinataire", "content"));
        COLUMNS.put("gps_claim_sla", List.of("breach_comment", "justified_by_id"));
        COLUMNS.put("gps_contact_attempt", List.of("comment"));
    }

    private final JdbcTemplate jdbc;
    private final FieldEncryptor encryptor;
    private final boolean actif;

    public RetraitChiffrementMigration(DataSource dataSource, FieldEncryptor encryptor,
            @Value("${gpr.crypto.retirer-chiffrement:false}") boolean actif) {
        this.jdbc = new JdbcTemplate(dataSource);
        this.encryptor = encryptor;
        this.actif = actif;
    }

    @PostConstruct
    void onStartup() {
        if (!actif) {
            return;
        }
        log.warn("GPR_RETIRER_CHIFFREMENT=true : remise en clair des données. Retirez la variable après ce démarrage.");
        COLUMNS.forEach(this::dechiffrerTable);
        dechiffrerAutresReglages();
        retirerEmpreinteTelephone();
        dechiffrerFichiers(Paths.get(Constante.DEVMODE ? Constante.TEST_PATH_PIECE_JOINTES : Constante.PROD_PATH_PIECE_JOINTES));
        dechiffrerFichiers(Paths.get(Constante.DEVMODE ? Constante.TEST_PATH_AUDIO : Constante.PROD_PATH_AUDIO));
    }

    private void dechiffrerTable(String table, List<String> colonnes) {
        for (String colonne : colonnes) {
            if (!colonneExiste(table, colonne)) {
                log.info("Colonne {}.{} absente : ignorée", table, colonne);
                continue;
            }
            int[] bilan = dechiffrerColonne(table, colonne, "");
            log.info("Retrait du chiffrement {}.{} : {} valeur(s) en clair, {} erreur(s)", table, colonne, bilan[0], bilan[1]);
        }
    }

    /** Les réglages restent en clair, sauf le mail et le SMS (voir Setting). */
    private void dechiffrerAutresReglages() {
        int[] bilan = dechiffrerColonne("gps_setting", "value", " AND libelle NOT IN ('" + Constante.MAIL_SLUG + "', '"
                + Constante.SMS_SLUG + "')");
        log.info("Retrait du chiffrement gps_setting.value : {} valeur(s) en clair, {} erreur(s)", bilan[0], bilan[1]);
    }

    private int[] dechiffrerColonne(String table, String colonne, String filtre) {
        String select = "SELECT id, " + colonne + " AS valeur FROM " + table + " WHERE id > ? AND " + colonne
                + " LIKE '" + FieldEncryptor.PREFIX + "%'" + filtre + " ORDER BY id LIMIT " + BATCH_SIZE;
        String update = "UPDATE " + table + " SET " + colonne + " = ? WHERE id = ? AND " + colonne + " = ?";
        int faits = 0;
        int erreurs = 0;
        long dernierId = 0;
        while (true) {
            List<Map<String, Object>> lignes = jdbc.queryForList(select, dernierId);
            if (lignes.isEmpty()) {
                return new int[] { faits, erreurs };
            }
            for (Map<String, Object> ligne : lignes) {
                long id = ((Number) ligne.get("id")).longValue();
                dernierId = id;
                String chiffre = ligne.get("valeur").toString();
                try {
                    faits += jdbc.update(update, encryptor.decrypt(chiffre), id, chiffre);
                } catch (IllegalStateException e) {
                    erreurs++;
                    log.error("Valeur illisible {}.{} id={} : laissée telle quelle", table, colonne, id);
                }
            }
        }
    }

    private void retirerEmpreinteTelephone() {
        if (!colonneExiste("gps_claim", "tel_hash")) {
            return;
        }
        Integer index = jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() "
                + "AND table_name = 'gps_claim' AND index_name = 'idx_claim_tel_hash'", Integer.class);
        if (index != null && index > 0) {
            jdbc.execute("DROP INDEX idx_claim_tel_hash ON gps_claim");
        }
        jdbc.execute("ALTER TABLE gps_claim DROP COLUMN tel_hash");
        log.info("Colonne gps_claim.tel_hash retirée");
    }

    private void dechiffrerFichiers(Path dossier) {
        if (!Files.isDirectory(dossier)) {
            log.info("Dossier {} absent : ignoré", dossier);
            return;
        }
        List<Path> fichiers = new ArrayList<>();
        try (Stream<Path> s = Files.list(dossier)) {
            s.filter(Files::isRegularFile).forEach(fichiers::add);
        } catch (IOException e) {
            log.error("Lecture du dossier {} impossible : {}", dossier, e.getMessage());
            return;
        }
        int faits = 0;
        int erreurs = 0;
        for (Path fichier : fichiers) {
            try {
                byte[] contenu = Files.readAllBytes(fichier);
                if (contenu.length >= FILE_MAGIC.length
                        && Arrays.equals(contenu, 0, FILE_MAGIC.length, FILE_MAGIC, 0, FILE_MAGIC.length)) {
                    ecrire(fichier, encryptor.decryptBytes(contenu, FILE_MAGIC.length));
                    faits++;
                }
            } catch (IOException | IllegalStateException e) {
                erreurs++;
                log.error("Fichier illisible {} : laissé tel quel", fichier.getFileName());
            }
        }
        log.info("Retrait du chiffrement {} : {} fichier(s) en clair, {} erreur(s)", dossier, faits, erreurs);
    }

    private void ecrire(Path fichier, byte[] contenu) throws IOException {
        Path temporaire = fichier.resolveSibling(fichier.getFileName() + ".tmp");
        Files.write(temporaire, contenu);
        try {
            Files.move(temporaire, fichier, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temporaire, fichier, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private boolean colonneExiste(String table, String colonne) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() "
                + "AND table_name = ? AND column_name = ?", Integer.class, table, colonne);
        return n != null && n > 0;
    }
}
