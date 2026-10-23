package com.sicmagroup.gpr.service.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.sicmagroup.gpr.api.report.FilterRequest;
import com.sicmagroup.gpr.domain.dto.reports.pieChart.PieChartDto;
import com.sicmagroup.gpr.domain.enumeration.ClaimStatus;
import com.sicmagroup.gpr.repository.ClaimRepository;
import com.sicmagroup.gpr.repository.projection.ClaimPerStatusSatisfactionProjection;
import com.sicmagroup.gpr.repository.projection.custom.ObjectTotalPerStatusPro;

/** Camembert de satisfaction : toujours trois libellés, trois couleurs et trois valeurs, avec ou sans filtre. */
class SatisfactionReportTest {

    private final ClaimRepository claimRepository = mock(ClaimRepository.class);
    private final ReportServiceImpl service = mock(ReportServiceImpl.class, CALLS_REAL_METHODS);

    SatisfactionReportTest() {
        ReflectionTestUtils.setField(service, "claimRepository", claimRepository);
    }

    private ClaimPerStatusSatisfactionProjection projection(String status, long total) {
        return new ClaimPerStatusSatisfactionProjection() {
            public String getStatus() { return status; }
            public Long getTotal() { return total; }
        };
    }

    @Test
    void sansFiltreTroisValeursPourTroisLibelles() {
        // le dépôt regroupe déjà classées et contentieux avec « non satisfait »
        when(claimRepository.countClaimPerSatisfaction()).thenReturn(List.of(
                projection("SATISFIED", 6), projection("UNSATISFIED", 3), projection("PARTIAL_SATISFIED", 1)));

        PieChartDto pie = service.repartitionClaimBySatisfaction(null);

        assertThat(pie.getLabels()).containsExactly("SATISFAIT", "NON SATISFAIT", "PARTIELLEMENT SATISFAIT");
        assertThat(pie.getBackgroundColors()).hasSize(3);
        assertThat(pie.getDatas()).hasSize(3);
        assertThat(pie.getDatas().get(0)).isEqualTo(60.0);
    }

    @Test
    void avecFiltreClasseesEtContentieuxCompentCommeNonSatisfaits() {
        when(claimRepository.countClaimByCriteriaAndSatisfaction(org.mockito.ArgumentMatchers.any(FilterRequest.class)))
                .thenReturn(List.of(
                        ObjectTotalPerStatusPro.builder().status(ClaimStatus.SATISFIED).total(5L).build(),
                        ObjectTotalPerStatusPro.builder().status(ClaimStatus.UNSATISFIED).total(1L).build(),
                        ObjectTotalPerStatusPro.builder().status(ClaimStatus.CLASSED).total(2L).build(),
                        ObjectTotalPerStatusPro.builder().status(ClaimStatus.LITIGATION).total(2L).build()));

        PieChartDto pie = service.repartitionClaimBySatisfaction(FilterRequest.builder().build());

        assertThat(pie.getLabels()).hasSize(3);
        assertThat(pie.getDatas()).hasSize(3);
        assertThat(pie.getDatas().get(0)).isEqualTo(50.0);   // satisfaits : 5 sur 10
        assertThat(pie.getDatas().get(1)).isEqualTo(50.0);   // non satisfaits : 1 + 2 + 2 sur 10
        assertThat(pie.getDatas().get(2)).isEqualTo(0.0);
    }

    @Test
    void sansAucuneDonneeLeCamembertResteComplet() {
        when(claimRepository.countClaimPerSatisfaction()).thenReturn(List.of());

        PieChartDto pie = service.repartitionClaimBySatisfaction(null);

        assertThat(pie.getLabels()).hasSize(3);
        assertThat(pie.getDatas()).hasSize(3);
    }
}
