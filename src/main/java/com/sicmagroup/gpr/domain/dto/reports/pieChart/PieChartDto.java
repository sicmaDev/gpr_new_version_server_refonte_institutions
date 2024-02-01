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
public class PieChartDto {
    private List<Long> ids;

    private List<String> labels;

    private List<Double> datas;

    private List<String> backgroundColors;

    private List<String> hoverColors;
}
