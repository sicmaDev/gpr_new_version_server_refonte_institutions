package com.sicmagroup.gpr.api.report;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sicmagroup.gpr.service.auth.AuthenticationServiceImpl;
import com.sicmagroup.gpr.service.report.BceaoReportService;
import com.sicmagroup.gpr.service.report.ReportServiceImpl;
import com.sicmagroup.gpr.service.report.StatsClaim;
import com.sicmagroup.gpr.service.report.StatsDenun;
import com.sicmagroup.gpr.service.report.StatsSuggest;
import com.sicmagroup.gpr.service.report.TemplateServiceImpl;
import com.sicmagroup.gpr.service.servicePoint.ServicePointServiceImpl;
import com.sicmagroup.gpr.utils.Utils;

import lombok.RequiredArgsConstructor;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.sicmagroup.gpr.domain.dto.ApiResponseDto;
import com.sicmagroup.gpr.domain.dto.ErrorResponse;
import com.sicmagroup.gpr.domain.dto.LicenceControl;
import com.sicmagroup.gpr.domain.dto.reports.BandChart.BandChart;
import com.sicmagroup.gpr.domain.dto.reports.StackedBar.StackedBar;
import com.sicmagroup.gpr.domain.dto.reports.pieChart.PieChartDto;
import com.sicmagroup.gpr.domain.enumeration.ClaimType;
import com.sicmagroup.gpr.domain.enumeration.Role;
import com.sicmagroup.gpr.domain.model.ServicePoint;
import com.sicmagroup.gpr.domain.model.Template;
import com.sicmagroup.gpr.domain.model.User;

@RestController
@RequestMapping("/api/v1/report")
@RequiredArgsConstructor
public class ReportController {
	private final ReportServiceImpl service;
	private final StatsClaim serviceStatsClaim;
	private final StatsDenun serviceStatsDenun;
	private final StatsSuggest serviceStatsSuggest;
	private final BceaoReportService bceaoReportService;
	private final TemplateServiceImpl templateService;
	private final AuthenticationServiceImpl authService;
	private final ServicePointServiceImpl spServiceImpl;

	// Ni RA ni élevé (H6/PILOTE/DE) ne doit voir, sur le Rapport Global (filtré ou
	// non), que son point de service et TOUS ceux qui lui sont rattachés — fils,
	// petits-fils, etc. (parcours récursif, comme pour l'écran Mesure de
	// satisfaction). Ça couvre aussi bien un RA qu'un simple titulaire de H11 qui
	// n'est ni RA ni PILOTE/DE/H6 — le frontend traite déjà ces deux cas de la
	// même façon (isElevatedUser = H6 || PILOTE || DE, indépendant de isRa()).
	private boolean isElevatedForReport(User user) {
		boolean hasH6 = user.getPoste() != null && user.getPoste().getHabilitations() != null
				&& Arrays.stream(user.getPoste().getHabilitations().split(","))
						.map(String::trim)
						.anyMatch(h -> h.equals("H6"));
		Role role = user.getAdditionalrole();
		return hasH6 || role == Role.PILOTE || role == Role.DE;
	}

	private User getConnectedUserOrNull() {
		try {
			UserDetails collectorDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication()
					.getPrincipal();
			return authService.getByEmail(collectorDetails.getUsername());
		} catch (Exception e) {
			return null;
		}
	}

	// Un agent simple (H11, ni RA ni élevé) ne voit que sa PROPRE agence — les
	// agences rattachées ne sont ajoutées que pour un RA. C'est la seule
	// différence entre les deux profils non-élevés.
	private List<Long> computeScopedServicePointIds(User connectedUser) {
		ServicePoint servicePoint = connectedUser.getServicePoint();
		List<Long> ids = new ArrayList<>();
		ids.add(servicePoint.getId());
		if (connectedUser.isRa()) {
			for (ServicePoint descendant : spServiceImpl.getAllDescendants(servicePoint.getId())) {
				ids.add(descendant.getId());
			}
		}
		return ids;
	}

