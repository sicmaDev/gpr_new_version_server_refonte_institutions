package com.sicmagroup.gpr.api.report;

import com.sicmagroup.gpr.domain.dto.reports.BandChart.BandChart;
import com.sicmagroup.gpr.domain.dto.reports.StackedBar.StackedBar;
import com.sicmagroup.gpr.domain.dto.reports.lineChart.LineChart;
import com.sicmagroup.gpr.domain.dto.reports.pieChart.PieChart;
import com.sicmagroup.gpr.domain.dto.reports.pieChart.PieChartDto;
import com.sicmagroup.gpr.domain.dto.reports.tableTotal.ObjectTotal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimReport {
    private BandChart nbreClaimPerAgence;
    private PieChartDto repartitionClaimPerAgence;
    private ObjectTotal basicStats;
    private PieChartDto repartitionClaimPerCanal;
    private StackedBar nbreClaimPerCanalPerAgence;
    private PieChartDto repartitionClaimPerObjet;
    private StackedBar nbreClaimPerObjPerAgence;
    private PieChartDto repartitionClaimPerGender;
    private StackedBar nbreClaimPerGenderPerAgence;
    private PieChartDto repartitionClaimPerObjRisque;
    private StackedBar nbreClaimPerObjLevelAndAgence;
    private PieChartDto repartitionClaimBySatisfaction;
    private StackedBar nbreClaimTreatInDelaiOrNot;
    private StackedBar tauxClaimSatisfactionByMonth;
    private StackedBar tauxClaimSatisfactionByMonthByAgence;
    private StackedBar nbreClaimTreatInDelaiOrNotByMonth;
    private StackedBar nbreClaimTreatInDelaiOrNotByMonthByAgence;
    private StackedBar nbreDenunTreatInDelaiOrNotByMonth;
    private StackedBar nbreDenunTreatInDelaiOrNotByMonthByAgence;
    private LineChart evolutionSatisfactionByThisYear;
    private double tauxResolution;
    private StackedBar evolutionByAgenceAndYear;

}
