package com.sicmagroup.gpr.sla.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.sicmagroup.gpr.domain.enumeration.ClaimEventType;
import com.sicmagroup.gpr.domain.enumeration.ClaimType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Pont entre le journal des événements (claim-events) et le moteur SLA.
 * Règle absolue : le SLA ne doit JAMAIS bloquer ni faire échouer le traitement d'une plainte. Toute erreur
 * est notée dans les journaux et ignorée. Le moteur travaille après la validation (commit) de l'action de
 * l'agent, dans sa propre transaction.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SlaEventHook {

    private final SlaEngine engine;
    private final SlaConfig config;

    public void afterEvent(Long claimId, ClaimType type, ClaimEventType event) {
        try {
            if (!config.enabled()) {
                return;
            }
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        run(claimId, type, event);
                    }
                });
            } else {
                run(claimId, type, event);
            }
        } catch (Exception e) {
            log.warn("SLA : événement {} de la plainte {} ignoré : {}", event, claimId, e.toString());
        }
    }

    private void run(Long claimId, ClaimType type, ClaimEventType event) {
        try {
            engine.onEvent(claimId, type, event);
        } catch (Exception e) {
            log.warn("SLA : mise à jour du compteur de la plainte {} échouée ({}) : {}", claimId, event, e.toString());
        }
    }
}
