package com.sicmagroup.gpr.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sicmagroup.gpr.domain.model.ExistingSolution;
import com.sicmagroup.gpr.domain.model.Objet;

import java.util.List;


public interface ExistingSolutionRepository extends JpaRepository<ExistingSolution, Long> {
    
    List<ExistingSolution> findByObjet(Objet objet);
}
