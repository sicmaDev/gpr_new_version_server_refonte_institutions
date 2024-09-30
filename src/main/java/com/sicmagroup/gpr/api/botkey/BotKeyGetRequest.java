package com.sicmagroup.gpr.api.botkey;

import com.sicmagroup.gpr.domain.enumeration.ClaimType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BotKeyGetRequest {
    private Long userCode;
    private ClaimType claimType;
}
