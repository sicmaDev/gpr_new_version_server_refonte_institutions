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
public class DenunReport {
    private BandChart nbreDenunPerAgence;
    private PieChartDto repartitionDenunPerAgence;
    private ObjectTotal basicStats;
    private PieChartDto repartitionDenunPerCanal;
    private StackedBar nbreDenunPerCanalPerAgence;
    private PieChartDto repartitionDenunPerObjet;
    private StackedBar nbreDenunPerObjPerAgence;
    private PieChartDto repartitionDenunPerGender;
    private PieChartDto repartitionDenunPerObjRisque;
    private StackedBar nbreDenunPerObjLevelAndAgence;
    private double tauxResolution;
    private StackedBar evolutionByAgenceAndYear;
}
