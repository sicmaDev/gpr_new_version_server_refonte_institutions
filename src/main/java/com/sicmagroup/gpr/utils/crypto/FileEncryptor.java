package com.sicmagroup.gpr.utils.crypto;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

/**
 * Chiffrement des fichiers stockés sur disque (pièces jointes, audios).
 *
 * Format : "GPRENC1" (7 octets) + IV (12 octets) + contenu chiffré + tag (AES-256-GCM, même clé
 * que les colonnes). Un fichier sans cet en-tête (ancien fichier en clair) est relu tel quel.
 */
public final class FileEncryptor {

    public static final byte[] MAGIC = "GPRENC1".getBytes(StandardCharsets.US_ASCII);

    private FileEncryptor() {
    }

    public static boolean isEncrypted(byte[] content) {
        return content != null && content.length >= MAGIC.length
                && Arrays.equals(content, 0, MAGIC.length, MAGIC, 0, MAGIC.length);
    }

    public static boolean isEncrypted(Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            return isEncrypted(in.readNBytes(MAGIC.length));
        }
    }

    public static byte[] encrypt(byte[] plain) {
        byte[] body = FieldEncryptor.current().encryptBytes(plain);
        byte[] out = Arrays.copyOf(MAGIC, MAGIC.length + body.length);
        System.arraycopy(body, 0, out, MAGIC.length, body.length);
        return out;
    }

    /** Déchiffre un contenu ; un contenu sans en-tête (ancien fichier en clair) est renvoyé tel quel. */
    public static byte[] decrypt(byte[] content) {
        if (!isEncrypted(content)) {
            return content;
        }
        return FieldEncryptor.current().decryptBytes(content, MAGIC.length);
    }

    /** Écrit le fichier chiffré (via un fichier temporaire, pour ne jamais laisser un fichier à moitié écrit). */
    public static void write(byte[] plain, Path target) throws IOException {
        writeAlreadyEncrypted(encrypt(plain), target);
    }

    /** Écrit un contenu déjà chiffré par {@link #encrypt(byte[])} (écriture atomique). */
    public static void writeAlreadyEncrypted(byte[] encrypted, Path target) throws IOException {
        if (!isEncrypted(encrypted)) {
            throw new IllegalArgumentException("Contenu non chiffré");
        }
        Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
        Files.write(tmp, encrypted);
        try {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static void write(InputStream in, Path target) throws IOException {
        write(in.readAllBytes(), target);
    }

    /** Lit et déchiffre un fichier (ou le renvoie tel quel s'il est encore en clair). */
    public static byte[] read(Path file) throws IOException {
        return decrypt(Files.readAllBytes(file));
    }

    /** Ressource déchiffrée, prête à être renvoyée par un contrôleur de téléchargement. */
    public static Resource readAsResource(Path file) throws IOException {
        String name = file.getFileName().toString();
        return new ByteArrayResource(read(file)) {
            @Override
            public String getFilename() {
                return name;
            }
        };
    }

    /** Déplace un fichier en le chiffrant au passage s'il est encore en clair. */
    public static void moveEncrypted(Path source, Path target) throws IOException {
        if (isEncrypted(source)) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        } else {
            write(Files.readAllBytes(source), target);
            Files.delete(source);
        }
    }
}
