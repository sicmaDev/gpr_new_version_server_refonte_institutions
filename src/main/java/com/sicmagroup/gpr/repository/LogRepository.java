package com.sicmagroup.gpr.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sicmagroup.gpr.domain.model.Log;

public interface LogRepository extends JpaRepository<Log, Long> {
    
}
