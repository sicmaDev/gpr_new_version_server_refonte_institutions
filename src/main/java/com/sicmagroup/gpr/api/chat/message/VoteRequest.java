package com.sicmagroup.gpr.api.chat.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoteRequest {
    private boolean removeVote;
    private boolean pour;
    private String claimCode;
    private Long authorId;
    private long messageId;

}
