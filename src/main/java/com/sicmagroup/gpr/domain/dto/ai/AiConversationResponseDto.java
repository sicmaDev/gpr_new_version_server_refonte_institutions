package com.sicmagroup.gpr.domain.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiConversationResponseDto {
    private String conversationId;
    private String userId;
    private String threadId;
    private String title;
    private Object messages;
    private String createdAt;
    private String updatedAt;
}
