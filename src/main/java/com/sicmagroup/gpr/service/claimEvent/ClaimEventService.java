package com.sicmagroup.gpr.service.claimEvent;

import java.util.List;

import com.sicmagroup.gpr.api.claim.ClaimEventResponse;
import com.sicmagroup.gpr.domain.enumeration.ClaimEventType;
import com.sicmagroup.gpr.domain.enumeration.ClaimType;

public interface ClaimEventService {
    void log(Long claimId, String claimCode, ClaimType claimType, ClaimEventType eventType,
             String actorName, String actorEmail, String metadata);

    List<ClaimEventResponse> getByClaimId(Long claimId);
}
