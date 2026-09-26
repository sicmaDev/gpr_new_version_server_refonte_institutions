package com.sicmagroup.gpr.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sicmagroup.gpr.domain.model.HistoriqueTransmission;

@Repository
public interface HistoriqueTransmissionRepository extends JpaRepository<HistoriqueTransmission, Long> {

    List<HistoriqueTransmission> findByClaimIdOrderByDateTransmissionDesc(Long claimId);
}
