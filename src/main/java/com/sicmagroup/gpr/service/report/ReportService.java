package com.sicmagroup.gpr.service.report;

import java.util.HashMap;
import java.util.List;

import com.sicmagroup.gpr.api.report.FilterRequest;
import com.sicmagroup.gpr.domain.dto.reports.BandChart.BandChart;
import com.sicmagroup.gpr.domain.dto.reports.StackedBar.StackedBar;
import com.sicmagroup.gpr.domain.dto.reports.lineChart.LineChart;
import com.sicmagroup.gpr.domain.dto.reports.pieChart.PieChart;
import com.sicmagroup.gpr.domain.dto.reports.pieChart.PieChartDto;
import com.sicmagroup.gpr.domain.dto.reports.tableTotal.ObjectTotal;
import com.sicmagroup.gpr.domain.enumeration.ClaimType;

import io.micrometer.common.lang.Nullable;

public interface ReportService {

    public PieChartDto repartitionClaimDenunSuggest(@Nullable FilterRequest request);

    public BandChart numberClaimPerServicePoint(@Nullable FilterRequest request);

    public BandChart numberDenunPerServicePoint(@Nullable FilterRequest request);

    public BandChart numberSuggestPerServicePoint(@Nullable FilterRequest request);

    public PieChartDto repartitionClaimPerServicePoint(@Nullable FilterRequest request);

    public PieChartDto repartitionDenunPerServicePoint(@Nullable FilterRequest request);

    public PieChartDto repartitionSuggestPerServicePoint(@Nullable FilterRequest request);

    public ObjectTotal statusTotals(ClaimType type, @Nullable FilterRequest request);

    public PieChartDto repartitionClaimByCanal(@Nullable FilterRequest request);

    public PieChartDto repartitionDenunByCanal(@Nullable FilterRequest request);

    public PieChartDto repartitionSuggestByCanal(@Nullable FilterRequest request);

    public PieChartDto repartitionObjectByCanal(@Nullable FilterRequest request);

    public StackedBar numberClaimByCanalByAgence(@Nullable FilterRequest request);

    public StackedBar numberDenunByCanalByAgence(@Nullable FilterRequest request);

    public StackedBar numberSuggestByCanalByAgence(@Nullable FilterRequest request);

    public StackedBar numberObjectByCanalByAgence(@Nullable FilterRequest request);

    public PieChartDto repartitionClaimPerObjet(@Nullable FilterRequest request);

    public PieChartDto repartitionDenunPerObjet(@Nullable FilterRequest request);

    public PieChartDto repartitionObjPerObjet(@Nullable FilterRequest request);

    public StackedBar numberObjByObjetByAgence(@Nullable FilterRequest request);

    public StackedBar numberClaimPerObjetByAgence(@Nullable FilterRequest request);

    public StackedBar numberDenunPerObjetByAgence(@Nullable FilterRequest request);

    public PieChartDto repartitionClaimByGender(@Nullable FilterRequest request);

    public PieChartDto repartitionDenunByGender(@Nullable FilterRequest request);

    public PieChartDto repartitionSuggestByGender(@Nullable FilterRequest request);

    public StackedBar numberClaimByGenderByAgence(@Nullable FilterRequest request);

    public StackedBar numberSuggestByGenderAgence(@Nullable FilterRequest request);

    public PieChartDto repartitionClaimByGravityLevel(@Nullable FilterRequest request);

    public PieChartDto repartitionDenunByGravityLevel(@Nullable FilterRequest request);

    public StackedBar numberClaimByGravityByAngence(@Nullable FilterRequest request);

    public StackedBar numberDenunByGravityByAgence(@Nullable FilterRequest request);

    public PieChartDto repartitionClaimBySatisfaction(@Nullable FilterRequest request);

    public StackedBar numberClaimTreatInDelaiOrNot(@Nullable FilterRequest request);

    public LineChart evolutionSatisfactionByYear(@Nullable FilterRequest request);

    public LineChart evolutionClaimDenunSuggestbyYear(@Nullable FilterRequest request);

    public double tauxResolutionClaim(@Nullable FilterRequest request);

    public double tauxResolutionDenun(@Nullable FilterRequest request);

    public StackedBar evolutionClaimBySpAndYear(@Nullable FilterRequest request);

    public StackedBar evolutionDenunBySpAndYear(@Nullable FilterRequest request);

    public StackedBar evolutionSuggestBySpAndYear(@Nullable FilterRequest request);

    public StackedBar evolutionObjBySpAndYear(@Nullable FilterRequest request);

    public double tauxResolutionSuggest(@Nullable FilterRequest request);

    public double tauxResolutionObj(@Nullable FilterRequest request);

    public HashMap<String, Double> statisticsClaims(@Nullable FilterRequest request);

}
