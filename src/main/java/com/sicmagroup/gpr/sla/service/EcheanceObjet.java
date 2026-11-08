package com.sicmagroup.gpr.sla.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.sicmagroup.gpr.domain.model.Claim;

import lombok.RequiredArgsConstructor;

/**
 * Échéance de traitement d'une plainte : la date de réception plus le délai de l'objet, en jours ouvrés
 * (calendrier et jours fériés de la configuration). Même calcul pour le suivi, les alertes et les rapports.
 */
@Service
@RequiredArgsConstructor
public class EcheanceObjet {

    private final BusinessTime businessTime;

    /** Sans délai (0 ou moins), l'échéance est la date de départ. */
    public LocalDateTime echeance(LocalDateTime depart, long jours) {
        if (jours <= 0) {
            return depart;
        }
        return businessTime.add(true, depart, jours * businessTime.workdayMinutes());
    }

    public LocalDateTime echeance(Claim claim) {
        LocalDateTime depart = claim.getReceiptDateTime() != null ? claim.getReceiptDateTime() : claim.getCreatedAt();
        return echeance(depart, claim.getObjet() == null ? 0 : claim.getObjet().getProcessingTime());
    }
}
