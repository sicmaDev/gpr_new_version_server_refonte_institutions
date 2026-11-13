package com.sicmagroup.gpr.domain.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "gpr_ai_conversations")
public class AiConversation {

    @Id
    @Column(name = "conversation_id", length = 64, nullable = false)
    private String conversationId;

    @Column(name = "user_id", length = 64, nullable = false)
    private String userId;

    @Column(name = "thread_id", length = 128)
    private String threadId;

    @Column(name = "title", length = 255, nullable = false)
    private String title;

    @Column(name = "messages_json", columnDefinition = "LONGTEXT", nullable = false)
    private String messagesJson;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
