package com.sicmagroup.gpr.sla.dto;

import java.time.LocalDateTime;

import com.sicmagroup.gpr.domain.enumeration.ClaimStatus;
import com.sicmagroup.gpr.domain.enumeration.ClaimType;
import com.sicmagroup.gpr.domain.enumeration.GravityLevel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Ligne de l'écran « Suivi SLA ». Jamais de nom, téléphone, e-mail ni adresse du client : seulement le code,
 * l'objet, l'agence et les échéances.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlaItem {
    private Long claimId;
    private ClaimType type;
    /** Code interne de la plainte (celui des adresses de l'application). */
    private String code;
    /** Code client, comme dans les listes de plaintes. */
    private String codeClient;
    private ClaimStatus status;
    private String objetLibelle;
    private GravityLevel gravity;
    private String servicePointLibelle;
    private LocalDateTime receiptDateTime;
    /** Personne qui a enregistré la plainte (sert à décider si elle peut l'ouvrir). */
    private Long collectorId;
    private SlaInfo sla;
}
