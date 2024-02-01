package com.sicmagroup.gpr.domain.dto;

import java.time.LocalDateTime;

import com.sicmagroup.gpr.domain.dto.claimResponse.UserResponse;
import com.sicmagroup.gpr.domain.enumeration.SolutionStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor 
@AllArgsConstructor
public class SolutionDto {
    private Long id;
    private String content;
    private UserResponse author;
    private SolutionStatus status;
    // private Long claimDto;
    private SatisfactionMeasureDto satisfactionMeasureDto;
    private String commentaire;
    private String motifDesaprobation;

    private UserResponse approuver;
    private UserResponse unApprouver;

    private LocalDateTime approuvedAt;
    private LocalDateTime unApprouvedAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
