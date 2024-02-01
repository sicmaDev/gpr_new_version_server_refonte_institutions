package com.sicmagroup.gpr.api.claim;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LitigateClaimRequest {
    
    private Long userId;
    private Long claimId;
    private String externalRecourseChoosed;
}
