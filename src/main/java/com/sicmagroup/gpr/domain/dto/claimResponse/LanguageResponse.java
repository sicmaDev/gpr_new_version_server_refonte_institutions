package com.sicmagroup.gpr.domain.dto.claimResponse;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LanguageResponse {
    private Long id;
	private String libelle;
	private String description;
}
