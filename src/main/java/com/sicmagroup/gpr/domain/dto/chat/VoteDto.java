package com.sicmagroup.gpr.domain.dto.chat;

import java.util.List;

import com.sicmagroup.gpr.domain.dto.claimResponse.UserResponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor 
@AllArgsConstructor
public class VoteDto {
     private Long id;

    private String contenu;

    private String commentaire;

    private List<UserVoteDto> userVote;

    private UserResponse author;

    private boolean isChoosed;

    private Long chatId;

    private String status;

    private Long messageId;
}
