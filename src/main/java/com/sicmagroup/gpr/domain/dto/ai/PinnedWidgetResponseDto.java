package com.sicmagroup.gpr.domain.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PinnedWidgetResponseDto {
    private String id;
    private String userId;
    private String title;
    private String description;
    private String chartType;
    private String sqlQuery;
    private Object data;
    private Object kpis;
    private String summary;
    private int displayOrder;
    private String createdAt;
    private String updatedAt;
}
