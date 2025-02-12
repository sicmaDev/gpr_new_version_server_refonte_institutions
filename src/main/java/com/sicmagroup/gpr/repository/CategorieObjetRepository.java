package com.sicmagroup.gpr.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sicmagroup.gpr.domain.model.CategorieObjet;
import com.sicmagroup.gpr.domain.model.ServicePoint;

public interface CategorieObjetRepository extends JpaRepository<CategorieObjet, Long> {
    
    
}
