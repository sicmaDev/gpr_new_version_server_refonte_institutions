package com.sicmagroup.gpr.api.report;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sicmagroup.gpr.service.report.BceaoReportService;
import com.sicmagroup.gpr.service.report.ReportServiceImpl;
import com.sicmagroup.gpr.service.report.StatsClaim;
import com.sicmagroup.gpr.service.report.StatsDenun;
import com.sicmagroup.gpr.service.report.StatsSuggest;
import com.sicmagroup.gpr.utils.Utils;

import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Arrays;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.sicmagroup.gpr.domain.dto.ApiResponseDto;
import com.sicmagroup.gpr.domain.dto.LicenceControl;
import com.sicmagroup.gpr.domain.dto.reports.BandChart.BandChart;
import com.sicmagroup.gpr.domain.dto.reports.StackedBar.StackedBar;
import com.sicmagroup.gpr.domain.dto.reports.pieChart.PieChartDto;
import com.sicmagroup.gpr.domain.enumeration.ClaimType;

@RestController
@RequestMapping("/api/v1/report")
@RequiredArgsConstructor
public class ReportController {
	private final ReportServiceImpl service;
	private final StatsClaim serviceStatsClaim;
	private final StatsDenun serviceStatsDenun;
	private final StatsSuggest serviceStatsSuggest;
	private final BceaoReportService bceaoReportService;

