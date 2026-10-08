package com.sicmagroup.gpr.sla.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sicmagroup.gpr.sla.domain.ContactAttempt;

public interface ContactAttemptRepository extends JpaRepository<ContactAttempt, Long> {
    List<ContactAttempt> findByClaimIdOrderByCreatedAtDesc(Long claimId);

    long countByClaimIdAndCreatedAtAfter(Long claimId, LocalDateTime after);

    /** Tentatives sans succès (client non joint) depuis une date. */
    long countByClaimIdAndReachedFalseAndCreatedAtAfter(Long claimId, LocalDateTime after);
}