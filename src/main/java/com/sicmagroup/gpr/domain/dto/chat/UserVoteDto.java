package com.sicmagroup.gpr.domain.dto.chat;

import com.sicmagroup.gpr.domain.dto.claimResponse.UserResponse;
import com.sicmagroup.gpr.domain.enumeration.VoteType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor 
@AllArgsConstructor
public class UserVoteDto {
    private Long id;
    private UserResponse author;
    private VoteType voteType;
}