	@GetMapping(value = "/global")
	public ResponseEntity<Object> generatReport() {
		ApiResponseDto apiResponseDto = Utils.verifyLicence();
		;
		if (apiResponseDto.isStatus() && apiResponseDto.getContent().getClass() == LicenceControl.class) {
			LicenceControl lc = (LicenceControl) apiResponseDto.getContent();
			if (lc.isActif()) {
				GlobalReport globalReport = GlobalReport
						.builder()
						.repartitionClaimDenunSuggest(service.repartitionClaimDenunSuggest(null))
						.repartitionClaimDenuSuggestPerCanal(service.repartitionObjectByCanal(null))
						.nbreObjectPerCanalAndAgence(service.numberObjectByCanalByAgence(null))
						.nbreObjetPerObjetAndAgence(service.numberObjByObjetByAgence(null))
						.repartitionObjectByObj(service.repartitionObjPerObjet(null))
						.evolutionClaimDenunSuggestByYear(service.evolutionClaimDenunSuggestbyYear(null))
						.tauxResolution(service.tauxResolutionObj(null))
						.evolutionObjByYearAndAgence(service.evolutionObjBySpAndYear(null))
						.build();

				ClaimReport claimReport = ClaimReport
						.builder()
						.nbreClaimPerAgence(service.numberClaimPerServicePoint(null))
						.repartitionClaimPerAgence(service.repartitionClaimPerServicePoint(null))
						.basicStats(service.statusTotals(ClaimType.CLAIM, null))
						.repartitionClaimPerCanal(service.repartitionClaimByCanal(null))
						.nbreClaimPerCanalPerAgence(service.numberClaimByCanalByAgence(null))
						.repartitionClaimPerObjet(service.repartitionClaimPerObjet(null))
						.nbreClaimPerObjPerAgence(service.numberClaimPerObjetByAgence(null))
						.repartitionClaimPerGender(service.repartitionClaimByGender(null))
						.nbreClaimPerGenderPerAgence(service.numberClaimByGenderByAgence(null))
						.repartitionClaimPerObjRisque(service.repartitionClaimByGravityLevel(null))
						.nbreClaimPerObjLevelAndAgence(service.numberClaimByGravityByAngence(null))
						.repartitionClaimBySatisfaction(service.repartitionClaimBySatisfaction(null))
						.nbreClaimTreatInDelaiOrNot(service.numberClaimTreatInDelaiOrNot(null))
						.evolutionSatisfactionByThisYear(service.evolutionSatisfactionByYear(null))
						.tauxResolution(service.tauxResolutionClaim(null))
						.evolutionByAgenceAndYear(service.evolutionClaimBySpAndYear(null))
						.build();

				DenunReport denunReport = DenunReport
						.builder()
						.nbreDenunPerAgence(service.numberDenunPerServicePoint(null))
						.repartitionDenunPerAgence(service.repartitionDenunPerServicePoint(null))
						.basicStats(service.statusTotals(ClaimType.DENUNCIACION, null))
						.repartitionDenunPerCanal(service.repartitionDenunByCanal(null))
						.nbreDenunPerCanalPerAgence(service.numberDenunByCanalByAgence(null))
						.repartitionDenunPerObjet(service.repartitionDenunPerObjet(null))
						.nbreDenunPerObjPerAgence(service.numberDenunPerObjetByAgence(null))
						.repartitionDenunPerGender(service.repartitionDenunByGender(null))
						.repartitionDenunPerObjRisque(service.repartitionDenunByGravityLevel(null))
						.nbreDenunPerObjLevelAndAgence(service.numberDenunByGravityByAgence(null))
						.tauxResolution(service.tauxResolutionDenun(null))
						.evolutionByAgenceAndYear(service.evolutionDenunBySpAndYear(null))
						.build();

				SuggestionReport suggestionReport = SuggestionReport
						.builder()
						.nbreSuggestPerAgence(service.numberSuggestPerServicePoint(null))
						.repartitionSuggestPerAgence(service.repartitionSuggestPerServicePoint(null))
						.basicStats(service.statusTotals(ClaimType.SUGGESTION, null))
						.repartitionSuggestPerCanal(service.repartitionSuggestByCanal(null))
						.nbreSuggestPerCanalPerAgence(service.numberSuggestByCanalByAgence(null))
						.repartitionSuggestPerGender(service.repartitionSuggestByGender(null))
						.nbreSuggestPerGenderPerAgence(service.numberSuggestByGenderAgence(null))
						.tauxResolution(service.tauxResolutionSuggest(null))
						.evolutionSuggestByYearAndAgence(service.evolutionSuggestBySpAndYear(null))
						.build();
				ReportGlobalResponse reportGlobalResponse = ReportGlobalResponse
						.builder()
						.global(globalReport)
						.claimReport(claimReport)
						.denunReport(denunReport)
						.suggestionReport(suggestionReport)

						.build();
				
				StatisticReport statisticReport = StatisticReport
						.builder()
						.ClaimStatsAndValue(Arrays.asList(serviceStatsClaim.totalSavedClaim(null),
								serviceStatsClaim.totalByGravity(null), serviceStatsClaim.tauxSatisfaction(null),
								serviceStatsClaim.totalReclamantSatisfait(null),
								serviceStatsClaim.totalSavedClaim(null),
								serviceStatsClaim.totalTreat(null), serviceStatsClaim.totalTreatByGravity(null),
								serviceStatsClaim.totalTreatByRespectTiming(null), serviceStatsClaim.totalUnTreat(null),
								serviceStatsClaim.totalUnTreatByGravity(null)))
						.DenunStatsAndValue(Arrays.asList(serviceStatsDenun.totalByGravity(null),
								serviceStatsDenun.totalSavedClaim(null), serviceStatsDenun.totalTreat(null),
								serviceStatsDenun.totalTreatByGravity(null),
								serviceStatsDenun.totalTreatByRespectTiming(null),
								serviceStatsDenun.totalUnTreat(null), serviceStatsDenun.totalUnTreatByGravity(null)))
						.SuggestStatsAndValue(
								Arrays.asList(serviceStatsSuggest.totalSaved(null),
										serviceStatsSuggest.totalSuggest(null)))
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

	@PostMapping(value = "/filtered")
	public ResponseEntity<Object> generateFilterReport(@RequestBody FilterRequest request) {
		// System.out.println("Status choosed");
		// System.out.println(request);
		ApiResponseDto apiResponseDto = Utils.verifyLicence();
		;
		if (apiResponseDto.isStatus() && apiResponseDto.getContent().getClass() == LicenceControl.class) {
			LicenceControl lc = (LicenceControl) apiResponseDto.getContent();
			if (lc.isActif()) {
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
								serviceStatsClaim.totalUnTreatByGravity(request)))
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

}
