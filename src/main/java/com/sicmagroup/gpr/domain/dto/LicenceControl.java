package com.sicmagroup.gpr.domain.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LicenceControl {
    private boolean isActif;
    private long dayBefore;
    private long id;
    private long maxPoste;
    private String message;
}
