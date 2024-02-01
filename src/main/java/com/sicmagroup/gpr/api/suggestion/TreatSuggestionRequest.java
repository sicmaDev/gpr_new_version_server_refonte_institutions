package com.sicmagroup.gpr.api.suggestion;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TreatSuggestionRequest {
    
    private Long id;
    private Long treatorId;
    private boolean accepted;
    private String commentaire;
}
