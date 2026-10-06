package com.sicmagroup.gpr.utils.crypto;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Chiffrement des données sensibles en base.
 *
 * AES-256-GCM, IV aléatoire de 12 octets, tag de 128 bits.
 * Format stocké : "ENC:v1:" + Base64(IV + texte chiffré + tag).
 * Une valeur sans ce préfixe (ancienne donnée en clair) est relue telle quelle.
 *
 * blindIndex : HMAC-SHA256 (clé distincte) pour rechercher une valeur chiffrée
 * (ex : téléphone) sans la déchiffrer.
 */
public final class FieldEncryptor {

    public static final String PREFIX = "ENC:v1:";
    public static final int KEY_LENGTH = 32;

    private static final String CIPHER = "AES/GCM/NoPadding";
    private static final String HMAC = "HmacSHA256";
    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;

    private static volatile FieldEncryptor current;

    private final SecretKeySpec encryptionKey;
    private final SecretKeySpec hmacKey;
    private final SecureRandom random = new SecureRandom();

    public FieldEncryptor(byte[] encryptionKey, byte[] hmacKey) {
        if (encryptionKey == null || encryptionKey.length != KEY_LENGTH) {
            throw new IllegalArgumentException("La clé de chiffrement doit faire exactement " + KEY_LENGTH + " octets");
        }
        if (hmacKey == null || hmacKey.length != KEY_LENGTH) {
            throw new IllegalArgumentException("La clé HMAC doit faire exactement " + KEY_LENGTH + " octets");
        }
        if (MessageDigest.isEqual(encryptionKey, hmacKey)) {
            throw new IllegalArgumentException("La clé de chiffrement et la clé HMAC doivent être différentes");
        }
        this.encryptionKey = new SecretKeySpec(encryptionKey.clone(), "AES");
        this.hmacKey = new SecretKeySpec(hmacKey.clone(), HMAC);
    }

    /** Crée l'encrypteur à partir de deux clés Base64 (32 octets chacune). */
    public static FieldEncryptor fromBase64(String encryptionKeyBase64, String hmacKeyBase64) {
        return new FieldEncryptor(
                decodeKey(encryptionKeyBase64, "GPR_ENCRYPTION_KEY"),
                decodeKey(hmacKeyBase64, "GPR_HMAC_KEY"));
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

    /** Empreinte HMAC-SHA256 (hexadécimal), espaces supprimés ; null si la valeur est vide. */
    public String blindIndex(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.replaceAll("\\s+", "");
        if (normalized.isEmpty()) {
            return null;
        }
        try {
            Mac mac = Mac.getInstance(HMAC);
            mac.init(hmacKey);
            return HexFormat.of().formatHex(mac.doFinal(normalized.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Échec du calcul de l'empreinte", e);
        }
    }
}
