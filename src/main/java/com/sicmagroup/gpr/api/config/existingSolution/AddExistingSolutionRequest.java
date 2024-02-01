package com.sicmagroup.gpr.api.config.existingSolution;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddExistingSolutionRequest {
    private String content;
    private Long objet;
}
