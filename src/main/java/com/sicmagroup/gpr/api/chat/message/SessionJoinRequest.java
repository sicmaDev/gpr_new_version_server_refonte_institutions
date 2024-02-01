package com.sicmagroup.gpr.api.chat.message;

import com.sicmagroup.gpr.domain.enumeration.MessageStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionJoinRequest {
    private Long userId;
    private String claimCode;
    private MessageStatus status;
}
