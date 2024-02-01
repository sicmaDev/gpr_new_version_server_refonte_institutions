package com.sicmagroup.gpr.domain.dto;

import java.time.LocalDateTime;

import com.sicmagroup.gpr.domain.dto.claimResponse.UserResponse;
import com.sicmagroup.gpr.domain.enumeration.SatisfactionStatus;
import com.sicmagroup.gpr.domain.model.User;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor 
@AllArgsConstructor
public class SatisfactionMeasureDto {
    private Long id;
    // private SolutionDto solutionDto;
    private SatisfactionStatus status;
    private UserResponse measurer;
    private LocalDateTime measureDateTime;
    private String commentaire;
}
