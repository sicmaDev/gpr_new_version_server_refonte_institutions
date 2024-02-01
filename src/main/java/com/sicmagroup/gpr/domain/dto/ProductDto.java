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
public class ProductDto {
    private Long id;
	private String libelle;
	private String description;
    private boolean isDeleted;
    private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}
