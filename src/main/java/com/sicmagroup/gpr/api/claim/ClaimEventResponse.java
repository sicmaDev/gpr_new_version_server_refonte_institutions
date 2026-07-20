package com.sicmagroup.gpr.api.claim;

import java.time.LocalDateTime;

import com.sicmagroup.gpr.domain.enumeration.ClaimEventType;
import com.sicmagroup.gpr.domain.enumeration.ClaimType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimEventResponse {
    private Long id;
    private Long claimId;
    private String claimCode;
    private ClaimType claimType;
    private ClaimEventType eventType;
    private String actorName;
    private String actorEmail;
    private String metadata;
    private LocalDateTime createdAt;
}
