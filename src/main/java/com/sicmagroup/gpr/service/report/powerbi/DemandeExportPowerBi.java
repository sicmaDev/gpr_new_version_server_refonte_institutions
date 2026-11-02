package com.sicmagroup.gpr.service.report.powerbi;

import java.util.List;
import java.util.Map;

import com.sicmagroup.gpr.api.report.FilterRequest;

import lombok.Data;

// Ce que la page Rapport Global envoie pour l'export Power BI. Aucun chiffre : ils sont recalculés par le serveur
// avec le service du rapport (mêmes calculs, même périmètre). La page n'envoie que ce qui décide de l'affichage.
@Data
public class DemandeExportPowerBi {
	// Filtre du dernier rapport affiché ; null quand la page a été ouverte sans filtre (rapport /global).
	private FilterRequest filtre;
	// Onglets affichés (global, claim, denun, suggest) : dépendent du filtre « type de plainte ».
	private Map<String, Boolean> sections;
	// Modèle de rapport actif : { global: { globalPieChartRef: true, ... }, claim: {...}, ... }. Clé absente = affichée.
	private Map<String, Map<String, Boolean>> modele;
	// Couleurs affichées au moment de l'export, par graphique (clé du modèle) puis par libellé (catégorie ou série).
	private Map<String, Map<String, String>> couleurs;
	// Couleur principale du rapport (couleur de l'institution, déjà ajustée par la page).
	private String couleurPrincipale;
	// Filtres appliqués, tels que la page les affiche (libellé, valeur).
	private List<Map<String, String>> filtresAffiches;
	// Pays de l'institution (code ISO) : le fond des départements n'est dessiné que pour le Bénin.
	private String paysCode;
}
