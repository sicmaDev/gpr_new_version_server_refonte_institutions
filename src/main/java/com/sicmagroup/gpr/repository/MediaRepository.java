package com.sicmagroup.gpr.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;


import java.util.Optional;

import com.sicmagroup.gpr.domain.model.Claim;
import com.sicmagroup.gpr.domain.model.Media;
import com.sicmagroup.gpr.domain.model.Suggestion;

public interface MediaRepository extends JpaRepository<Media, Long> {
    
    List<Media> findByClaim(Claim claim);
    Optional<Media> findByPath(String path);
    Optional<Media> findByName(String name);

    List<Media> findBySuggestion(Suggestion suggestion);

    @Query(value = "SELECT CASE WHEN COUNT(*) > 0 THEN TRUE ELSE FALSE END " +
        "FROM gps_media " +
        "WHERE claim_id = :claimId", nativeQuery = true)
    boolean existsByClaimId(@Param("claimId") Long claimId);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM gps_media WHERE claim_id = :claimId", nativeQuery = true)
    void deleteByClaimId(@Param("claimId") Long claimId);
}
