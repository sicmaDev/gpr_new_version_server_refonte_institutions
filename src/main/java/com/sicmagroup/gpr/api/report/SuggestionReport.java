package com.sicmagroup.gpr.api.report;

import com.sicmagroup.gpr.domain.dto.reports.BandChart.BandChart;
import com.sicmagroup.gpr.domain.dto.reports.StackedBar.StackedBar;
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
public class SuggestionReport {
    private BandChart nbreSuggestPerAgence;
    private PieChartDto repartitionSuggestPerAgence;
    private ObjectTotal basicStats;
    private PieChartDto repartitionSuggestPerCanal;
    private StackedBar nbreSuggestPerCanalPerAgence;
    private PieChartDto repartitionSuggestPerGender;
    private StackedBar nbreSuggestPerGenderPerAgence;
    private double tauxResolution;
    private StackedBar evolutionSuggestByYearAndAgence;
}
