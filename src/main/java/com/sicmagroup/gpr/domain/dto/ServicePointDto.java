package com.sicmagroup.gpr.domain.dto;

import java.time.LocalDateTime;

import com.sicmagroup.gpr.domain.model.ServicePoint;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServicePointDto {
    private Long id;
    private String uuid;
    private String libelle;
    private String description;
	private String type;    
    private boolean isDeleted;
    private boolean isPrincipalAgence;
    private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
    private Long direction_id;
}
