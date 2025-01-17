package com.sicmagroup.gpr.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sicmagroup.gpr.domain.model.InboxMessage;


public interface InboxMessageRepository extends JpaRepository<InboxMessage, Long> {

}
