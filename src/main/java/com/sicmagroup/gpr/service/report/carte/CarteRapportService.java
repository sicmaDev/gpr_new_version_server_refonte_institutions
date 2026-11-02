package com.sicmagroup.gpr.service.report.carte;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.sicmagroup.gpr.api.report.FilterRequest;
import com.sicmagroup.gpr.domain.dto.reports.StackedBar.StackedBar;
import com.sicmagroup.gpr.domain.model.ServicePointLocalisation;
import com.sicmagroup.gpr.repository.ServicePointLocalisationRepository;
import com.sicmagroup.gpr.service.report.PerimetreRapport;
import com.sicmagroup.gpr.service.report.ReportServiceImpl;
import com.sicmagroup.gpr.service.report.StatsClaim;
import com.sicmagroup.gpr.service.report.StatsDenun;
import com.sicmagroup.gpr.service.report.StatsSuggest;

import lombok.RequiredArgsConstructor;

// Chiffres de la carte du rapport Global, agence par agence.
// Aucun calcul nouveau : chaque chiffre vient des méthodes déjà utilisées par le rapport (tableaux récapitulatifs
// et graphique de satisfaction par agence), appelées avec le filtre de la page restreint à une agence.
// Même périmètre que le rapport : un RA ne reçoit que son agence et celles qui lui sont rattachées.
@Service
@RequiredArgsConstructor
public class CarteRapportService {
	private final StatsClaim statsClaim;
	private final StatsDenun statsDenun;
	private final StatsSuggest statsSuggest;
	private final ReportServiceImpl reportService;
	private final ServicePointLocalisationRepository localisationRepository;
	private final PerimetreRapport perimetre;

	public List<CarteAgence> agences(FilterRequest filtrePage) {
		FilterRequest filtre = filtrePage != null ? copie(filtrePage, filtrePage.getServicePoints()) : filtreVide();
		// Périmètre : écrasé pour un non-élevé (comme enforceRaScope) ; sinon les agences choisies dans le filtre, ou toutes.
		List<Long> imposees = perimetre.agencesImposeesOuNull();
		if (imposees != null) {
			filtre.setServicePoints(imposees);
		}
		List<Long> choisies = filtre.getServicePoints();
		Map<Long, ServicePointLocalisation> localisations = localisationRepository.findAll().stream()
				.collect(Collectors.toMap(ServicePointLocalisation::getId, Function.identity()));
		List<Long> ids = (choisies != null && !choisies.isEmpty()) ? choisies
				: localisations.values().stream().map(ServicePointLocalisation::getId).collect(Collectors.toList());

		// Taux de satisfaction par agence : une seule fois, avec le filtre complet (périmètre compris).
		Map<Long, Double> satisfaction = satisfactionParAgence(copie(filtre, filtre.getServicePoints()));
		// Suggestions : vides dès qu'un objet est filtré, exactement comme le rapport.
		boolean avecSuggestions = filtre.getObjets() == null || filtre.getObjets().isEmpty();

		List<CarteAgence> resultat = new ArrayList<>();
		for (Long id : ids) {
			ServicePointLocalisation loc = localisations.get(id);
			if (loc == null) continue;
			long rec = valeur(statsClaim.totalSavedClaim(pour(filtre, id)), "Nombre de réclamations Enregistrées");
			long den = valeur(statsDenun.totalSavedClaim(pour(filtre, id)), "Nombre de dénonciations Enregistrées");
			long sug = avecSuggestions ? valeur(statsSuggest.totalSaved(pour(filtre, id)), "Nombre de suggestions Enregistrées") : 0;
			boolean supprimee = loc.isDeleted();
			if (supprimee && rec + den + sug == 0) continue; // agence supprimée sans aucune plainte : rien à montrer
			long recT = valeur(statsClaim.totalTreat(pour(filtre, id)), "Nombre de réclamations traitées");
			long recNT = valeur(statsClaim.totalUnTreat(pour(filtre, id)), "Nombre de réclamations non traitées");
			long recDelai = valeur(statsClaim.totalTreatByRespectTiming(pour(filtre, id)), "Nombre de réclamations traitées dans le délai");
			long denT = valeur(statsDenun.totalTreat(pour(filtre, id)), "Nombre de dénonciations traitées");
			long denNT = valeur(statsDenun.totalUnTreat(pour(filtre, id)), "Nombre de dénonciations non traitées");
			long denDelai = valeur(statsDenun.totalTreatByRespectTiming(pour(filtre, id)), "Nombre de dénonciations traitées dans le délai");
			long traitees = recT + denT;
			Double horsDelai = traitees > 0 ? arrondi(100.0 * (traitees - recDelai - denDelai) / traitees) : null;
			resultat.add(CarteAgence.builder()
					.id(id).libelle(loc.getLibelle()).ville(loc.getVille()).quartier(loc.getQuartier())
					.latitude(loc.getLatitude()).longitude(loc.getLongitude())
					.reclamations(rec).reclamationsTraitees(recT).reclamationsNonTraitees(recNT)
					.denonciations(den).denonciationsTraitees(denT).denonciationsNonTraitees(denNT)
					.suggestions(sug).horsDelaiPct(horsDelai).satisfactionPct(satisfaction.get(id))
					.build());
		}
		return resultat;
	}

	private Map<Long, Double> satisfactionParAgence(FilterRequest filtre) {
		Map<Long, Double> taux = new HashMap<>();
		StackedBar bar = reportService.tauxClaimSatisfactionByMonthByAgence(filtre);
		if (bar == null || bar.getDatasets() == null || bar.getDatasets().isEmpty()) return taux;
		List<Double> data = bar.getDatasets().get(0).getData();
		for (int i = 0; i < bar.getIds().size() && i < data.size(); i++) {
			taux.put(bar.getIds().get(i), data.get(i));
		}
		return taux;
	}

	// Le filtre de la page restreint à une agence (listes absentes remplacées par des listes vides).
	private FilterRequest pour(FilterRequest filtre, Long agence) {
		return copie(filtre, List.of(agence));
	}

	private static FilterRequest copie(FilterRequest f, List<Long> agences) {
		return FilterRequest.builder()
				.year(f.getYear())
				.receiveStart(f.getReceiveStart())
				.receiveEnd(f.getReceiveEnd())
				.servicePoints(agences != null ? new ArrayList<>(agences) : new ArrayList<>())
				.objets(f.getObjets() != null ? new ArrayList<>(f.getObjets()) : new ArrayList<>())
				.etats(f.getEtats() != null ? new ArrayList<>(f.getEtats()) : new ArrayList<>())
				.products(f.getProducts() != null ? new ArrayList<>(f.getProducts()) : new ArrayList<>())
				.savedBy(f.getSavedBy() != null ? new ArrayList<>(f.getSavedBy()) : new ArrayList<>())
				.canals(f.getCanals() != null ? new ArrayList<>(f.getCanals()) : new ArrayList<>())
				.build();
	}

	// Page ouverte sans filtre (/report/global) : listes vides, comme buildRaScopedFilterOrNull.
	private static FilterRequest filtreVide() {
		return copie(new FilterRequest(), new ArrayList<>());
	}

	private static long valeur(Map<String, Double> resultat, String cle) {
		Double v = resultat != null ? resultat.get(cle) : null;
		return v == null ? 0 : Math.round(v);
	}

	private static double arrondi(double v) {
		return Math.round(v * 10.0) / 10.0;
	}
}
