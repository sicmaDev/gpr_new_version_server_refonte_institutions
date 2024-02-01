package com.sicmagroup.gpr.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sicmagroup.gpr.domain.model.ExternalRecourse;

public interface ExternalRecourseRepository extends JpaRepository<ExternalRecourse, Long> {
    List<ExternalRecourse> findByIsDeleted(boolean deleted);

    Optional<ExternalRecourse> findByIdAndIsDeleted(Long id, boolean deleted);
}
