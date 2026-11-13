package com.sicmagroup.gpr.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sicmagroup.gpr.domain.model.AiConversation;

@Repository
public interface AiConversationRepository extends JpaRepository<AiConversation, String> {

    List<AiConversation> findByUserIdOrderByUpdatedAtDesc(String userId);

    Optional<AiConversation> findByConversationIdAndUserId(String conversationId, String userId);

    void deleteByConversationIdAndUserId(String conversationId, String userId);

    void deleteByUserId(String userId);
}