	// Utilisé par /global : construit un FilterRequest ne portant que le
	// périmètre d'agences de l'utilisateur connecté, sauf s'il est élevé
	// (H6/PILOTE/DE), auquel cas on retourne null pour préserver l'accès complet.
	private FilterRequest buildRaScopedFilterOrNull() {
		User connectedUser = getConnectedUserOrNull();
		if (connectedUser == null || isElevatedForReport(connectedUser)) {
			return null;
		}
		// Les autres listes sont initialisées vides (pas null) pour matcher
		// exactement ce que le frontend envoie sur /filtered (buildBackendFiltres) :
		// plusieurs méthodes de ReportServiceImpl distinguent "null" de "liste vide"
		// (ex: le comptage des suggestions dans repartitionClaimDenunSuggest), donc
		// laisser ces champs à null changerait leur comportement par rapport à un
		// filtre "normal" qui ne restreint que par point de service.
		return FilterRequest.builder()
				.servicePoints(computeScopedServicePointIds(connectedUser))
				.objets(new ArrayList<>())
				.etats(new ArrayList<>())
				.products(new ArrayList<>())
				.savedBy(new ArrayList<>())
				.canals(new ArrayList<>())
				.build();
	}

	// Utilisé par /filtered : écrase le filtre "points de service" envoyé par le
	// client dès que l'utilisateur n'est pas élevé (H6/PILOTE/DE), pour ne jamais
	// faire confiance à ce que le frontend a envoyé (que ce soit un RA ou un
	// simple titulaire de H11, il ne doit pas pouvoir élargir son périmètre en
	// modifiant la requête).
	private void enforceRaScope(FilterRequest request) {
		User connectedUser = getConnectedUserOrNull();
		if (connectedUser != null && !isElevatedForReport(connectedUser)) {
			request.setServicePoints(computeScopedServicePointIds(connectedUser));
		}
	}

