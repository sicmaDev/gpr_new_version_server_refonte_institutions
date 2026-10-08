package com.sicmagroup.gpr.sla.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sicmagroup.gpr.sla.domain.BusinessDay;

public interface BusinessDayRepository extends JpaRepository<BusinessDay, Long> {
}