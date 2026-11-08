package com.sicmagroup.gpr.sla.service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.sicmagroup.gpr.domain.model.Setting;
import com.sicmagroup.gpr.repository.SettingRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Paramètres du SLA, lus dans gps_setting (clés « sla.* »). Tant que « sla.enabled » n'est pas à true,
 * GPR se comporte exactement comme avant. Les valeurs sont mises en cache quelques secondes.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SlaConfig {

    public static final String ENABLED = "sla.enabled";
    public static final String TIMEZONE = "sla.timezone";
    public static final String REGULATORY_DAYS = "sla.regulatory_days";
    public static final String REGULATORY_WARNING_DAYS = "sla.regulatory_warning_days";
    public static final String AUTO_ESCALATION = "sla.auto_escalation";
    public static final String DAILY_DIGEST = "sla.daily_digest";
    public static final String DIGEST_TIME = "sla.digest_time";
    public static final String SCAN_MINUTES = "sla.scan_minutes";
    public static final String UNREACHABLE_ATTEMPTS = "sla.unreachable_attempts";
    public static final String UNREACHABLE_DAYS = "sla.unreachable_days";
    public static final String CUSTOMER_WAITING_MESSAGE = "sla.customer_waiting_message";
    public static final String TAKEOVER_HOURS = "sla.takeover_hours";
    public static final String CLOSURE_DAYS = "sla.closure_days";
    public static final String REOPEN_DAYS = "sla.reopen_days";
    public static final String REMINDER1_PCT = "sla.reminder1_pct";
    public static final String REMINDER2_PCT = "sla.reminder2_pct";
    public static final String GRACE_HOURS = "sla.grace_hours";
    public static final String SUGGESTION_DAYS = "sla.suggestion_days";

    private static final long CACHE_MS = 15_000;

    private final SettingRepository settingRepository;

    private volatile Map<String, String> cache = Map.of();
    private volatile long loadedAt = 0;

    private Map<String, String> values() {
        long now = System.currentTimeMillis();
        if (now - loadedAt > CACHE_MS) {
            Map<String, String> fresh = new HashMap<>();
            try {
                for (Setting s : settingRepository.findByLibelleStartingWith("sla.")) {
                    fresh.put(s.getLibelle(), s.getValue());
                }
                cache = fresh;
            } catch (Exception e) {
                // lecture impossible (base, clé de chiffrement...) : on garde les dernières valeurs connues,
                // ou les défauts (SLA désactivé) ; jamais d'erreur remontée à l'appelant
                log.warn("SLA : paramètres illisibles, valeurs précédentes conservées : {}", e.toString());
            }
            loadedAt = now;
        }
        return cache;
    }

    public String get(String key, String defaultValue) {
        String v = values().get(key);
        return v == null || v.isBlank() ? defaultValue : v.trim();
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return Boolean.parseBoolean(get(key, String.valueOf(defaultValue)));
    }

    public int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(get(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public boolean enabled() {
        return getBoolean(ENABLED, false);
    }

    public boolean autoEscalation() {
        return getBoolean(AUTO_ESCALATION, true);
    }

    public boolean dailyDigest() {
        return getBoolean(DAILY_DIGEST, true);
    }

    public int regulatoryDays() {
        return getInt(REGULATORY_DAYS, 30);
    }

    public int regulatoryWarningDays() {
        return getInt(REGULATORY_WARNING_DAYS, 5);
    }

    public int unreachableAttempts() {
        return getInt(UNREACHABLE_ATTEMPTS, 3);
    }

    public int unreachableDays() {
        return getInt(UNREACHABLE_DAYS, 7);
    }

    public String customerWaitingMessage() {
        return get(CUSTOMER_WAITING_MESSAGE, "NONE");
    }

    /** Prise en charge par défaut, en heures ouvrées (l'objet peut avoir la sienne). */
    public int takeoverHours() {
        return getInt(TAKEOVER_HOURS, 9);
    }

    /** Clôture des réclamations (solution approuvée -> mesure de satisfaction), en jours ouvrés. */
    public int closureDays() {
        return getInt(CLOSURE_DAYS, 3);
    }

    /** Nouveau cycle après une mesure « non satisfait » ou « partiellement satisfait », en jours ouvrés. */
    public int reopenDays() {
        return getInt(REOPEN_DAYS, 3);
    }

    public int reminder1Pct() {
        return getInt(REMINDER1_PCT, 50);
    }

    public int reminder2Pct() {
        return getInt(REMINDER2_PCT, 75);
    }

    /** Délai avant la remontée automatique, après le dépassement, en heures ouvrées. */
    public int graceHours() {
        return getInt(GRACE_HOURS, 9);
    }

    /** Traitement d'une suggestion (elle n'a pas d'objet), en jours ouvrés. */
    public int suggestionDays() {
        return getInt(SUGGESTION_DAYS, 10);
    }

    public LocalTime digestTime() {
        try {
            return LocalTime.parse(get(DIGEST_TIME, "08:00"));
        } catch (Exception e) {
            return LocalTime.of(8, 0);
        }
    }

    /** Toutes les valeurs « sla.* » avec leurs défauts, pour l'écran de configuration. */
    public Map<String, String> all() {
        Map<String, String> out = new HashMap<>();
        out.put(ENABLED, String.valueOf(enabled()));
        out.put(TIMEZONE, get(TIMEZONE, com.sicmagroup.gpr.GprApplication.resolveTimezone()));
        out.put(REGULATORY_DAYS, String.valueOf(regulatoryDays()));
        out.put(REGULATORY_WARNING_DAYS, String.valueOf(regulatoryWarningDays()));
        out.put(AUTO_ESCALATION, String.valueOf(autoEscalation()));
        out.put(DAILY_DIGEST, String.valueOf(dailyDigest()));
        out.put(DIGEST_TIME, digestTime().toString());
        out.put(SCAN_MINUTES, String.valueOf(getInt(SCAN_MINUTES, 15)));
        out.put(UNREACHABLE_ATTEMPTS, String.valueOf(unreachableAttempts()));
        out.put(UNREACHABLE_DAYS, String.valueOf(unreachableDays()));
        out.put(CUSTOMER_WAITING_MESSAGE, customerWaitingMessage());
        out.put(TAKEOVER_HOURS, String.valueOf(takeoverHours()));
        out.put(CLOSURE_DAYS, String.valueOf(closureDays()));
        out.put(REOPEN_DAYS, String.valueOf(reopenDays()));
        out.put(REMINDER1_PCT, String.valueOf(reminder1Pct()));
        out.put(REMINDER2_PCT, String.valueOf(reminder2Pct()));
        out.put(GRACE_HOURS, String.valueOf(graceHours()));
        out.put(SUGGESTION_DAYS, String.valueOf(suggestionDays()));
        return out;
    }

    /** Oublie le cache : les valeurs sont relues à la prochaine demande. */
    public void invalidate() {
        loadedAt = 0;
    }

    /** Crée ou met à jour un paramètre « sla.* ». */
    public void set(String key, String value) {
        if (key == null || !key.startsWith("sla.")) {
            throw new IllegalArgumentException("Paramètre SLA inconnu : " + key);
        }
        Setting setting = settingRepository.findByLibelle(key).orElse(null);
        LocalDateTime now = LocalDateTime.now();
        if (setting == null) {
            setting = Setting.builder().libelle(key).createdAt(now).build();
        }
        setting.setValue(value);
        setting.setUpdatedAt(now);
        settingRepository.save(setting);
        loadedAt = 0;
    }
}