	@GetMapping(value = "/global")
	public ResponseEntity<Object> generatReport() {
		ApiResponseDto apiResponseDto = Utils.verifyLicence();
		;
		if (apiResponseDto.isStatus() && apiResponseDto.getContent().getClass() == LicenceControl.class) {
			LicenceControl lc = (LicenceControl) apiResponseDto.getContent();
			if (lc.isActif()) {
				// Un RA ne doit voir, même sur le rapport "non filtré" généré
				// automatiquement à l'ouverture de l'écran, que son point de service
				// et tous ceux qui lui sont rattachés (fils, petits-fils, ...).
				// Les autres rôles (PILOTE, DE, H5, ...) gardent l'accès complet.
				FilterRequest request = buildRaScopedFilterOrNull();

				GlobalReport globalReport = GlobalReport
						.builder()
						.repartitionClaimDenunSuggest(service.repartitionClaimDenunSuggest(request))
						.repartitionClaimDenuSuggestPerCanal(service.repartitionObjectByCanal(request))
						.nbreObjectPerCanalAndAgence(service.numberObjectByCanalByAgence(request))
						.nbreObjetPerObjetAndAgence(service.numberObjByObjetByAgence(request))
						.repartitionObjectByObj(service.repartitionObjPerObjet(request))
						.evolutionClaimDenunSuggestByYear(service.evolutionClaimDenunSuggestbyYear(request))
						.tauxResolution(service.tauxResolutionObj(request))
						.evolutionObjByYearAndAgence(service.evolutionObjBySpAndYear(request))
						.build();

				ClaimReport claimReport = ClaimReport
						.builder()
						.nbreClaimPerAgence(service.numberClaimPerServicePoint(request))
						.repartitionClaimPerAgence(service.repartitionClaimPerServicePoint(request))
						.basicStats(service.statusTotals(ClaimType.CLAIM, request))
						.repartitionClaimPerCanal(service.repartitionClaimByCanal(request))
						.nbreClaimPerCanalPerAgence(service.numberClaimByCanalByAgence(request))
						.repartitionClaimPerObjet(service.repartitionClaimPerObjet(request))
						.nbreClaimPerObjPerAgence(service.numberClaimPerObjetByAgence(request))
						.repartitionClaimPerGender(service.repartitionClaimByGender(request))
						.nbreClaimPerGenderPerAgence(service.numberClaimByGenderByAgence(request))
						.repartitionClaimPerObjRisque(service.repartitionClaimByGravityLevel(request))
						.nbreClaimPerObjLevelAndAgence(service.numberClaimByGravityByAngence(request))
						.repartitionClaimBySatisfaction(service.repartitionClaimBySatisfaction(request))
						.nbreClaimTreatInDelaiOrNot(service.numberClaimTreatInDelaiOrNot(request))
						.tauxClaimSatisfactionByMonth(service.tauxClaimSatisfactionByMonth(request))
						.tauxClaimSatisfactionByMonthByAgence(service.tauxClaimSatisfactionByMonthByAgence(request))
						.nbreClaimTreatInDelaiOrNotByMonth(service.numberClaimTreatInDelaiOrNotByMonth(request))
						.nbreClaimTreatInDelaiOrNotByMonthByAgence(service.numberClaimTreatInDelaiOrNotByMonthByAgence(request))
						.nbreDenunTreatInDelaiOrNotByMonth(service.numberDenunTreatInDelaiOrNotByMonth(request))
						.nbreDenunTreatInDelaiOrNotByMonthByAgence(service.numberDenunTreatInDelaiOrNotByMonthByAgence(request))
						.evolutionSatisfactionByThisYear(service.evolutionSatisfactionByYear(request))
						.tauxResolution(service.tauxResolutionClaim(request))
						.evolutionByAgenceAndYear(service.evolutionClaimBySpAndYear(request))
						.build();

				DenunReport denunReport = DenunReport
						.builder()
						.nbreDenunPerAgence(service.numberDenunPerServicePoint(request))
						.repartitionDenunPerAgence(service.repartitionDenunPerServicePoint(request))
						.basicStats(service.statusTotals(ClaimType.DENUNCIACION, request))
						.repartitionDenunPerCanal(service.repartitionDenunByCanal(request))
						.nbreDenunPerCanalPerAgence(service.numberDenunByCanalByAgence(request))
						.repartitionDenunPerObjet(service.repartitionDenunPerObjet(request))
						.nbreDenunPerObjPerAgence(service.numberDenunPerObjetByAgence(request))
						.repartitionDenunPerGender(service.repartitionDenunByGender(request))
						.repartitionDenunPerObjRisque(service.repartitionDenunByGravityLevel(request))
						.nbreDenunPerObjLevelAndAgence(service.numberDenunByGravityByAgence(request))
						.tauxResolution(service.tauxResolutionDenun(request))
						.evolutionByAgenceAndYear(service.evolutionDenunBySpAndYear(request))
						.build();

				SuggestionReport suggestionReport = SuggestionReport
						.builder()
						.nbreSuggestPerAgence(service.numberSuggestPerServicePoint(request))
						.repartitionSuggestPerAgence(service.repartitionSuggestPerServicePoint(request))
						.basicStats(service.statusTotals(ClaimType.SUGGESTION, request))
						.repartitionSuggestPerCanal(service.repartitionSuggestByCanal(request))
						.nbreSuggestPerCanalPerAgence(service.numberSuggestByCanalByAgence(request))
						.repartitionSuggestPerGender(service.repartitionSuggestByGender(request))
						.nbreSuggestPerGenderPerAgence(service.numberSuggestByGenderAgence(request))
						.tauxResolution(service.tauxResolutionSuggest(request))
						.evolutionSuggestByYearAndAgence(service.evolutionSuggestBySpAndYear(request))
						.build();

				ReportGlobalResponse reportGlobalResponse = ReportGlobalResponse
						.builder()
						.global(globalReport)
						.claimReport(claimReport)
						.denunReport(denunReport)
						.suggestionReport(suggestionReport)
						.newVersionStat(generateNewVersionReport(request))
						.build();

				StatisticReport statisticReport = StatisticReport
						.builder()
						.ClaimStatsAndValue(Arrays.asList(serviceStatsClaim.totalSavedClaim(request),
								serviceStatsClaim.totalByGravity(request), serviceStatsClaim.tauxSatisfaction(request),
								serviceStatsClaim.totalReclamantSatisfait(request),
								serviceStatsClaim.totalTreat(request), serviceStatsClaim.totalTreatByGravity(request),
								serviceStatsClaim.totalTreatByRespectTiming(request), serviceStatsClaim.totalUnTreat(request),
								serviceStatsClaim.totalUnTreatByGravity(request),
								serviceStatsClaim.pourcentageReclamationsTraitees(request),
								serviceStatsClaim.pourcentageReelReclamationsTraitees(request),
								serviceStatsClaim.pourcentageReclamationsTraiteesDansDelai(request)
								))
						.DenunStatsAndValue(Arrays.asList(serviceStatsDenun.totalSavedClaim(request),
								serviceStatsDenun.totalByGravity(request),
								serviceStatsDenun.totalTreat(request),
								serviceStatsDenun.totalTreatByGravity(request),
								serviceStatsDenun.totalTreatByRespectTiming(request),
								serviceStatsDenun.totalUnTreat(request), serviceStatsDenun.totalUnTreatByGravity(request)))
						.SuggestStatsAndValue(
								Arrays.asList(serviceStatsSuggest.totalSaved(request),
										serviceStatsSuggest.totalSuggest(request)))
						.build();
				reportGlobalResponse.setStatistic(statisticReport);

				apiResponseDto = ApiResponseDto
						.builder()
						.status(true)
						.content(reportGlobalResponse)
						.build();

				// return ResponseEntity.ok(reportGlobalResponse);
				return ResponseEntity.ok(apiResponseDto);
			} else {
				apiResponseDto = ApiResponseDto
						.builder()
						.status(false)
						.content(lc)
						.build();

				return ResponseEntity.ok(apiResponseDto);
			}

		} else {

			return ResponseEntity.ok(apiResponseDto);
		}
	}

