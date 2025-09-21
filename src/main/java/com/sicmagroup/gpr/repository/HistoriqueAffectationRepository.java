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