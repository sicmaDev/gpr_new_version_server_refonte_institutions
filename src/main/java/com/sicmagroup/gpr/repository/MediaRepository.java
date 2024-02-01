package com.sicmagroup.gpr.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sicmagroup.gpr.domain.model.Claim;
import com.sicmagroup.gpr.domain.model.Media;
import com.sicmagroup.gpr.domain.model.Suggestion;

public interface MediaRepository extends JpaRepository<Media, Long> {
    
    List<Media> findByClaim(Claim claim);

    List<Media> findBySuggestion(Suggestion suggestion);
}
