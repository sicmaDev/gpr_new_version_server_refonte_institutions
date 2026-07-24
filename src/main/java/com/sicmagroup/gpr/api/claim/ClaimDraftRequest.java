package com.sicmagroup.gpr.api.claim;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimDraftRequest {
    private String draftSolution;
    private String draftCommentaire;
    private Long userId;
}
