package com.sicmagroup.gpr.service.claim;

public class ClaimException extends Exception {

    private final String key; // clé d'erreur personnalisée (peut être null)

    // 🔹 Constructeur principal avec clé et message
    public ClaimException(String key, String message) {
        super(message);
        this.key = key;
    }

    // 🔹 Constructeur avec clé, message et cause
    public ClaimException(String key, String message, Throwable cause) {
        super(message, cause);
        this.key = key;
    }

    // 🔹 Constructeur simplifié (clé par défaut null)
    public ClaimException(String message) {
        super(message);
        this.key = null;
    }

    // 🔹 Constructeur simplifié avec cause (clé par défaut null)
    public ClaimException(String message, Throwable cause) {
        super(message, cause);
        this.key = null;
    }

    // 🔹 Getter pour la clé
    public String getKey() {
        return key;
    }
}

