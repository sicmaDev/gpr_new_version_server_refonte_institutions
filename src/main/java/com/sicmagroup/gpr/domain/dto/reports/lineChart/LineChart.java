package com.sicmagroup.gpr.domain.dto.reports.lineChart;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LineChart {
    List<Long> ids;
    List<String> labels;
    List<LineDataset> data;
}
