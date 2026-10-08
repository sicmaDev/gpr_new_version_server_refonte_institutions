package com.sicmagroup.gpr.sla.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sicmagroup.gpr.sla.domain.SlaBreachReason;

public interface SlaBreachReasonRepository extends JpaRepository<SlaBreachReason, Long> {
    List<SlaBreachReason> findByActiveTrue();
}