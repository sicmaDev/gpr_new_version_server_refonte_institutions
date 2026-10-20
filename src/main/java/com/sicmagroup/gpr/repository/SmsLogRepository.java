package com.sicmagroup.gpr.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sicmagroup.gpr.domain.model.SmsLog;

@Repository
public interface SmsLogRepository extends JpaRepository<SmsLog, Long> {

    List<SmsLog> findByIsDeletedFalseOrderByCreatedAtDesc();
}
