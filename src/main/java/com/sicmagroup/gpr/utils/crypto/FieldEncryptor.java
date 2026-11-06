package com.sicmagroup.gpr.utils.crypto;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Chiffrement des réglages mail et SMS et du mot de passe temporaire des comptes.
 *
 * AES-256-GCM, IV aléatoire de 12 octets, tag de 128 bits.
 * Format stocké : "ENC:v1:" + Base64(IV + texte chiffré + tag).
 * Une valeur sans ce préfixe est relue telle quelle.
 */
public final class FieldEncryptor {

    public static final String PREFIX = "ENC:v1:";
    public static final int KEY_LENGTH = 32;

    private static final String CIPHER = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;

    private static volatile FieldEncryptor current;

    private final SecretKeySpec encryptionKey;
    private final SecureRandom random = new SecureRandom();

    public FieldEncryptor(byte[] encryptionKey) {
        if (encryptionKey == null || encryptionKey.length != KEY_LENGTH) {
            throw new IllegalArgumentException("La clé de chiffrement doit faire exactement " + KEY_LENGTH + " octets");
        }
        this.encryptionKey = new SecretKeySpec(encryptionKey.clone(), "AES");
    }

    /** Crée l'encrypteur à partir d'une clé Base64 de 32 octets. */
    public static FieldEncryptor fromBase64(String encryptionKeyBase64) {
        return new FieldEncryptor(decodeKey(encryptionKeyBase64, "GPR_ENCRYPTION_KEY"));
    }

    private static byte[] decodeKey(String base64, String name) {
        if (base64 == null || base64.isBlank()) {
            throw new IllegalArgumentException(name + " est absente");
        }
        byte[] key;
        try {
            key = Base64.getDecoder().decode(base64.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(name + " n'est pas une valeur Base64 valide");
        }
        if (key.length != KEY_LENGTH) {
            throw new IllegalArgumentException(name + " doit faire " + KEY_LENGTH + " octets (reçu : " + key.length + ")");
        }
        return key;
    }

    /** Rend l'encrypteur accessible aux convertisseurs JPA et aux entités. */
    public static void install(FieldEncryptor encryptor) {
        current = encryptor;
    }

    public static FieldEncryptor current() {
        FieldEncryptor encryptor = current;
        if (encryptor == null) {
            throw new IllegalStateException("FieldEncryptor non initialisé (EncryptionConfig n'a pas été chargé)");
        }
        return encryptor;
    }

    public static boolean isEncrypted(String value) {
        return value != null && value.startsWith(PREFIX);
    }

    public String encrypt(String plainText) {
        if (plainText == null) {
            return null;
        }
        return PREFIX + Base64.getEncoder().encodeToString(encryptBytes(plainText.getBytes(StandardCharsets.UTF_8)));
    }

    public String decrypt(String stored) {
        if (stored == null || !stored.startsWith(PREFIX)) {
            // null ou ancienne donnée en clair : renvoyée telle quelle
            return stored;
        }
        byte[] data;
        try {
            data = Base64.getDecoder().decode(stored.substring(PREFIX.length()));
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Impossible de déchiffrer la donnée (clé incorrecte ou donnée altérée)", e);
        }
        return new String(decryptBytes(data), StandardCharsets.UTF_8);
    }

    /** Chiffre des octets : renvoie IV (12 octets) + texte chiffré + tag. */
    public byte[] encryptBytes(byte[] plain) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(CIPHER);
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plain);
            return ByteBuffer.allocate(IV_LENGTH + encrypted.length).put(iv).put(encrypted).array();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Échec du chiffrement", e);
        }
    }

    /** Déchiffre des octets produits par {@link #encryptBytes(byte[])} (offset = début de l'IV). */
    public byte[] decryptBytes(byte[] data, int offset) {
        try {
            if (data.length - offset < IV_LENGTH + TAG_BITS / 8) {
                throw new IllegalStateException("Donnée chiffrée tronquée");
            }
            Cipher cipher = Cipher.getInstance(CIPHER);
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey, new GCMParameterSpec(TAG_BITS, data, offset, IV_LENGTH));
            return cipher.doFinal(data, offset + IV_LENGTH, data.length - offset - IV_LENGTH);
        } catch (GeneralSecurityException e) {
            // Mauvaise clé ou donnée modifiée
            throw new IllegalStateException("Impossible de déchiffrer la donnée (clé incorrecte ou donnée altérée)", e);
        }
    }

    public byte[] decryptBytes(byte[] data) {
        return decryptBytes(data, 0);
    }
}
