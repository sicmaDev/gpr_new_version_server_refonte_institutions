package com.sicmagroup.gpr.api.config.setting;

import com.sicmagroup.gpr.domain.enumeration.ClaimType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SmsTestRequest {

    public String phone;
    public String message;
    public String claimCode;
    public Long claimId;
    public ClaimType claimType;
    public String senderName;
    public String senderEmail;
    public String clientName;
}
