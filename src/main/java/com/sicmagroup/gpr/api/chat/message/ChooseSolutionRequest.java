package com.sicmagroup.gpr.api.chat.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChooseSolutionRequest {
    private long messageId;
    private String claimCode;

}
