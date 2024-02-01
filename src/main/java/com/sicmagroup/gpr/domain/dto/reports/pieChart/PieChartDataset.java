package com.sicmagroup.gpr.domain.dto.reports.pieChart;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PieChartDataset {
    private String backgroundColor;
    private String hoverBackgroundColor;
    private Double data;
    private String libelle;
}
