package com.sicmagroup.gpr.repository.chat;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sicmagroup.gpr.domain.model.chat.Vote;

public interface VoteRepository extends JpaRepository<Vote, Long> {
    
}
