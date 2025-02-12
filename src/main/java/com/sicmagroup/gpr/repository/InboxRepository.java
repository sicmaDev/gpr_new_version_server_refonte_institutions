package com.sicmagroup.gpr.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sicmagroup.gpr.domain.model.Inbox;


public interface InboxRepository extends JpaRepository<Inbox, Long> {
   Optional<Inbox> findByCode(String code);
}
