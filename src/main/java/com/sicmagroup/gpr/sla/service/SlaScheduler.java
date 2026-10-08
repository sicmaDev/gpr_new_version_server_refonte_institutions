package com.sicmagroup.gpr.sla.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.sicmagroup.gpr.sla.domain.ClaimSla;
import com.sicmagroup.gpr.sla.repository.ClaimSlaRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Tâche planifiée du SLA. Elle se réveille chaque minute mais ne travaille que toutes les « sla.scan_minutes »
 * (15 par défaut) et seulement si « sla.enabled » est à true. À chaque passage :
 * - rattrapage des plaintes sans compteur (nouvelles, hors ligne, robot, restaurées) ;
 * - traitement des compteurs dont le prochain contrôle est arrivé (rappels, alertes, remontées) ;
 * - récapitulatif quotidien.
 * La reprise des plaintes déjà ouvertes à l'activation se fait une seule fois, sans alerte rétroactive.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SlaScheduler {

    private static final String BACKFILL_DONE = "sla.backfill_done";
    private static final int PAGE = 100;
    private static final int MAX_PAGES = 20;

    private final SlaConfig config;
    private final SlaEngine engine;
    private final SlaScanner scanner;
    private final SlaDigest digest;
    private final ClaimSlaRepository repository;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile long lastScan = 0;

    @Scheduled(fixedDelay = 60_000, initialDelay = 120_000)
    public void tick() {
        if (!config.enabled() || !running.compareAndSet(false, true)) {
            return;
        }
        try {
            long interval = Math.max(1, config.getInt(SlaConfig.SCAN_MINUTES, 15)) * 60_000L;
            if (System.currentTimeMillis() - lastScan >= interval) {
                lastScan = System.currentTimeMillis();
                scan();
            }
            digest.sendIfDue(LocalDateTime.now());
        } catch (Exception e) {
            log.warn("SLA : passage de la tâche planifiée échoué : {}", e.toString());
        } finally {
            running.set(false);
        }
    }

    /** Un passage complet (aussi appelé par les tests). Renvoie le nombre de compteurs traités. */
    public int scan() {
        backfillOnce();
        engine.syncDeletions();
        engine.reconcileMissing(200, false);
        int processed = 0;
        for (int page = 0; page < MAX_PAGES; page++) {
            List<ClaimSla> due = repository.findDue(LocalDateTime.now(), PageRequest.of(0, PAGE));
            if (due.isEmpty()) {
                break;
            }
            for (ClaimSla s : due) {
                try {
                    scanner.processOne(s.getId());
                    processed++;
                } catch (Exception e) {
                    // conflit de version ou erreur ponctuelle : le compteur sera revu au prochain passage
                    log.warn("SLA : compteur {} non traité : {}", s.getId(), e.toString());
                }
            }
            if (due.size() < PAGE) {
                break;
            }
        }
        return processed;
    }

    /** Reprise des plaintes ouvertes à l'activation du SLA : compteurs créés sans aucune alerte rétroactive. */
    private void backfillOnce() {
        if (config.getBoolean(BACKFILL_DONE, false)) {
            return;
        }
        int total = 0;
        for (int i = 0; i < 1000; i++) {
            int created = engine.reconcileMissing(200, true);
            total += created;
            if (created == 0) {
                break;
            }
        }
        config.set(BACKFILL_DONE, "true");
        log.info("SLA : reprise terminée, {} compteur(s) créé(s) sans alerte rétroactive", total);
    }
}
