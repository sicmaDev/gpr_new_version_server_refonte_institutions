package com.sicmagroup.gpr.api.config.existingSolution;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateExistingSolutionRequest {
    private Long id;
    private String content;
    private Long objet;
}
