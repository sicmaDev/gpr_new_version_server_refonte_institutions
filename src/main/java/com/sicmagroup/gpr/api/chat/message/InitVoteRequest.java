package com.sicmagroup.gpr.api.chat.message;

import com.sicmagroup.gpr.domain.dto.claimResponse.UserResponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitVoteRequest {
    private String contenu;
    private String commentaire;
    private Long authorId;
    private String status;
    private String claimCode;
}
