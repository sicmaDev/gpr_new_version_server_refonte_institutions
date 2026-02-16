package com.sicmagroup.gpr.domain.dto;

import java.time.LocalDateTime;

import java.util.List;

import com.sicmagroup.gpr.domain.enumeration.GravityLevel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PhoneCheckResponseDto {
    private boolean exists;
    private String message;
    private List<ClaimDto> claims;
}



