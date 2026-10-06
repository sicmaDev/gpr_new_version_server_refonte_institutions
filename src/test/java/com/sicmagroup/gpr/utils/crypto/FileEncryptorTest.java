package com.sicmagroup.gpr.utils.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileEncryptorTest {

    @TempDir
    Path dir;

    private final byte[] contenu = new byte[300_000];

    private static byte[] randomKey() {
        byte[] k = new byte[32];
        new SecureRandom().nextBytes(k);
        return k;
    }

    @BeforeEach
    void setUp() {
        FieldEncryptor.install(new FieldEncryptor(randomKey(), randomKey()));
        new SecureRandom().nextBytes(contenu);
        // Un motif reconnaissable pour vérifier qu'il n'apparaît plus en clair sur le disque
        byte[] motif = "RELEVE BANCAIRE DE KOFFI".getBytes(StandardCharsets.UTF_8);
        System.arraycopy(motif, 0, contenu, 1000, motif.length);
    }

    @Test
    void ecritureEtRelectureIdentiques() throws Exception {
        Path f = dir.resolve("piece.pdf");
        FileEncryptor.write(new ByteArrayInputStream(contenu), f);
        assertThat(FileEncryptor.read(f)).isEqualTo(contenu);
        assertThat(FileEncryptor.readAsResource(f).getContentAsByteArray()).isEqualTo(contenu);
        assertThat(FileEncryptor.readAsResource(f).getFilename()).isEqualTo("piece.pdf");
        assertThat(Files.exists(dir.resolve("piece.pdf.tmp"))).isFalse();
    }

    @Test
    void fichierIllisibleSurLeDisque() throws Exception {
        Path f = dir.resolve("audio.ogg");
        FileEncryptor.write(contenu, f);
        byte[] surDisque = Files.readAllBytes(f);
        assertThat(FileEncryptor.isEncrypted(f)).isTrue();
        assertThat(new String(surDisque, StandardCharsets.ISO_8859_1)).doesNotContain("RELEVE BANCAIRE DE KOFFI");
        assertThat(surDisque).isNotEqualTo(contenu);
    }

    @Test
    void ancienFichierEnClairRelueTelQuel() throws Exception {
        Path f = dir.resolve("ancien.jpg");
        Files.write(f, contenu);
        assertThat(FileEncryptor.isEncrypted(f)).isFalse();
        assertThat(FileEncryptor.read(f)).isEqualTo(contenu);
    }

    @Test
    void fichierVideEtTresPetit() throws Exception {
        for (byte[] c : new byte[][] { new byte[0], new byte[] { 1, 2, 3 } }) {
            Path f = dir.resolve("petit-" + c.length);
            FileEncryptor.write(c, f);
            assertThat(FileEncryptor.read(f)).isEqualTo(c);
        }
        Path clair = dir.resolve("clair-3");
        Files.write(clair, new byte[] { 1, 2, 3 });
        assertThat(FileEncryptor.read(clair)).containsExactly(1, 2, 3);
    }

    @Test
    void fichierModifieEstDetecte() throws Exception {
        Path f = dir.resolve("modifie.pdf");
        FileEncryptor.write(contenu, f);
        byte[] b = Files.readAllBytes(f);
        b[b.length / 2] ^= 0x01;
        Files.write(f, b);
        assertThatThrownBy(() -> FileEncryptor.read(f)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void mauvaiseCleProvoqueUneErreur() throws Exception {
        Path f = dir.resolve("cle.pdf");
        FileEncryptor.write(contenu, f);
        FieldEncryptor.install(new FieldEncryptor(randomKey(), randomKey()));
        assertThatThrownBy(() -> FileEncryptor.read(f)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void deplacementChiffreAuPassage() throws Exception {
        Path source = dir.resolve("whatsapp.ogg");
        Files.write(source, contenu);
        Path cible = dir.resolve("audio").resolve("whatsapp.ogg");
        Files.createDirectories(cible.getParent());
        FileEncryptor.moveEncrypted(source, cible);
        assertThat(Files.exists(source)).isFalse();
        assertThat(FileEncryptor.isEncrypted(cible)).isTrue();
        assertThat(FileEncryptor.read(cible)).isEqualTo(contenu);
    }
}
