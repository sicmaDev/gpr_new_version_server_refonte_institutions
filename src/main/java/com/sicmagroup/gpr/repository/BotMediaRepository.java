package com.sicmagroup.gpr.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sicmagroup.gpr.domain.model.BotMedia;
import com.sicmagroup.gpr.domain.model.Claim;
import com.sicmagroup.gpr.domain.model.Suggestion;

public interface BotMediaRepository extends JpaRepository<BotMedia, Long>{
    List<BotMedia> findByClaim(Claim claim);

    List<BotMedia> findBySuggestion(Suggestion suggestion);
}
