package com.sicmagroup.gpr.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.sicmagroup.gpr.domain.model.Claim;
import com.sicmagroup.gpr.domain.model.ExtraContent;
import com.sicmagroup.gpr.domain.model.Suggestion;
import com.sicmagroup.gpr.domain.model.User;

public interface ExtraContentRepository extends JpaRepository<ExtraContent, Long> {
    
    List<ExtraContent> findByClaim(Claim claim);
    
    List<ExtraContent> findBySuggestion(Suggestion suggestion);

    List<ExtraContent> findByUser(User user);    

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM gps_extra_content WHERE claim_id = :claimId", nativeQuery = true)
    void deleteByClaimId(@Param("claimId") Long claimId);
}