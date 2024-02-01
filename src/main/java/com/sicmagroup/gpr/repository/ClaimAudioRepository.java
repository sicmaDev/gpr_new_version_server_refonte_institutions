package com.sicmagroup.gpr.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sicmagroup.gpr.domain.model.Claim;
import com.sicmagroup.gpr.domain.model.ClaimAudio;
import com.sicmagroup.gpr.domain.model.Suggestion;

public interface ClaimAudioRepository extends JpaRepository<ClaimAudio, Long>{
     List<ClaimAudio> findByClaim(Claim claim);

    List<ClaimAudio> findBySuggestion(Suggestion suggestion);
}
