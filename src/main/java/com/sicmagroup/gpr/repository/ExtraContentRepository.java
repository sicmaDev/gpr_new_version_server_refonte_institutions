package com.sicmagroup.gpr.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sicmagroup.gpr.domain.model.Claim;
import com.sicmagroup.gpr.domain.model.ExtraContent;
import com.sicmagroup.gpr.domain.model.Suggestion;
import com.sicmagroup.gpr.domain.model.User;

public interface ExtraContentRepository extends JpaRepository<ExtraContent, Long> {
    
    List<ExtraContent> findByClaim(Claim claim);
    
    List<ExtraContent> findBySuggestion(Suggestion suggestion);

    List<ExtraContent> findByUser(User user);    
}