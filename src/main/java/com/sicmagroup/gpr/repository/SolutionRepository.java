package com.sicmagroup.gpr.repository;

import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;

import com.sicmagroup.gpr.domain.model.Claim;
import com.sicmagroup.gpr.domain.model.Solution;
import java.util.List;


public interface SolutionRepository extends JpaRepository<Solution, Long> {

    // Export pour l'IA (adresses /api/v1/ai/export-*), repris de l'ancienne version GPR.
    @Query("SELECT s FROM Solution s " +
           "WHERE (:updatedAfter IS NULL OR COALESCE(s.updatedAt, s.createdAt) > :updatedAfter) " +
           "AND (:updatedBefore IS NULL OR COALESCE(s.updatedAt, s.createdAt) <= :updatedBefore) " +
           "ORDER BY COALESCE(s.updatedAt, s.createdAt) ASC, s.id ASC")
    org.springframework.data.domain.Page<Solution> findReportingSolutions(
            @Param("updatedAfter") java.time.LocalDateTime updatedAfter,
            @Param("updatedBefore") java.time.LocalDateTime updatedBefore,
            org.springframework.data.domain.Pageable pageable);

    
    List<Solution> findByClaim(Claim claim);
}
