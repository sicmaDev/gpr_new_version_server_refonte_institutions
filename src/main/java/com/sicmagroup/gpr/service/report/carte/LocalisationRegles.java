package com.sicmagroup.gpr.service.report.carte;

import com.sicmagroup.gpr.domain.model.ServicePointLocalisation;

// Règles de saisie de la localisation d'un point de service (les mêmes que le formulaire de l'interface).
public final class LocalisationRegles {
	private LocalisationRegles() {
	}

	// Latitude et longitude vont ensemble, dans leurs plages ; textes limités à 120 caractères. null = valide.
	public static String verifier(ServicePointLocalisation d) {
		if ((d.getLatitude() == null) != (d.getLongitude() == null)) {
			return "Renseignez la latitude et la longitude ensemble, ou laissez les deux vides.";
		}
		if (d.getLatitude() != null && (d.getLatitude() < -90 || d.getLatitude() > 90)) {
			return "La latitude doit être comprise entre -90 et 90.";
		}
		if (d.getLongitude() != null && (d.getLongitude() < -180 || d.getLongitude() > 180)) {
			return "La longitude doit être comprise entre -180 et 180.";
		}
		if ((d.getVille() != null && d.getVille().trim().length() > 120) || (d.getQuartier() != null && d.getQuartier().trim().length() > 120)) {
			return "La ville et le quartier ne doivent pas dépasser 120 caractères.";
		}
		return null;
	}
}
