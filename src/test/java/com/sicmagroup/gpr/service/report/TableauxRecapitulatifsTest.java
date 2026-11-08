package com.sicmagroup.gpr.service.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.sicmagroup.gpr.api.report.FilterRequest;
import com.sicmagroup.gpr.domain.dto.reports.lineChart.LineChart;
import com.sicmagroup.gpr.domain.enumeration.ClaimStatus;
import com.sicmagroup.gpr.domain.enumeration.ClaimType;
import com.sicmagroup.gpr.domain.enumeration.GravityLevel;
import com.sicmagroup.gpr.domain.model.Claim;
import com.sicmagroup.gpr.domain.model.Objet;
import com.sicmagroup.gpr.domain.model.SatisfactionMeasure;
import com.sicmagroup.gpr.domain.model.Solution;
import com.sicmagroup.gpr.repository.ClaimRepository;
import com.sicmagroup.gpr.repository.SuggestionRepository;
import com.sicmagroup.gpr.repository.projection.custom.ClaimPerObjLevelPro;

/**
 * Tableaux récapitulatifs et graphiques du rapport Global : les calculs ne modifient plus le filtre
 * partagé, le taux de satisfaction et le pourcentage de réclamations traitées divisent par les
 * réclamations du filtre, les niveaux de gravité et la courbe de satisfaction sont correctement ventilés,
 * et les filtres de dates unilatéraux / de fin de journée sont pris en compte.
 */
class TableauxRecapitulatifsTest {

    private static FilterRequest filtreAgence() {
        return FilterRequest.builder()
                .objets(new ArrayList<>()).etats(new ArrayList<>()).products(new ArrayList<>())
                .savedBy(new ArrayList<>()).canals(new ArrayList<>())
                .servicePoints(new ArrayList<>(List.of(22L)))
                .build();
    }

