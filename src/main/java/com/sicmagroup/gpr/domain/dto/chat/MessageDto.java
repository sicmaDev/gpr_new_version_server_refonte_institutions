package com.sicmagroup.gpr.domain.dto.chat;

import com.sicmagroup.gpr.domain.dto.claimResponse.UserResponse;
import com.sicmagroup.gpr.domain.model.chat.Vote;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor 
@AllArgsConstructor
public class MessageDto {
      private Long id;

    private String content;

    private UserResponse sender;
    private Long chatId;
    private String status;
    private String createdAt;
       
    private boolean isVote;
    private VoteDto voteDto;

}
