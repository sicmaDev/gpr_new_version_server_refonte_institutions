package com.sicmagroup.gpr.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sicmagroup.gpr.domain.model.HistoriqueAffectation;

@Repository
public interface HistoriqueAffectationRepository extends JpaRepository<HistoriqueAffectation, Long> {

    // Export pour l'IA (adresses /api/v1/ai/export-*), repris de l'ancienne version GPR.
    @Query("SELECT h FROM HistoriqueAffectation h " +
           "WHERE (:updatedAfter IS NULL OR h.updatedAt > :updatedAfter) " +
           "AND (:updatedBefore IS NULL OR h.updatedAt <= :updatedBefore) " +
           "ORDER BY h.updatedAt ASC, h.id ASC")
    org.springframework.data.domain.Page<HistoriqueAffectation> findReportingAffectations(
            @Param("updatedAfter") java.time.LocalDateTime updatedAfter,
            @Param("updatedBefore") java.time.LocalDateTime updatedBefore,
            org.springframework.data.domain.Pageable pageable);

    
    List<HistoriqueAffectation> findByReclamationIdOrderByDateAffectationDesc(Long claimId);
    
  
    @Query("SELECT h FROM HistoriqueAffectation h WHERE h.dateFinAffectation IS NULL " +
           "AND DATEADD(DAY, h.delaiJours, h.dateAffectation) < :currentDate")
    List<HistoriqueAffectation> findRetardAffectations(@Param("currentDate") LocalDateTime currentDate);
    
   
    @Query("SELECT h FROM HistoriqueAffectation h WHERE h.dateFinAffectation IS NULL " +
           "AND DATEADD(DAY, h.delaiJours, h.dateAffectation) BETWEEN :currentDate AND :endDate")
    List<HistoriqueAffectation> findSoonToRetardAffectations(
        @Param("currentDate") LocalDateTime currentDate, 
        @Param("endDate") LocalDateTime endDate);
    
  
    List<HistoriqueAffectation> findByEmailAgentOrderByDateAffectationDesc(String emailAgent);
    
  
    List<HistoriqueAffectation> findByCodePlainteOrderByDateAffectationDesc(String codePlainte);
    
    @Query("SELECT h FROM HistoriqueAffectation h WHERE h.dateFinAffectation IS NULL " +
           "AND (h.lastNotification IS NULL OR h.lastNotification < :dateLimit)")
    List<HistoriqueAffectation> findAffectationsToNotify(@Param("dateLimit") LocalDateTime dateLimit);
}