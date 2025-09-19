package com.sicmagroup.gpr.domain.dto;

import java.time.LocalDateTime;

import com.sicmagroup.gpr.domain.enumeration.ClaimStatus;
import com.sicmagroup.gpr.domain.enumeration.ClaimType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtraContentResponse {
    private Long id;
	private String contenu;
	private ClaimStatus status;
	private ClaimType type;
    private UserDto user;
    private LocalDateTime createdAt;
}
