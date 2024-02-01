package com.sicmagroup.gpr.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sicmagroup.gpr.domain.model.Claim;
import com.sicmagroup.gpr.domain.model.Solution;
import java.util.List;


public interface SolutionRepository extends JpaRepository<Solution, Long> {
    
    List<Solution> findByClaim(Claim claim);
}