    private static List<Claim> claims(int n) {
        List<Claim> l = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            l.add(new Claim());
        }
        return l;
    }

    private static ReportServiceImpl creerReportService(ClaimRepository claimRepo, SuggestionRepository suggestRepo) {
        return new ReportServiceImpl(null, claimRepo, suggestRepo, null, null, null, null, null, null, null, null, null);
    }

    @Test
    void copieIndependante() {
        FilterRequest f = filtreAgence();
        FilterRequest c = FilterRequest.copieDe(f);
        c.setEtats(List.of(ClaimStatus.SATISFIED));
        c.getServicePoints().add(23L);
        assertThat(f.getEtats()).isEmpty();
        assertThat(f.getServicePoints()).containsExactly(22L);
        assertThat(FilterRequest.copieDe(null)).isNull();
    }

    @Test
    void lesCalculsNeModifientPlusLeFiltre() {
        ClaimRepository repo = mock(ClaimRepository.class);
        SuggestionRepository suggestRepo = mock(SuggestionRepository.class);
        when(repo.countClaimByCriteriaAndStatusIn(any(), any())).thenReturn(Collections.emptyList());
        when(repo.countClaimByCriteriaAndStatusNot(any(), any())).thenReturn(Collections.emptyList());
        when(repo.countClaimByCriteriaAndTypeAndStatusIn(any(), any(), any())).thenReturn(Collections.emptyList());
        when(repo.countClaimByCriteriaAndTypeAndStatusNotAndReceiptDateTimeBetween(any(), any()))
                .thenReturn(Collections.emptyList());
        when(suggestRepo.countSuggestByCriteriaAndPeriode(any())).thenReturn(Collections.emptyList());

        StatsClaim statsClaim = new StatsClaim(new com.sicmagroup.gpr.sla.service.EcheanceObjet(new com.sicmagroup.gpr.sla.service.BusinessTime()), repo);
        StatsDenun statsDenun = new StatsDenun(new com.sicmagroup.gpr.sla.service.EcheanceObjet(new com.sicmagroup.gpr.sla.service.BusinessTime()), repo, null, null, null, null);
        ReportServiceImpl reportService = creerReportService(repo, suggestRepo);

        FilterRequest f = filtreAgence();
        f.setEtats(new ArrayList<>(List.of(ClaimStatus.SATISFIED)));

        statsClaim.tauxSatisfaction(f);
        statsClaim.totalReclamantSatisfait(f);
        statsClaim.totalTreat(f);
        statsClaim.totalTreatByGravity(f);
        statsClaim.totalTreatByRespectTiming(f);
        statsClaim.totalUnTreat(f);
        statsClaim.totalUnTreatByGravity(f);
        statsDenun.totalTreat(f);
        statsDenun.totalTreatByGravity(f);
        statsDenun.totalTreatByRespectTiming(f);
        statsDenun.totalUnTreat(f);
        statsDenun.totalUnTreatByGravity(f);
        reportService.tauxResolutionClaim(f);
        reportService.tauxResolutionDenun(f);
        reportService.tauxResolutionObj(f);

        assertThat(f.getEtats()).containsExactly(ClaimStatus.SATISFIED);
    }

    @Test
    void tauxDeSatisfactionDiviseParLesReclamationsDuFiltre() {
        ClaimRepository repo = mock(ClaimRepository.class);
        when(repo.countClaimByCriteriaAndStatusIn(argThat(r -> r != null && r.getEtats().size() == 1), eq(ClaimType.CLAIM)))
                .thenReturn(claims(2));
        when(repo.countClaimByCriteriaAndStatusIn(argThat(r -> r != null && r.getEtats().size() == 5), eq(ClaimType.CLAIM)))
                .thenReturn(claims(4));
        StatsClaim statsClaim = new StatsClaim(new com.sicmagroup.gpr.sla.service.EcheanceObjet(new com.sicmagroup.gpr.sla.service.BusinessTime()), repo);

        assertThat(statsClaim.tauxSatisfaction(filtreAgence()).get("Taux de satisfaction(%)")).isEqualTo(50.0);
        verify(repo, never()).findByTypeAndIsDeletedFalseAndStatusIn(any(), any());
    }

    @Test
    void pourcentageReclamationsTraiteesDiviseParLesReclamationsDuFiltre() {
        ClaimRepository repo = mock(ClaimRepository.class);
        List<Claim> sixClaims = claims(6);
        for (int i = 0; i < 3; i++) {
            sixClaims.get(i).setStatus(ClaimStatus.TREAT);
        }
        for (int i = 3; i < 6; i++) {
            sixClaims.get(i).setStatus(ClaimStatus.SAVED);
        }
        when(repo.countClaimByCriteriaAndStatusNot(any(), eq(ClaimType.CLAIM))).thenReturn(sixClaims);
        StatsClaim statsClaim = new StatsClaim(new com.sicmagroup.gpr.sla.service.EcheanceObjet(new com.sicmagroup.gpr.sla.service.BusinessTime()), repo);

        assertThat(statsClaim.pourcentageReclamationsTraitees(filtreAgence())
                .get("Pourcentage réclamations traitées (%)")).isEqualTo(50.0);
        verify(repo, never()).countByTypeAndIsDeletedFalseAndStatusNot(any(), any());
    }

    @Test
    void nonTraiteesRespecteLeFiltreEtatSelectionne() {
        ClaimRepository repo = mock(ClaimRepository.class);
        StatsClaim statsClaim = new StatsClaim(new com.sicmagroup.gpr.sla.service.EcheanceObjet(new com.sicmagroup.gpr.sla.service.BusinessTime()), repo);

        FilterRequest filtreSeulementTraite = filtreAgence();
        filtreSeulementTraite.setEtats(new ArrayList<>(List.of(ClaimStatus.SATISFIED)));
        assertThat(statsClaim.totalUnTreat(filtreSeulementTraite).get("Nombre de réclamations non traitées"))
                .isEqualTo(0.0);
        verify(repo, never()).countClaimByCriteriaAndStatusIn(any(), any());

        FilterRequest filtreMixte = filtreAgence();
        filtreMixte.setEtats(new ArrayList<>(List.of(ClaimStatus.SAVED, ClaimStatus.SATISFIED)));
        when(repo.countClaimByCriteriaAndStatusIn(argThat(r -> r != null && r.getEtats().equals(List.of(ClaimStatus.SAVED))),
                eq(ClaimType.CLAIM))).thenReturn(claims(4));
        assertThat(statsClaim.totalUnTreat(filtreMixte).get("Nombre de réclamations non traitées"))
                .isEqualTo(4.0);
    }

    @Test
    void evolutionSatisfactionSepareSatisfaitNonSatisfaitEtPartiel() {
        ClaimRepository repo = mock(ClaimRepository.class);
        LocalDateTime now = LocalDateTime.now().withDayOfMonth(10).withHour(12).withMinute(0);
        Claim cSat = claimAvecSatisfaction(ClaimStatus.SATISFIED, now);
        Claim cUnsat1 = claimAvecSatisfaction(ClaimStatus.UNSATISFIED, now);
        Claim cUnsat2 = claimAvecSatisfaction(ClaimStatus.UNSATISFIED, now);
        Claim cPartial = claimAvecSatisfaction(ClaimStatus.PARTIAL_SATISFIED, now);

        when(repo.findByTypeAndIsDeletedFalseAndStatusIn(eq(ClaimType.CLAIM), any()))
                .thenReturn(List.of(cSat, cUnsat1, cUnsat2, cPartial));

        ReportServiceImpl reportService = creerReportService(repo, null);

        LineChart chart = reportService.evolutionSatisfactionByYear(null);
        List<Double> satData = chart.getData().get(0).getData();
        List<Double> unsatData = chart.getData().get(1).getData();
        List<Double> partialData = chart.getData().get(2).getData();

        assertThat(satData.get(11)).isEqualTo(1.0);
        assertThat(unsatData.get(11)).isEqualTo(2.0);
        assertThat(partialData.get(11)).isEqualTo(1.0);
    }

    @Test
    void filterReportAccepteUneBorneDeDateUniqueEtTouteLaJourneeDeFin() {
        ReportServiceImpl reportService = creerReportService(null, null);
        Claim c = new Claim();
        c.setReceiptDateTime(LocalDateTime.of(2026, 5, 15, 16, 45));

        FilterRequest filtreFinMinuit = new FilterRequest();
        filtreFinMinuit.setReceiveEnd("15-05-2026 00:00");
        Boolean accepteFinMinuit = ReflectionTestUtils.invokeMethod(reportService, "filterReport", c, filtreFinMinuit);
        assertThat(accepteFinMinuit).isTrue();

        FilterRequest filtreDebutSeulApres = new FilterRequest();
        filtreDebutSeulApres.setReceiveStart("16-05-2026 00:00");
        Boolean accepteDebutApres = ReflectionTestUtils.invokeMethod(reportService, "filterReport", c, filtreDebutSeulApres);
        assertThat(accepteDebutApres).isFalse();
    }

    @Test
    void graviteAdditionneLesObjetsDUnMemeNiveau() {
        ClaimRepository repo = mock(ClaimRepository.class);
        when(repo.countClaimByCriteriaAndByObjLevel(any(), eq(ClaimType.CLAIM))).thenReturn(List.of(
                new ClaimPerObjLevelPro(1L, "A", GravityLevel.MINEUR, 50L),
                new ClaimPerObjLevelPro(2L, "B", GravityLevel.MINEUR, 32L),
                new ClaimPerObjLevelPro(3L, "C", GravityLevel.GRAVE, 15L)));
        HashMap<String, Double> r = new StatsClaim(new com.sicmagroup.gpr.sla.service.EcheanceObjet(new com.sicmagroup.gpr.sla.service.BusinessTime()), repo).totalByGravity(filtreAgence());

        assertThat(r.get("Nombre de réclamations à niveau de gravité Mineur enregistrées")).isEqualTo(82.0);
        assertThat(r.get("Nombre de réclamations à niveau de gravité Grave enregistrées")).isEqualTo(15.0);
        assertThat(r.get("Nombre de réclamations à niveau de gravité Moyen enregistrées")).isEqualTo(0.0);
    }

    @Test
    void denonciationsFaiblesNonTraiteesCompteesAPart() {
        ClaimRepository repo = mock(ClaimRepository.class);
        when(repo.findByTypeAndStatusNotIn(eq(ClaimType.DENUNCIACION), any()))
                .thenReturn(List.of(denonciation(GravityLevel.MINEUR), denonciation(GravityLevel.MINEUR),
                        denonciation(GravityLevel.MOYEN)));
        HashMap<String, Double> r = new StatsDenun(new com.sicmagroup.gpr.sla.service.EcheanceObjet(new com.sicmagroup.gpr.sla.service.BusinessTime()), repo, null, null, null, null).totalUnTreatByGravity(null);

        assertThat(r.get("Nombre de dénonciations à niveau de gravité Faible non traitées")).isEqualTo(2.0);
        assertThat(r.get("Nombre de dénonciations à niveau de gravité Moyen non traitées")).isEqualTo(1.0);
    }

    @Test
    void suggestionsSansValeurSousUnFiltreParObjet() {
        StatsSuggest statsSuggest = new StatsSuggest(null, null, null, null, null);
        FilterRequest f = filtreAgence();
        f.setObjets(new ArrayList<>(List.of(1L)));

        assertThat(statsSuggest.totalSaved(f)).containsEntry("Nombre de suggestions Enregistrées", null);
        assertThat(statsSuggest.totalSuggest(f)).containsEntry("Nombre de suggestion Prises en compte", null)
                .containsEntry("Nombre de suggestion Non Prises en compte", null);
    }

    @Test
    void filtreParEtatSansBrouillons() {
        ClaimRepository repo = mock(ClaimRepository.class);
        when(repo.countClaimByCriteriaAndStatusIn(any(), any())).thenReturn(Collections.emptyList());
        FilterRequest f = filtreAgence();
        f.setEtats(new ArrayList<>(List.of(ClaimStatus.SATISFIED)));

        new StatsClaim(new com.sicmagroup.gpr.sla.service.EcheanceObjet(new com.sicmagroup.gpr.sla.service.BusinessTime()), repo).pourcentageReelReclamationsTraitees(f);

        verify(repo).countClaimByCriteriaAndStatusIn(argThat(r -> r.getEtats().equals(List.of(ClaimStatus.SATISFIED))),
                eq(ClaimType.CLAIM));
        verify(repo, never()).countClaimByCriteriaAndStatusNot(any(), any());
        assertThat(f.getEtats()).containsExactly(ClaimStatus.SATISFIED);
    }

    private static Claim denonciation(GravityLevel niveau) {
        Objet objet = new Objet();
        objet.setRisqueLevel(niveau);
        Claim c = new Claim();
        c.setObjet(objet);
        return c;
    }

    private static Claim claimAvecSatisfaction(ClaimStatus status, LocalDateTime measureDateTime) {
        SatisfactionMeasure sm = new SatisfactionMeasure();
        sm.setMeasureDateTime(measureDateTime);
        Solution sol = new Solution();
        sol.setSatisfactionMeasure(sm);
        Claim c = new Claim();
        c.setStatus(status);
        c.setSolutions(List.of(sol));
        return c;
    }

    @Test
    void tauxDeSatisfactionSansFiltreInchange() {
        ClaimRepository repo = mock(ClaimRepository.class);
        when(repo.findByTypeAndIsDeletedFalseAndStatusIn(eq(ClaimType.CLAIM), argThat(l -> l != null && l.size() == 1)))
                .thenReturn(claims(6));
        when(repo.findByTypeAndIsDeletedFalseAndStatusIn(eq(ClaimType.CLAIM), argThat(l -> l != null && l.size() == 5)))
                .thenReturn(claims(26));
        StatsClaim statsClaim = new StatsClaim(new com.sicmagroup.gpr.sla.service.EcheanceObjet(new com.sicmagroup.gpr.sla.service.BusinessTime()), repo);

        assertThat(statsClaim.tauxSatisfaction(null).get("Taux de satisfaction(%)")).isEqualTo(23.08);
    }
}
