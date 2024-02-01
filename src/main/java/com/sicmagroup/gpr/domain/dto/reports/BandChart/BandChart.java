package com.sicmagroup.gpr.domain.dto.reports.BandChart;

import java.util.List;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BandChart {

    List<String> labels;
    List<Double> datas;
    List<String> backgroundColors;
    List<String> borderColors;
    String borderWidth;
    
}
