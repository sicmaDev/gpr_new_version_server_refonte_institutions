package com.sicmagroup.gpr.sla.dto;

import java.time.LocalDateTime;

import com.sicmagroup.gpr.domain.enumeration.ClaimStatus;
import com.sicmagroup.gpr.domain.enumeration.ClaimType;
import com.sicmagroup.gpr.domain.enumeration.GravityLevel;
import com.sicmagroup.gpr.sla.domain.ClaimSla;

/**
 * Lignes de lecture légères (une requête, seulement les colonnes utiles) : évite de charger les plaintes
 * entières avec toutes leurs tables liées, ce qui était très lent sur des dizaines de milliers de plaintes.
 */
public final class SlaRows {

    private SlaRows() {
    }

    /** Plainte en retard (page Alertes, tableau de bord). */
    public record OverdueRow(Long claimId, ClaimType type, LocalDateTime dueAt, String code, String codeClient,
            String clientName, ClaimStatus status, LocalDateTime receiptDateTime, String objet, GravityLevel risk,
            String agence) {
    }

    /** Ligne d'affichage de l'écran de suivi. */
    public record ItemRow(Long claimId, ClaimType type, String code, String codeClient, ClaimStatus status,
            String objet, GravityLevel risk, String agence, LocalDateTime receiptDateTime) {
    }

    /** Compteur et attributs de la plainte pour les indicateurs et les rapports. */
    public record StatsRow(ClaimSla sla, GravityLevel risk, String category, String objet, String agence,
            String agent, String channel) {
    }
}
