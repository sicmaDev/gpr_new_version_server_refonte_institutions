package com.sicmagroup.gpr.domain.dto;

import java.time.LocalDateTime;

import com.sicmagroup.gpr.domain.enumeration.ClaimType;
import com.sicmagroup.gpr.domain.model.Claim;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertDto {
    private Long claimId;
    private String claimCode;
    private String claimClient;
    private LocalDateTime receiptDateTime;
    private String  retardDay;
    private LocalDateTime declenchedDate;
    private ClaimType type;
}
