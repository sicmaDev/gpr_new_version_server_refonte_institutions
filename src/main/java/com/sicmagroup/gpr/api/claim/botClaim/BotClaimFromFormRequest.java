package com.sicmagroup.gpr.api.claim.botClaim;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BotClaimFromFormRequest {
    private String firstAndLastName;
    private String tel;
    private String content;
}
