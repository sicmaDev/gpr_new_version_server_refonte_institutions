package com.sicmagroup.gpr.domain.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PinnedWidgetRequestDto {
    private String id;
    private String title;
    private String description;
    private String chartType;
    private String sqlQuery;
    private Object data;
    private Object kpis;
    private String summary;
    private Integer displayOrder;
}
