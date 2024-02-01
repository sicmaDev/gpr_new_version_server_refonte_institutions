package com.sicmagroup.gpr.service.categorieObjet;

import java.util.List;

import com.sicmagroup.gpr.api.config.categorieObjet.CategorieObjetRequest;
import com.sicmagroup.gpr.api.config.categorieObjet.UpdateCategorieObjetRequest;
import com.sicmagroup.gpr.domain.model.CategorieObjet;

public interface CategorieObjetService {
    
    List<CategorieObjet> getAll();
    CategorieObjet getOneById(Long id) throws Exception;
    CategorieObjet saveOne(CategorieObjetRequest request);
    CategorieObjet updateOne(UpdateCategorieObjetRequest request) throws Exception; 
    void removeOne(Long id) throws Exception;
    
}
