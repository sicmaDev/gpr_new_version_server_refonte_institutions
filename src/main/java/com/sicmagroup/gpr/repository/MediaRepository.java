package com.sicmagroup.gpr.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

import com.sicmagroup.gpr.domain.model.Claim;
import com.sicmagroup.gpr.domain.model.Media;
import com.sicmagroup.gpr.domain.model.Suggestion;

public interface MediaRepository extends JpaRepository<Media, Long> {
    
    List<Media> findByClaim(Claim claim);
    Optional<Media> findByPath(String path);

    List<Media> findBySuggestion(Suggestion suggestion);
}
