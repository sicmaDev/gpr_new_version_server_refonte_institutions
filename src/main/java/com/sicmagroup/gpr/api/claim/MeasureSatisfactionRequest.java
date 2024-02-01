package com.sicmagroup.gpr.api.claim;

import com.sicmagroup.gpr.domain.enumeration.SatisfactionStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeasureSatisfactionRequest {
    private Long solutionId;
    private Long claimId;
    private Long measurerId;
    private SatisfactionStatus satisfactionStatus;
    private String commentaire;
}
