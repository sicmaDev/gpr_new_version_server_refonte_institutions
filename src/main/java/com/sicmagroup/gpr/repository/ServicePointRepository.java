package com.sicmagroup.gpr.repository;


import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sicmagroup.gpr.domain.model.ServicePoint;
 // you need to import the entity class arccording to your context 
public interface ServicePointRepository  extends JpaRepository<ServicePoint, Long> {

    List<ServicePoint> findByIsDeleted(boolean deleted);

    @Query("SELECT sp FROM ServicePoint sp WHERE sp.direction_id = :directionId")
    List<ServicePoint> findByDirectionId(@Param("directionId") Long directionId);

    // List<ServicePoint> findByDirection_id(Long direction_id);

    Optional<ServicePoint> findByIdAndIsDeleted(Long id, boolean deleted);

    Optional<ServicePoint> findByUuid(String uuid);
}