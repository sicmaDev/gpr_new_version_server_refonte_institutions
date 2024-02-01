package com.sicmagroup.gpr.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sicmagroup.gpr.domain.model.SatisfactionMeasure;
import com.sicmagroup.gpr.domain.model.Solution;
import java.util.List;


public interface SatisfactionMeasureRepository extends JpaRepository<SatisfactionMeasure, Long>{
    
    Optional<SatisfactionMeasure> findBySolution(Solution solution);
}
