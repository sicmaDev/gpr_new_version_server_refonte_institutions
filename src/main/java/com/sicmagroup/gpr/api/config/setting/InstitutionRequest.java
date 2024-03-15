package com.sicmagroup.gpr.api.config.setting;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data 
@AllArgsConstructor 
@NoArgsConstructor
@Builder
public class InstitutionRequest {

    private String denomination;
    private String email;
    private String numAgrement;
    private String adresse;
    private String tel;
    private String logo;
    private String pays;
    private String paysCode;
}
