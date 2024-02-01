package com.sicmagroup.gpr.api.config.categorieObjet;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategorieObjetRequest {
    private String libelle;
    private String description;
}