	@PostMapping(value = "/filtered")
	public ResponseEntity<Object> generateFilterReport(@RequestBody FilterRequest request) {
		// System.out.println("Status choosed");
		// System.out.println(request);
		ApiResponseDto apiResponseDto = Utils.verifyLicence();
		
		if (apiResponseDto.isStatus() && apiResponseDto.getContent().getClass() == LicenceControl.class) {
			LicenceControl lc = (LicenceControl) apiResponseDto.getContent();
			if (lc.isActif()) {
				// Un RA ne peut pas élargir son périmètre en modifiant le filtre envoyé
				// depuis le frontend : on écrase servicePoints avec son périmètre réel.
				enforceRaScope(request);

				GlobalReport globalReport = GlobalReport
						.builder()
						.repartitionClaimDenunSuggest(service.repartitionClaimDenunSuggest(request))
						.repartitionClaimDenuSuggestPerCanal(service.repartitionObjectByCanal(request))
						.nbreObjectPerCanalAndAgence(service.numberObjectByCanalByAgence(request))
						.nbreObjetPerObjetAndAgence(service.numberObjByObjetByAgence(request))
						.repartitionObjectByObj(service.repartitionObjPerObjet(request))
						.evolutionClaimDenunSuggestByYear(service.evolutionClaimDenunSuggestbyYear(request))
						.tauxResolution(service.tauxResolutionObj(request))
						.evolutionObjByYearAndAgence(service.evolutionObjBySpAndYear(request))
						.build();

				ClaimReport claimReport = ClaimReport
						.builder()
						.nbreClaimPerAgence(service.numberClaimPerServicePoint(request))
						.repartitionClaimPerAgence(service.repartitionClaimPerServicePoint(request))
						.basicStats(service.statusTotals(ClaimType.CLAIM, request))
						.repartitionClaimPerCanal(service.repartitionClaimByCanal(request))
						.nbreClaimPerCanalPerAgence(service.numberClaimByCanalByAgence(request))
						.repartitionClaimPerObjet(service.repartitionClaimPerObjet(request))
						.nbreClaimPerObjPerAgence(service.numberClaimPerObjetByAgence(request))
						.repartitionClaimPerGender(service.repartitionClaimByGender(request))
						.nbreClaimPerGenderPerAgence(service.numberClaimByGenderByAgence(request))
						.repartitionClaimPerObjRisque(service.repartitionClaimByGravityLevel(request))
						.nbreClaimPerObjLevelAndAgence(service.numberClaimByGravityByAngence(request))
						.repartitionClaimBySatisfaction(service.repartitionClaimBySatisfaction(request))
						.nbreClaimTreatInDelaiOrNot(service.numberClaimTreatInDelaiOrNot(request))
						.tauxClaimSatisfactionByMonth(service.tauxClaimSatisfactionByMonth(request))
						.tauxClaimSatisfactionByMonthByAgence(service.tauxClaimSatisfactionByMonthByAgence(request))
						.nbreClaimTreatInDelaiOrNotByMonth(service.numberClaimTreatInDelaiOrNotByMonth(request))
						.nbreClaimTreatInDelaiOrNotByMonthByAgence(service.numberClaimTreatInDelaiOrNotByMonthByAgence(request))
						.nbreDenunTreatInDelaiOrNotByMonth(service.numberDenunTreatInDelaiOrNotByMonth(request))
						.nbreDenunTreatInDelaiOrNotByMonthByAgence(service.numberDenunTreatInDelaiOrNotByMonthByAgence(request))
						.evolutionSatisfactionByThisYear(service.evolutionSatisfactionByYear(request))
						.tauxResolution(service.tauxResolutionClaim(request))
						.evolutionByAgenceAndYear(service.evolutionClaimBySpAndYear(request))
						.build();

				DenunReport denunReport = DenunReport
						.builder()
						.nbreDenunPerAgence(service.numberDenunPerServicePoint(request))
						.repartitionDenunPerAgence(service.repartitionDenunPerServicePoint(request))
						.basicStats(service.statusTotals(ClaimType.DENUNCIACION, request))
						.repartitionDenunPerCanal(service.repartitionDenunByCanal(request))
						.nbreDenunPerCanalPerAgence(service.numberDenunByCanalByAgence(request))
						.repartitionDenunPerObjet(service.repartitionDenunPerObjet(request))
						.nbreDenunPerObjPerAgence(service.numberDenunPerObjetByAgence(request))
						// .repartitionDenunPerGender(service.repartitionDenunByGender(null))
						.repartitionDenunPerObjRisque(service.repartitionDenunByGravityLevel(request))
						.nbreDenunPerObjLevelAndAgence(service.numberDenunByGravityByAgence(request))
						.tauxResolution(service.tauxResolutionDenun(request))
						.evolutionByAgenceAndYear(service.evolutionDenunBySpAndYear(request))
						.build();
				ReportGlobalResponse reportGlobalResponse = ReportGlobalResponse
						.builder()
						.global(globalReport)
						.claimReport(claimReport)
						.denunReport(denunReport)
						.newVersionStat(generateNewVersionReport(request))
						.build();

				if (request.getObjets() != null && request.getObjets().isEmpty()) {
					SuggestionReport suggestionReport = SuggestionReport
							.builder()
							.nbreSuggestPerAgence(service.numberSuggestPerServicePoint(request))
							.repartitionSuggestPerAgence(service.repartitionSuggestPerServicePoint(request))
							.basicStats(service.statusTotals(ClaimType.SUGGESTION, request))
							.repartitionSuggestPerCanal(service.repartitionSuggestByCanal(request))
							.nbreSuggestPerCanalPerAgence(service.numberSuggestByCanalByAgence(request))
							.repartitionSuggestPerGender(service.repartitionSuggestByGender(request))
							.nbreSuggestPerGenderPerAgence(service.numberSuggestByGenderAgence(request))
							.tauxResolution(service.tauxResolutionSuggest(request))
							.evolutionSuggestByYearAndAgence(service.evolutionSuggestBySpAndYear(request))
							.build();
					reportGlobalResponse.setSuggestionReport(suggestionReport);
				} else {
					BandChart bandChart = BandChart
							.builder()
							.backgroundColors(new ArrayList<>())
							.borderColors(new ArrayList<>())
							.borderWidth("1")
							.datas(new ArrayList<>())
							.labels(new ArrayList<>())
							.build();
					PieChartDto pieChartDto = PieChartDto
							.builder()
							.backgroundColors(new ArrayList<>())
							.labels(new ArrayList<>())
							.datas(new ArrayList<>())
							.ids(new ArrayList<>())
							.build();
					StackedBar stackedBar = StackedBar
							.builder()
							.ids(new ArrayList<>())
							.labels(new ArrayList<>())
							.datasets(new ArrayList<>())
							.build();
					SuggestionReport suggestionReport = SuggestionReport
							.builder()
							.nbreSuggestPerAgence(bandChart)
							.repartitionSuggestPerAgence(pieChartDto)
							.basicStats(service.statusTotals(ClaimType.SUGGESTION, request))
							.repartitionSuggestPerCanal(pieChartDto)
							.nbreSuggestPerCanalPerAgence(stackedBar)
							.repartitionSuggestPerGender(pieChartDto)
							.nbreSuggestPerGenderPerAgence(stackedBar)
							.tauxResolution((double) 0)
							.evolutionSuggestByYearAndAgence(stackedBar)
							.build();
					reportGlobalResponse.setSuggestionReport(suggestionReport);
				}

				StatisticReport statisticReport = StatisticReport
						.builder()
						.ClaimStatsAndValue(Arrays.asList(serviceStatsClaim.totalSavedClaim(request),
								serviceStatsClaim.totalByGravity(request), serviceStatsClaim.tauxSatisfaction(request),
								serviceStatsClaim.totalReclamantSatisfait(request),
								serviceStatsClaim.totalTreat(request), serviceStatsClaim.totalTreatByGravity(request),
								serviceStatsClaim.totalTreatByRespectTiming(request),
								serviceStatsClaim.totalUnTreat(request),
								serviceStatsClaim.totalUnTreatByGravity(request),
								serviceStatsClaim.pourcentageReclamationsTraitees(request),
								serviceStatsClaim.pourcentageReelReclamationsTraitees(request),
								serviceStatsClaim.pourcentageReclamationsTraiteesDansDelai(request) 
								))
						.DenunStatsAndValue(Arrays.asList(serviceStatsDenun.totalByGravity(request),
								serviceStatsDenun.totalSavedClaim(request), serviceStatsDenun.totalTreat(request),
								serviceStatsDenun.totalTreatByGravity(request),
								serviceStatsDenun.totalTreatByRespectTiming(request),
								serviceStatsDenun.totalUnTreat(request),
								serviceStatsDenun.totalUnTreatByGravity(request)))
						.SuggestStatsAndValue(Arrays.asList(serviceStatsSuggest.totalSaved(request),
								serviceStatsSuggest.totalSuggest(request)))
						.build();
				reportGlobalResponse.setStatistic(statisticReport);

				return ResponseEntity.ok(reportGlobalResponse);
			} else {
				apiResponseDto = ApiResponseDto
						.builder()
						.status(false)
						.content(lc)
						.build();

				return ResponseEntity.ok(apiResponseDto);
			}

		} else {

			return ResponseEntity.ok(apiResponseDto);
		}
	}

