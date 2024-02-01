package com.sicmagroup.gpr.repository.chat;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sicmagroup.gpr.domain.model.User;
import com.sicmagroup.gpr.domain.model.chat.UserVote;
import com.sicmagroup.gpr.domain.model.chat.Vote;

public interface UserVoteRepository extends JpaRepository<UserVote, Long> {
    
    Optional<UserVote> findByUserAndVote(User user, Vote vote);
}
