package com.sicmagroup.gpr.utils;

import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sicmagroup.gpr.domain.model.Setting;

/**
 * Mots de passe SMTP (pwd) et SMS (valMdp) stockés dans gps_setting.value (JSON) :
 * - jamais renvoyés au navigateur (valeur remplacée par null) ;
 * - conservés à l'enregistrement si le navigateur envoie une valeur vide.
 */
public final class SettingSecrets {

    public static final Set<String> SECRET_KEYS = Set.of("pwd", "valMdp");

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private SettingSecrets() {
    }

    /** Renvoie le JSON sans les mots de passe ; une valeur non JSON est renvoyée telle quelle. */
    public static String withoutSecrets(String json) {
        ObjectNode node = parse(json);
        if (node == null) {
            return json;
        }
        boolean changed = false;
        for (String key : SECRET_KEYS) {
            if (node.has(key)) {
                node.putNull(key);
                changed = true;
            }
        }
        return changed ? node.toString() : json;
    }

    /** Copie détachée du paramètre, sans mots de passe (ne jamais modifier l'entité gérée par JPA). */
    public static Setting withoutSecrets(Setting setting) {
        if (setting == null) {
            return null;
        }
        return Setting.builder()
                .id(setting.getId())
                .libelle(setting.getLibelle())
                .value(withoutSecrets(setting.getValue()))
                .createdAt(setting.getCreatedAt())
                .updatedAt(setting.getUpdatedAt())
                .build();
    }

    public static List<Setting> withoutSecrets(List<Setting> settings) {
        return settings == null ? null : settings.stream().map(SettingSecrets::withoutSecrets).toList();
    }

    /**
     * Si le nouveau JSON a un mot de passe vide (le navigateur ne le connaît plus), on garde l'ancien.
     */
    public static String keepOldSecrets(String newJson, String oldJson) {
        ObjectNode nouveau = parse(newJson);
        ObjectNode ancien = parse(oldJson);
        if (nouveau == null || ancien == null) {
            return newJson;
        }
        boolean changed = false;
        for (String key : SECRET_KEYS) {
            JsonNode n = nouveau.get(key);
            JsonNode a = ancien.get(key);
            boolean vide = n == null || n.isNull() || n.asText().isBlank();
            if (vide && a != null && !a.isNull() && !a.asText().isBlank()) {
                nouveau.set(key, a);
                changed = true;
            }
        }
        return changed ? nouveau.toString() : newJson;
    }

    private static ObjectNode parse(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            JsonNode node = MAPPER.readTree(json);
            return node instanceof ObjectNode o ? o : null;
        } catch (Exception e) {
            return null;
        }
    }
}
