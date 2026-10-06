package com.sicmagroup.gpr.configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import com.sicmagroup.gpr.utils.Constante;
import com.sicmagroup.gpr.utils.crypto.FileEncryptor;

import jakarta.annotation.PostConstruct;

/**
 * Migration des fichiers existants (pièces jointes et audios) vers des fichiers chiffrés (étape 6).
 *
 * gpr.crypto.migrate-files (variable GPR_ENCRYPT_FILES) :
 * - "non" (défaut) : rien ;
 * - "copie" : écrit les versions chiffrées dans un dossier à part (<dossier>_chiffre), sans toucher
 *   aux originaux, pour validation ;
 * - "remplacer" : chiffre les originaux sur place (à faire après validation et sauvegarde des dossiers).
 *
 * Idempotent : un fichier déjà chiffré (en-tête GPRENC1) n'est jamais rechiffré ; chaque fichier est
 * vérifié par déchiffrement avant d'être écrit.
 */
@Component
@DependsOn("encryptionConfig")
public class FileEncryptionMigration {

    private static final Logger log = LoggerFactory.getLogger(FileEncryptionMigration.class);

    public enum Mode { NON, COPIE, REMPLACER }

    static final String COPY_SUFFIX = "_chiffre";

    private final Mode mode;
    private final List<Path> directories;

    @Autowired
    public FileEncryptionMigration(@Value("${gpr.crypto.migrate-files:non}") String mode) {
        this(parse(mode), List.of(
                Paths.get(Constante.DEVMODE ? Constante.TEST_PATH_PIECE_JOINTES : Constante.PROD_PATH_PIECE_JOINTES),
                Paths.get(Constante.DEVMODE ? Constante.TEST_PATH_AUDIO : Constante.PROD_PATH_AUDIO)));
    }

    FileEncryptionMigration(Mode mode, List<Path> directories) {
        this.mode = mode;
        this.directories = directories.stream().map(p -> p.toAbsolutePath().normalize()).toList();
    }

    static Mode parse(String value) {
        String v = value == null ? "" : value.trim().toLowerCase();
        return switch (v) {
            case "", "non", "false", "no" -> Mode.NON;
            case "copie", "copy" -> Mode.COPIE;
            case "remplacer", "replace" -> Mode.REMPLACER;
            default -> throw new IllegalStateException("Démarrage impossible : GPR_ENCRYPT_FILES doit valoir "
                    + "non, copie ou remplacer (reçu : " + value + ")");
        };
    }

    @PostConstruct
    void onStartup() {
        if (mode == Mode.NON) {
            return;
        }
        log.warn("GPR_ENCRYPT_FILES={} : chiffrement des fichiers existants en cours. "
                + "Pensez à remettre la variable à non après ce démarrage.", mode.name().toLowerCase());
        run();
    }

    /** Lance la migration sur tous les dossiers ; renvoie, par dossier, le nombre de fichiers chiffrés. */
    public Map<Path, Integer> run() {
        Map<Path, Integer> result = new LinkedHashMap<>();
        for (Path dir : directories) {
            if (!Files.isDirectory(dir)) {
                log.info("Dossier {} absent : ignoré", dir);
                continue;
            }
            int n = migrateDirectory(dir);
            result.put(dir, n);
        }
        return result;
    }

    private int migrateDirectory(Path dir) {
        Path target = mode == Mode.COPIE ? dir.resolveSibling(dir.getFileName() + COPY_SUFFIX) : dir;
        int encrypted = 0, alreadyEncrypted = 0, errors = 0;
        try {
            Files.createDirectories(target);
        } catch (IOException e) {
            log.error("Impossible de créer le dossier {} : {}", target, e.getMessage());
            return 0;
        }
        List<Path> files;
        try (Stream<Path> s = Files.list(dir)) {
            files = s.filter(Files::isRegularFile)
                    .filter(p -> !p.getFileName().toString().endsWith(".tmp"))
                    .sorted().toList();
        } catch (IOException e) {
            log.error("Impossible de lister le dossier {} : {}", dir, e.getMessage());
            return 0;
        }
        for (Path source : files) {
            Path destination = target.resolve(source.getFileName());
            try {
                byte[] content = Files.readAllBytes(source);
                if (mode == Mode.COPIE && Files.exists(destination) && FileEncryptor.isEncrypted(destination)) {
                    alreadyEncrypted++;
                    continue;
                }
                if (FileEncryptor.isEncrypted(content)) {
                    alreadyEncrypted++;
                    if (mode == Mode.COPIE) {
                        Files.write(destination, content);
                    }
                    continue;
                }
                byte[] cipher = FileEncryptor.encrypt(content);
                if (!Arrays.equals(content, FileEncryptor.decrypt(cipher))) {
                    errors++;
                    log.error("Vérification échouée pour {} : fichier non modifié", source);
                    continue;
                }
                FileEncryptor.writeAlreadyEncrypted(cipher, destination);
                encrypted++;
            } catch (IOException | RuntimeException e) {
                errors++;
                log.error("Erreur sur {} : {} (fichier non modifié)", source, e.getMessage());
            }
        }
        log.info("Fichiers {} ({}) : {} chiffré(s), {} déjà chiffré(s), {} erreur(s){}", dir,
                mode.name().toLowerCase(), encrypted, alreadyEncrypted, errors,
                mode == Mode.COPIE ? " -> copies dans " + target : "");
        return encrypted;
    }
}
