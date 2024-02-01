package com.sicmagroup.gpr.domain.dto.claimResponse;

import java.util.List;

import com.sicmagroup.gpr.domain.dto.CategorieObjetDto;
import com.sicmagroup.gpr.domain.dto.ExistingSolutionResponse;
import com.sicmagroup.gpr.domain.enumeration.GravityLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@Builder
@NoArgsConstructor 
@AllArgsConstructor
public class ObjetResponse {
    private Long id;
    private String libelle;
    private String description;
    private GravityLevel risqueLevel;
    private int processingTime;
    private List<ExistingSolutionResponse> existingSolutions;
    private CategorieObjetDto categorie;
}
