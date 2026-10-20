package com.sicmagroup.gpr.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sicmagroup.gpr.domain.enumeration.ReportRecipientType;
import com.sicmagroup.gpr.domain.model.ScheduledReportConfig;

@Repository
public interface ScheduledReportConfigRepository extends JpaRepository<ScheduledReportConfig, Long> {

    Optional<ScheduledReportConfig> findByRecipientType(ReportRecipientType recipientType);
}
