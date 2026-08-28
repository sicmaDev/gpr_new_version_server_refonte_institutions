package com.sicmagroup.gpr.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.sicmagroup.gpr.domain.model.Claim;
import com.sicmagroup.gpr.domain.model.ClaimAudio;
import com.sicmagroup.gpr.domain.model.Suggestion;

public interface ClaimAudioRepository extends JpaRepository<ClaimAudio, Long>{
     List<ClaimAudio> findByClaim(Claim claim);

    Optional<ClaimAudio> findByName(String name);

    List<ClaimAudio> findBySuggestion(Suggestion suggestion);

    @Query(value = "SELECT CASE WHEN COUNT(*) > 0 THEN TRUE ELSE FALSE END " +
        "FROM gps_claim_audio " +
        "WHERE claim_id = :claimId", nativeQuery = true)
    boolean existsByClaimId(@Param("claimId") Long claimId);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM gps_claim_audio WHERE claim_id = :claimId", nativeQuery = true)
    void deleteByClaimId(@Param("claimId") Long claimId);
    
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM gps_claim_audio WHERE suggestion_id = :suggestionId", nativeQuery = true)
    void deleteBySuggestionId(@Param("suggestionId") Long suggestionId);

    @Query("SELECT c FROM ClaimAudio c WHERE c.claim.id = :claimId")
    List<ClaimAudio> findByClaimId(@Param("claimId") Long claimId);
}
