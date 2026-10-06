package com.sicmagroup.gpr.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.sicmagroup.gpr.configuration.FileEncryptionMigration.Mode;
import com.sicmagroup.gpr.utils.crypto.FieldEncryptor;
import com.sicmagroup.gpr.utils.crypto.FileEncryptor;

/** Étape 6 : migration des fichiers existants (copie puis remplacement), sur des dossiers temporaires. */
class FileEncryptionMigrationTest {

    @TempDir
    Path racine;

    private Path preuve;
    private Path audio;
    private final Map<String, byte[]> originaux = new TreeMap<>();

    private static byte[] random(int n) {
        byte[] b = new byte[n];
        new SecureRandom().nextBytes(b);
        return b;
    }

    @BeforeEach
    void setUp() throws Exception {
        FieldEncryptor.install(new FieldEncryptor(random(32), random(32)));
        preuve = Files.createDirectories(racine.resolve("preuve"));
        audio = Files.createDirectories(racine.resolve("claim_audio"));
        for (int i = 0; i < 5; i++) {
            byte[] b = random(1000 + i * 5000);
            Files.write(preuve.resolve("piece-" + i + ".pdf"), b);
            originaux.put("preuve/piece-" + i + ".pdf", b);
        }
        byte[] a = random(40_000);
        Files.write(audio.resolve("vocal.ogg"), a);
        originaux.put("claim_audio/vocal.ogg", a);
        // Un fichier déjà chiffré par l'application (nouvel envoi) et un fichier vide
        byte[] deja = random(2000);
        FileEncryptor.write(deja, preuve.resolve("deja-chiffre.png"));
        originaux.put("preuve/deja-chiffre.png", deja);
        Files.write(preuve.resolve("vide.txt"), new byte[0]);
        originaux.put("preuve/vide.txt", new byte[0]);
    }

    private Map<String, byte[]> brut(Path... dirs) throws Exception {
        Map<String, byte[]> m = new TreeMap<>();
        for (Path d : dirs) {
            try (Stream<Path> s = Files.list(d)) {
                for (Path f : s.toList()) {
                    m.put(d.getFileName() + "/" + f.getFileName(), Files.readAllBytes(f));
                }
            }
        }
        return m;
    }

    private FileEncryptionMigration migration(Mode mode) {
        return new FileEncryptionMigration(mode, List.of(preuve, audio));
    }

    @Test
    void modeNonNeFaitRien() throws Exception {
        Map<String, byte[]> avant = brut(preuve, audio);
        migration(Mode.NON).onStartup();
        assertThat(brut(preuve, audio)).usingRecursiveComparison().isEqualTo(avant);
        assertThat(Files.exists(racine.resolve("preuve_chiffre"))).isFalse();
    }

    @Test
    void modeCopieNeTouchePasAuxOriginaux() throws Exception {
        Map<String, byte[]> avant = brut(preuve, audio);
        Map<Path, Integer> n = migration(Mode.COPIE).run();

        // Originaux intacts
        assertThat(brut(preuve, audio)).usingRecursiveComparison().isEqualTo(avant);
        // 5 pièces + le fichier vide chiffrés dans preuve_chiffre, 1 audio dans claim_audio_chiffre
        assertThat(n.values()).containsExactly(6, 1);

        Path copiePreuve = racine.resolve("preuve_chiffre");
        Path copieAudio = racine.resolve("claim_audio_chiffre");
        for (Map.Entry<String, byte[]> e : originaux.entrySet()) {
            Path copie = racine.resolve(e.getKey().replace("preuve/", "preuve_chiffre/")
                    .replace("claim_audio/", "claim_audio_chiffre/"));
            assertThat(FileEncryptor.isEncrypted(copie)).as(e.getKey()).isTrue();
            assertThat(FileEncryptor.read(copie)).as(e.getKey()).isEqualTo(e.getValue());
        }

        // 2e exécution : rien de plus
        Map<String, byte[]> copies = brut(copiePreuve, copieAudio);
        assertThat(migration(Mode.COPIE).run().values()).containsOnly(0);
        assertThat(brut(copiePreuve, copieAudio)).usingRecursiveComparison().isEqualTo(copies);
    }

    @Test
    void modeRemplacerChiffreSurPlace() throws Exception {
        Map<Path, Integer> n = migration(Mode.REMPLACER).run();
        assertThat(n.values()).containsExactly(6, 1);

        for (Map.Entry<String, byte[]> e : originaux.entrySet()) {
            Path f = racine.resolve(e.getKey());
            assertThat(FileEncryptor.isEncrypted(f)).as(e.getKey()).isTrue();
            assertThat(FileEncryptor.read(f)).as(e.getKey()).isEqualTo(e.getValue());
        }
        try (Stream<Path> s = Files.list(preuve)) {
            assertThat(s.filter(p -> p.toString().endsWith(".tmp"))).isEmpty();
        }

        // 2e exécution : ne change rien
        Map<String, byte[]> apres = brut(preuve, audio);
        assertThat(migration(Mode.REMPLACER).run().values()).containsOnly(0);
        assertThat(brut(preuve, audio)).usingRecursiveComparison().isEqualTo(apres);
    }

    @Test
    void valeurInconnueRefusee() {
        assertThat(FileEncryptionMigration.parse(null)).isEqualTo(Mode.NON);
        assertThat(FileEncryptionMigration.parse(" Copie ")).isEqualTo(Mode.COPIE);
        assertThatThrownBy(() -> FileEncryptionMigration.parse("oui"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("GPR_ENCRYPT_FILES");
    }

    @Test
    void dossierAbsentIgnore() {
        FileEncryptionMigration m = new FileEncryptionMigration(Mode.REMPLACER, List.of(racine.resolve("inexistant")));
        assertThat(m.run()).isEmpty();
    }
}
