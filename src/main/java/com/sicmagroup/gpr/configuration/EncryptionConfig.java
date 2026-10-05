package com.sicmagroup.gpr.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import com.sicmagroup.gpr.utils.crypto.FieldEncryptor;

/**
 * Charge les clés de chiffrement depuis gpr.crypto.key / gpr.crypto.hmac-key
 * (variables d'environnement GPR_ENCRYPTION_KEY / GPR_HMAC_KEY).
 * L'application refuse de démarrer si une clé est absente ou invalide.
 */
@Configuration
public class EncryptionConfig {

    private static final String HELP = " Générez une clé avec : openssl rand -base64 32"
            + " (deux clés différentes pour GPR_ENCRYPTION_KEY et GPR_HMAC_KEY).";

    @Bean
    public FieldEncryptor fieldEncryptor(Environment environment) {
        FieldEncryptor encryptor = create(environment);
        FieldEncryptor.install(encryptor);
        return encryptor;
    }

    static FieldEncryptor create(Environment environment) {
        String key = read(environment, "gpr.crypto.key");
        String hmacKey = read(environment, "gpr.crypto.hmac-key");
        try {
            return FieldEncryptor.fromBase64(key, hmacKey);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Démarrage impossible : " + e.getMessage() + "." + HELP);
        }
    }

    private static String read(Environment environment, String property) {
        try {
            return environment.getProperty(property);
        } catch (IllegalArgumentException e) {
            // Placeholder ${GPR_...} non résolu : variable d'environnement absente
            return null;
        }
    }
}
