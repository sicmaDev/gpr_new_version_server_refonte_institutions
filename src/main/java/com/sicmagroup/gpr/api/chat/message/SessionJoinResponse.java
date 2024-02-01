package com.sicmagroup.gpr.api.chat.message;

import com.sicmagroup.gpr.domain.dto.claimResponse.UserResponse;
import com.sicmagroup.gpr.domain.enumeration.MessageStatus;
import com.sicmagroup.gpr.domain.enumeration.Role;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionJoinResponse {
    private Long id;
    private String code;
    private String firstAndLastName;
    private Role role;
    private String claimCode;
    private MessageStatus status;
    private boolean isGuest;
}
