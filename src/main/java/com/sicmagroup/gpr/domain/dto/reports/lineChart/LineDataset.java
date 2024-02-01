package com.sicmagroup.gpr.domain.dto.reports.lineChart;

import java.util.List;

import com.sicmagroup.gpr.domain.dto.reports.StackedBar.StackedBarDataset;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LineDataset {
    private String label;
    private Long id;
    private List<Double> data;
    String borderColor;
    String  backgroundColor;
}
