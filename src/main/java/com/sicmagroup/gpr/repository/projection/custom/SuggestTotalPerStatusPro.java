package com.sicmagroup.gpr.repository.projection.custom;

import com.sicmagroup.gpr.domain.enumeration.ClaimStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuggestTotalPerStatusPro {
    private ClaimStatus status;
    private boolean accepted;
    private Long total;
}
