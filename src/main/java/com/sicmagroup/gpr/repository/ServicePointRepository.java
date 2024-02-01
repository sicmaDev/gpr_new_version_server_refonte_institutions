package com.sicmagroup.gpr.repository;


import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sicmagroup.gpr.domain.model.ServicePoint;
 // you need to import the entity class arccording to your context 
public interface ServicePointRepository  extends JpaRepository<ServicePoint, Long> {

    List<ServicePoint> findByIsDeleted(boolean deleted);

    Optional<ServicePoint> findByIdAndIsDeleted(Long id, boolean deleted);

    Optional<ServicePoint> findByUuid(String uuid);
}