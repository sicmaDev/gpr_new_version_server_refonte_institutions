package com.sicmagroup.gpr.api.webhook;

import com.sicmagroup.gpr.domain.enumeration.ClaimType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookRequest {
    private Long userCode;
    private ClaimType claimType;
}
