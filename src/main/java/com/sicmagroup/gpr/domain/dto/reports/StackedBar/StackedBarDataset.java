package com.sicmagroup.gpr.domain.dto.reports.StackedBar;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StackedBarDataset {
    private String label;
    private Long id;
    private List<Double> data;
    private String backgroundColor;
    private String borderColor;
}
