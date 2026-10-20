package com.sicmagroup.gpr.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Résultat d'un envoi manuel de test d'une relance planifiée (bouton « Tester l'envoi »). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduledReportTestResult {
    private boolean success;
    private String message;
    private int claimCount;
}
