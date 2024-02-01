package com.sicmagroup.gpr.domain.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LanguageDto {
    private Long id;
	private String libelle;
	private String description;
    private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
    private boolean isDeleted; 
}
