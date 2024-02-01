package com.sicmagroup.gpr.api.chat.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewMessageRequest {
    private String content;
    private Long senderId;
    private String claimCode;
    private String status;
    private boolean isVote;
}
