package com.sicmagroup.gpr.api.claim;

import com.sicmagroup.gpr.domain.enumeration.SatisfactionStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeasureSatisfactionBotRequest {
    // private Long solutionId; // dernière solution
    private String codeClient; // récupérer à partir du codeClient
    // private Long measurerId; // sera à null 
    private  SatisfactionStatus satisfactionStatus; // sera envoyée
    private String commentaire; // sera envoyée ou null
}
