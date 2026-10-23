package com.sicmagroup.gpr.domain.dto;

import com.sicmagroup.gpr.domain.enumeration.ServicePointEnum;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO minimal pour une route PUBLIQUE (formulaire d'inscription anonyme) :
// uniquement ce qu'il faut pour construire le sélecteur "Point de service"
// (regroupé par type Direction/Agence/Guichet côté frontend).
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicServicePointDto {
    private Long id;
    private String libelle;
    private ServicePointEnum type;
}
