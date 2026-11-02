package com.sicmagroup.gpr.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sicmagroup.gpr.domain.model.ServicePointLocalisation;

public interface ServicePointLocalisationRepository extends JpaRepository<ServicePointLocalisation, Long> {
}
