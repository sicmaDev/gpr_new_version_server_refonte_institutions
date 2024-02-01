package com.sicmagroup.gpr.domain.dto.chat;

import java.util.List;

import com.sicmagroup.gpr.domain.dto.claimResponse.UserResponse;
import com.sicmagroup.gpr.domain.enumeration.ChatStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor 
@AllArgsConstructor
public class ChatDto {
     private Long id;


    private UserResponse createdBy;

    private ChatStatus status;

    private List<UserResponse> members;

    private List<UserResponse> guests;

    private List<VoteDto> vote;

    private List<MessageDto> messages;
}
