package com.sicmagroup.gpr.api.config.setting;

import com.sicmagroup.gpr.domain.enumeration.ClaimType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailRequest {
    private String email;
    private String message;
    private String subject;
    private String claimCode;
    private Long claimId;
    private ClaimType claimType;
    private String senderName;
    private String senderEmail;
    private String clientName;
}