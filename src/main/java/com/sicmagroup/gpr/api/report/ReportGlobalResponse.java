package com.sicmagroup.gpr.api.report;

import java.util.HashMap;

import com.sicmagroup.gpr.domain.dto.reports.BandChart.BandChart;
import com.sicmagroup.gpr.domain.dto.reports.pieChart.PieChart;
import com.sicmagroup.gpr.domain.dto.reports.pieChart.PieChartDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportGlobalResponse {
    
    private GlobalReport global;
    private ClaimReport claimReport;
    private DenunReport denunReport;
    private SuggestionReport suggestionReport;
    private StatisticReport statistic;
    private HashMap<String,Object> newVersionStat;
    
}
