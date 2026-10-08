package com.sicmagroup.gpr.sla.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sicmagroup.gpr.sla.domain.Holiday;

public interface HolidayRepository extends JpaRepository<Holiday, Long> {
}