package com.sicmagroup.gpr.domain.dto.ai;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExportSolutionDto {
    private Long id;
    private Long claimId;
    private Long authorId;
    private String status;
    private String content;
    private String commentaire;
    private String motifDesaprobation;
    private Long approverId;
    private LocalDateTime approvedAt;
    private LocalDateTime unapprovedAt;
    private Boolean isAiProposed;
    private String aiSelectedSolution;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private ExportSatisfactionMeasureDto satisfactionMeasure;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ExportSatisfactionMeasureDto {
        private Long id;
        private Long solutionId;
        private Long measurerId;
        private String status;
        private String commentaire;
        private LocalDateTime measureDateTime;
    }
}
