package com.sicmagroup.gpr.domain.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LicenceDto {
    private String createdAt;
    private String serial;
    private String company;
    private String activationRequest;
    private String fullname;
    private long id;
    private String email;
    private String updatedAt;
}
