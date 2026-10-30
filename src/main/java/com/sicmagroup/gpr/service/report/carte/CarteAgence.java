package com.sicmagroup.gpr.service.report.carte;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Une agence sur la carte du rapport Global : uniquement des chiffres agrégés, aucune donnée personnelle.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CarteAgence {
	private Long id;
	private String libelle;
	private String ville;
	private String quartier;
	private Double latitude;
	private Double longitude;
	private long reclamations;
	private long reclamationsTraitees;
	private long reclamationsNonTraitees;
	private long denonciations;
	private long denonciationsTraitees;
	private long denonciationsNonTraitees;
	private long suggestions;
	// Part des réclamations et dénonciations traitées hors délai (%), null s'il n'y a aucune traitée.
	private Double horsDelaiPct;
	// Taux de satisfaction des réclamations (%), le même que le graphique « Taux de satisfaction par agence ».
	private Double satisfactionPct;
}
