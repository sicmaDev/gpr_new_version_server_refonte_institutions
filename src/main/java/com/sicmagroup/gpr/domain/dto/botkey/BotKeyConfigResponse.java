package com.sicmagroup.gpr.domain.dto.botkey;


import java.util.List;

import com.sicmagroup.gpr.domain.enumeration.Gender;
import com.sicmagroup.gpr.domain.model.CategorieObjet;
import com.sicmagroup.gpr.domain.model.CollectionChannel;
import com.sicmagroup.gpr.domain.model.ExistingSolution;
import com.sicmagroup.gpr.domain.model.ExternalRecourse;
import com.sicmagroup.gpr.domain.model.Language;
import com.sicmagroup.gpr.domain.model.Objet;
import com.sicmagroup.gpr.domain.model.Poste;
import com.sicmagroup.gpr.domain.model.Product;
import com.sicmagroup.gpr.domain.model.ServicePoint;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor 
@AllArgsConstructor
public class BotKeyConfigResponse {
    private List<ServicePoint> servicePoints;
    private List<Gender> genders;
    private List<Language> languages;
    private List<Poste> postes;
    private List<CollectionChannel> modalites;
    private List<Product> products;
    private List<Objet>objets;
    private List<CategorieObjet> categorieObjets;
    
    private List<ExternalRecourse> externalRecourses;
    private List<ExistingSolution> existingSolutions;


    

}
