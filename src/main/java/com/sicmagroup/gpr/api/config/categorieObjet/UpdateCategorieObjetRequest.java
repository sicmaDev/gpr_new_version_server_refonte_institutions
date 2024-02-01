package com.sicmagroup.gpr.api.config.categorieObjet;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCategorieObjetRequest {
    private Long id;
    private String libelle;
    private String description;
}
