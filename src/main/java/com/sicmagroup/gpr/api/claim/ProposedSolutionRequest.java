package com.sicmagroup.gpr.api.claim;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProposedSolutionRequest {
    
    private String solution;
    private Long claimId;
    private Long treatorId;
    private String commentaire;
    private boolean isExisting;
    private Long existingId;
}
