package com.sicmagroup.gpr.api.claim;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AffectTreatmentRequest {
    private Long claimId;
    private Long affectToId;
    private Long affectorId;
    private Boolean affectedAnonymous;
}
