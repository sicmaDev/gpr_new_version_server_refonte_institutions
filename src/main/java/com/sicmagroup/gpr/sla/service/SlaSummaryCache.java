package com.sicmagroup.gpr.sla.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mémoire de quelques secondes pour le résumé du SLA (badge du menu, cartes, totaux des listes). Elle est vidée à
 * chaque changement d'une plainte (événement, action, contrôle) : l'affichage ne retarde donc jamais d'une
 * action, et au pire d'une minute pour les changements de date (une échéance qui passe).
 */
public final class SlaSummaryCache {

    private static final long TTL_MS = 60_000;
    private static final int MAX_ENTRIES = 500;

    private record Entry(long at, Map<String, Object> value) {
    }

    private static final Map<String, Entry> ENTRIES = new ConcurrentHashMap<>();

    private SlaSummaryCache() {
    }

    public static Map<String, Object> get(String key) {
        Entry e = ENTRIES.get(key);
        return e != null && System.currentTimeMillis() - e.at() < TTL_MS ? e.value() : null;
    }

    public static void put(String key, Map<String, Object> value) {
        if (ENTRIES.size() >= MAX_ENTRIES) {
            ENTRIES.clear();
        }
        ENTRIES.put(key, new Entry(System.currentTimeMillis(), value));
    }

    public static void clear() {
        ENTRIES.clear();
    }
}