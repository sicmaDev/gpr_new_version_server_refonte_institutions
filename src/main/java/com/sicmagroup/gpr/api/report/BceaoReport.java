package com.sicmagroup.gpr.api.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BceaoReport {
    private int totalClaim;
    private int totalDenun;
    private int totalSuggest;
    private int totalClaimTreat;
    private int totalClaimUnResolve;
    private double tauxClaimTreat;
    private double tauxClaimTreatRespectingTiming;
    private double tauxSatisfaction;
    private int totalLigitigateClaimInPeriode;
    private int totalLitigateClaim;
    private BceaoClaimDetails[] claimsReceivedInPeriod;
    private BceaoClaimDetails[] claimsTreatInPeriod;
    private BceaoClaimDetails[] claimsUnResolveInPeriod;
    private BceaoClaimDetails[] claimsLitigateInPeriod;
    private String periode;
    private String piloteName;
    private String piloteContact;
}
