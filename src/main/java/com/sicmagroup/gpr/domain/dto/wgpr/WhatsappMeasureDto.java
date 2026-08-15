package com.sicmagroup.gpr.domain.dto.wgpr;

import lombok.Data;

@Data
public class WhatsappMeasureDto {
    private Long   claimId;
    private Long   solutionId;
    private Long   measurerId;
    private String satisfactionStatus; // SATISFIED | PARTIAL | UNSATISFIED
    private String commentaire;
}
