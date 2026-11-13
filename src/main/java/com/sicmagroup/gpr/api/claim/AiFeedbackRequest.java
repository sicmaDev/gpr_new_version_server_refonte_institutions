package com.sicmagroup.gpr.api.claim;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sicmagroup.gpr.domain.enumeration.AiFeedbackStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Retour de l'agent sur l'IA (repris de l'ancienne version GPR). L'agent est pris dans la connexion, pas dans la demande. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiFeedbackRequest {
    private String finalCategory;
    private String finalMotif;
    private String finalUrgency;
    private String retainedSolution;
    private AiFeedbackStatus status;
    private String justification;
    private String satisfactionStatus;
}
