package com.sicmagroup.gpr.api.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatInitRequest {
    private Long creatorId;
    private Long claimId;
    
}
