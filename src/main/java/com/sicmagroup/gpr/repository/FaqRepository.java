package com.sicmagroup.gpr.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sicmagroup.gpr.domain.model.Faq;

public interface FaqRepository extends JpaRepository<Faq, Long> {
    
}
