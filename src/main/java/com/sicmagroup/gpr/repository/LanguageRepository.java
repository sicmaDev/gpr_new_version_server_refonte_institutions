package com.sicmagroup.gpr.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sicmagroup.gpr.domain.model.Language;

public interface LanguageRepository extends JpaRepository<Language, Long> {
    List<Language> findByIsDeleted(boolean deleted);

    Optional<Language> findByIdAndIsDeleted(Long id, boolean deleted);
}
