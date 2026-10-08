package com.sicmagroup.gpr.sla.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sicmagroup.gpr.sla.domain.SlaEvent;

public interface SlaEventRepository extends JpaRepository<SlaEvent, Long> {

    boolean existsByDedupKey(String dedupKey);

    /** Rappels en attente d'envoi dans le récapitulatif quotidien. */
    List<SlaEvent> findByDigestPendingTrueOrderByRecipientUserIdAscCreatedAtAsc();

    List<SlaEvent> findByClaimIdOrderByCreatedAtAsc(Long claimId);
}
