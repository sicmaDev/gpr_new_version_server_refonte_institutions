package com.sicmagroup.gpr.api.report;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BceaoRequest {
    private String startDate;
    private String endDate;
}
