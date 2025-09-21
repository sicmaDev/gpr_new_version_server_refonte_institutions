package com.sicmagroup.gpr.api.claim;

import java.time.LocalDateTime;

import com.sicmagroup.gpr.domain.enumeration.ClaimType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistoriqueAffectationResponse {
      private Long id;
    private Long claimId;
    private String codePlainte;
    private ClaimType typePlainte;
    private String nomAgent;
    private String emailAgent;
    private String telephoneAgent;
    private LocalDateTime dateAffectation;
    private Integer delaiJours;
    private LocalDateTime dateFinAffectation;
    private Boolean mailEnvoye;
    private Boolean smsEnvoye;
    private LocalDateTime lastNotification;
    private String nomAffecteur;
    private String emailAffecteur;
    private LocalDateTime createdAt;
    

    private long joursRetard;
    private long joursRestants;
    private boolean estEnRetard;
    private boolean estActif;
    private LocalDateTime dateLimite;
}
