package com.sicmagroup.gpr.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sicmagroup.gpr.domain.enumeration.LogTarget;
import com.sicmagroup.gpr.domain.model.Log;

public interface LogRepository extends JpaRepository<Log, Long> {
    List<Log> findByTargetOrderByCreatedAtDesc(LogTarget target);
}
