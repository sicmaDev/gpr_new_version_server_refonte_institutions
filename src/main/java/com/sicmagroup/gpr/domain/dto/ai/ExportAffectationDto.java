package com.sicmagroup.gpr.domain.dto.ai;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExportAffectationDto {
    private Long id;
    private Long reclamationId;
    private Long suggestionId;
    private String codePlainte;
    private String typePlainte;
    private String nomAgent;
    private String emailAgent;
    private String telephoneAgent;
    private Long affecteurId;
    private String nomAffecteur;
    private String emailAffecteur;
    private LocalDateTime dateAffectation;
    private Integer delaiJours;
    private LocalDateTime dateFinAffectation;
    private Long affectationPrecedenteId;
    private LocalDateTime lastNotification;
    private Boolean mailEnvoye;
    private Boolean smsEnvoye;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
