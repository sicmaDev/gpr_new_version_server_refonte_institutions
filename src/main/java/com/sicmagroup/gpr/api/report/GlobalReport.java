package com.sicmagroup.gpr.api.report;

import com.sicmagroup.gpr.domain.dto.reports.StackedBar.StackedBar;
import com.sicmagroup.gpr.domain.dto.reports.StackedBar.StackedBarDataset;
import com.sicmagroup.gpr.domain.dto.reports.lineChart.LineChart;
import com.sicmagroup.gpr.domain.dto.reports.pieChart.PieChartDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlobalReport {
    private PieChartDto repartitionClaimDenunSuggest;
    private PieChartDto repartitionClaimDenuSuggestPerCanal;
    private StackedBar nbreObjectPerCanalAndAgence;
    private StackedBar nbreObjetPerObjetAndAgence;
    private PieChartDto repartitionObjectByObj;
    private LineChart evolutionClaimDenunSuggestByYear;
    private double tauxResolution;
    private StackedBar evolutionObjByYearAndAgence;
}
