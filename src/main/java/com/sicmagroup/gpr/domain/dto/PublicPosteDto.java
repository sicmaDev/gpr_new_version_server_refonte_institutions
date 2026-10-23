package com.sicmagroup.gpr.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO volontairement minimal, destiné à une route PUBLIQUE (formulaire
// d'inscription anonyme) : ne jamais y ajouter "habilitations" (PosteDto le
// porte) — ça révélerait publiquement quel poste détient quel niveau de droit
// (ex: lequel a l'habilitation H12, admin).
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicPosteDto {
    private Long id;
    private String libelle;
}
