package com.sicmagroup.gpr.domain.dto;

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
public class HistoriqueTransmissionDto {
    private Long id;
    private Long claimId;
    private String codePlainte;
    private ClaimType typePlainte;
    private Long transmisParId;
    private String transmisParNom;
    private Long transmisAId;
    private String transmisANom;
    private String commentaire;
    private LocalDateTime dateTransmission;
}
