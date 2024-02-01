package com.sicmagroup.gpr.domain.dto.reports.StackedBar;

import java.util.HashMap;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StackedBar {
    List<Long> ids;
    List<String> labels;
    List<StackedBarDataset> datasets;

}
