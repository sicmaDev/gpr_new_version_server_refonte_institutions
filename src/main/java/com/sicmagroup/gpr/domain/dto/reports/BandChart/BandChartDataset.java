package com.sicmagroup.gpr.domain.dto.reports.BandChart;

import java.util.ArrayList;
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
public class BandChartDataset {
    private Long id;
    private Double data;
    private String backgroundColor;
    private String hoverBackgroundColor;
    private String borderColor;
    private String hoverBorderColor;
    private int borderWidth;
    private String libelle;

}