	@PostMapping(value = "/bceao")
	public ResponseEntity<Object> generateBceaoReport(@RequestBody BceaoRequest BceaoRequest) {
		ApiResponseDto apiResponseDto = Utils.verifyLicence();
		;
		if (apiResponseDto.isStatus() && apiResponseDto.getContent().getClass() == LicenceControl.class) {
			LicenceControl lc = (LicenceControl) apiResponseDto.getContent();
			if (lc.isActif()) {
				return ResponseEntity.ok(bceaoReportService.generateBceaoReport(BceaoRequest));
			} else {
				apiResponseDto = ApiResponseDto
						.builder()
						.status(false)
						.content(lc)
						.build();

				return ResponseEntity.ok(apiResponseDto);
			}

		} else {

			return ResponseEntity.ok(apiResponseDto);
		}
	}


	@PostMapping(value = "/global/new")
	public HashMap<String,Object> generateNewVersionReport(@RequestBody FilterRequest request) {
		// Endpoint atteignable directement par le client (pas seulement via
		// generatReport()/generateFilterReport() qui l'appellent aussi en interne) :
		// on applique le même verrou de périmètre RA ici.
		enforceRaScope(request);

		HashMap<String,Object> result = new HashMap<>();
		HashMap<String,Object> general = new HashMap<>();



		general.put("RSDObjet", service.listRDSPerAgencePerObjet(request));
		general.put("RSDModalite", service.listRDSPerAgencePerModalite(request));

		result.put("GeneralPerAgence", general);
		result.put("AgencePerObjet", service.listPerAgencePerObjet(request));
		result.put("AgencePerModalite", service.listPerAgencePerModalite(request));
		result.put("AgencePerMesure", service.listPerAgencePerMesure(request));
		result.put("AgencePerGenre", service.listPerAgencePerGenre(request));
		result.put("AgencePerGravity", service.listPerAgencePerGravity(request));

		return result;
		
	}
	


	
	@GetMapping(value = "/dashboard")
	public HashMap<String,Object> getDashboardResume() {
		HashMap<String,Object> result =  service.getDashboardResume();

		return result;
		
	}

	
	@GetMapping(value = "/templates")
	public ResponseEntity<ApiResponseDto>getAllTemplate() {
		try {
            List<Template> result = templateService.findAll();
            ApiResponseDto apiResponseDto = ApiResponseDto
                    .builder()
                    .status(true)
                    .content(result)
                    .build();
            return ResponseEntity.ok(apiResponseDto);
        } catch (Exception e) {
            ApiResponseDto apiResponseDto = ApiResponseDto
                    .builder()
                    .status(false)
                    .content(ErrorResponse.builder().message(e.getMessage()).title("EXCEPTION TGHROW").build())
                    .build();
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponseDto);
            }
        }
	}

	@PostMapping(value = "/templates/update")
	public ResponseEntity<ApiResponseDto> createOrUpdateTemplate(@RequestBody Template request) {
		try {
			Template result = templateService.createOrUpdate(request);
			System.out.println("TemplateRequest" + result);
			ApiResponseDto apiResponseDto = ApiResponseDto
					.builder()
					.status(true)
					.content(result)
					.build();
			return ResponseEntity.ok(apiResponseDto);

		} catch (Exception e) {
			ApiResponseDto apiResponseDto = ApiResponseDto
					.builder()
					.status(false)
					.content(ErrorResponse.builder()
							.message(e.getMessage())
							.title("EXCEPTION TGHROW")
							.build())
					.build();

			if (e.getMessage().contains("not found")) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
			} else {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponseDto);
			}
		}
	}
	@GetMapping(value = "/system-performance")
	public ResponseEntity<HashMap<String, Object>> getSystemPerformance() {
		HashMap<String, Object> result = new HashMap<>();
		try {
			com.sun.management.OperatingSystemMXBean osBean =
				(com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

			double cpuLoad = osBean.getSystemCpuLoad();
			int cpu = (cpuLoad < 0) ? 0 : (int) Math.round(cpuLoad * 100);

			long totalRam = osBean.getTotalPhysicalMemorySize();
			long freeRam  = osBean.getFreePhysicalMemorySize();
			long usedRam  = totalRam - freeRam;
			double ramTotalGo = Math.round(totalRam / 1e9 * 10.0) / 10.0;
			double ramUsedGo  = Math.round(usedRam  / 1e9 * 10.0) / 10.0;

			long totalDisk = 0, usableDisk = 0;
			for (File root : File.listRoots()) {
				totalDisk  += root.getTotalSpace();
				usableDisk += root.getUsableSpace();
			}
			long usedDisk = totalDisk - usableDisk;
			double diskTotalGo = Math.round(totalDisk / 1e9 * 10.0) / 10.0;
			double diskUsedGo  = Math.round(usedDisk  / 1e9 * 10.0) / 10.0;

			result.put("cpu",        cpu);
			result.put("ramUsedGo",  ramUsedGo);
			result.put("ramTotalGo", ramTotalGo);
			result.put("diskUsedGo", diskUsedGo);
			result.put("diskTotalGo",diskTotalGo);
		} catch (Exception e) {
			result.put("cpu",        0);
			result.put("ramUsedGo",  0);
			result.put("ramTotalGo", 0);
			result.put("diskUsedGo", 0);
			result.put("diskTotalGo",0);
		}
		return ResponseEntity.ok(result);
	}

	@DeleteMapping(value="/templates/{id}")

	public ResponseEntity<ApiResponseDto> deleteTemplate(@PathVariable(name = "id") Long id) {
		ApiResponseDto apiResponseDto;
		try {
			templateService.delete(id);
			apiResponseDto = ApiResponseDto
					.builder()
					.status(true)
					.content("Suppression réussie")
					.build();
			return ResponseEntity.ok(apiResponseDto);
		} catch (Exception e) {
			e.printStackTrace();
			apiResponseDto = ApiResponseDto
					.builder()
					.status(false)
					.content(ErrorResponse.builder()
							.title("NOT FOUND")
							.message(e.getMessage())
							.build())
					.build();
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
		}
	}
}
