package com.sicmagroup.gpr.sla.service;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Au démarrage : crée la configuration par défaut (politiques, calendrier, motifs) si elle n'existe pas.
 * Cela ne change aucun comportement de GPR tant que « sla.enabled » n'est pas à true.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SlaBootstrap {

    private final SlaPolicyService policyService;

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        try {
            policyService.seedDefaults();
        } catch (Exception e) {
            log.warn("SLA : configuration par défaut non créée : {}", e.toString());
        }
    }
}
