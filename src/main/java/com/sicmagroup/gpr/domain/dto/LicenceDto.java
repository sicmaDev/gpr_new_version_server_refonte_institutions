package com.sicmagroup.gpr.domain.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LicenceDto {
    private String createdAt;
    private String serial;
    private String company;
    private String activationRequest;
    private String fullname;
    private String email;
    private String updatedAt;
}
