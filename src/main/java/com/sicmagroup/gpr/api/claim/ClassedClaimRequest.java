package com.sicmagroup.gpr.api.claim;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassedClaimRequest {
    
    private Long userId;
    private Long claimId;
    
}
