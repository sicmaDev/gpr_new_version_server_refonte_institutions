package com.sicmagroup.gpr.service.ai;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sicmagroup.gpr.domain.dto.ai.AiConversationRequestDto;
import com.sicmagroup.gpr.domain.dto.ai.AiConversationResponseDto;
import com.sicmagroup.gpr.domain.model.AiConversation;
import com.sicmagroup.gpr.repository.AiConversationRepository;

@Service
public class AiConversationService {

    private static final Logger log = LoggerFactory.getLogger(AiConversationService.class);

    private final AiConversationRepository aiConversationRepository;
    private final ObjectMapper objectMapper;

    public AiConversationService(AiConversationRepository aiConversationRepository, ObjectMapper objectMapper) {
        this.aiConversationRepository = aiConversationRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<AiConversationResponseDto> getConversations(String userId) {
        if (userId == null || userId.isBlank()) {
            return List.of();
        }
        List<AiConversation> entities = aiConversationRepository.findByUserIdOrderByUpdatedAtDesc(userId);
        return entities.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<AiConversationResponseDto> getConversation(String userId, String conversationId) {
        if (userId == null || conversationId == null) {
            return Optional.empty();
        }
        return aiConversationRepository.findByConversationIdAndUserId(conversationId, userId).map(this::toDto);
    }

    @Transactional
    public AiConversationResponseDto saveConversation(String userId, AiConversationRequestDto dto) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("Identifiant utilisateur manquant ou invalide.");
        }
        if (dto == null) {
            throw new IllegalArgumentException("Le payload de la conversation ne peut pas être vide.");
        }

        LocalDateTime now = LocalDateTime.now();
        String convId = (dto.getConversationId() != null && !dto.getConversationId().isBlank())
                ? dto.getConversationId()
                : "conv_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 5);

        String title = (dto.getTitle() != null && !dto.getTitle().isBlank())
                ? dto.getTitle().trim()
                : "Nouvelle analyse";

        Optional<AiConversation> existingOpt = aiConversationRepository.findByConversationIdAndUserId(convId, userId);
        AiConversation entity;

        if (existingOpt.isPresent()) {
            entity = existingOpt.get();
            entity.setTitle(title);
            if (dto.getThreadId() != null) entity.setThreadId(dto.getThreadId());
            if (dto.getMessages() != null) entity.setMessagesJson(serializeJson(dto.getMessages()));
            entity.setUpdatedAt(now);
            log.info("[AI-CONVERSATIONS] Action=UPDATE UserId={} ConvId={}", userId, convId);
        } else {
            // Identifiant déjà pris par une autre personne : nouvel identifiant (jamais d'écrasement).
            if (aiConversationRepository.existsById(convId)) {
                convId = "conv_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 5);
            }
            entity = AiConversation.builder()
                    .conversationId(convId)
                    .userId(userId)
                    .threadId(dto.getThreadId())
                    .title(title)
                    .messagesJson(serializeJson(dto.getMessages() != null ? dto.getMessages() : new ArrayList<>()))
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            log.info("[AI-CONVERSATIONS] Action=CREATE UserId={} ConvId={}", userId, convId);
        }

        AiConversation saved = aiConversationRepository.save(entity);
        return toDto(saved);
    }

    @Transactional
    public boolean deleteConversation(String userId, String conversationId) {
        if (userId == null || conversationId == null) return false;
        Optional<AiConversation> opt = aiConversationRepository.findByConversationIdAndUserId(conversationId, userId);
        if (opt.isPresent()) {
            aiConversationRepository.deleteByConversationIdAndUserId(conversationId, userId);
            log.info("[AI-CONVERSATIONS] Action=DELETE UserId={} ConvId={}", userId, conversationId);
            return true;
        }
        return false;
    }

    @Transactional
    public boolean clearConversations(String userId) {
        if (userId == null || userId.isBlank()) return false;
        aiConversationRepository.deleteByUserId(userId);
        log.info("[AI-CONVERSATIONS] Action=CLEAR_ALL UserId={}", userId);
        return true;
    }

    private String serializeJson(Object obj) {
        if (obj == null) return "[]";
        if (obj instanceof String) return (String) obj;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.warn("Erreur sérialisation messages conversation: {}", e.getMessage());
            return String.valueOf(obj);
        }
    }

    private Object deserializeJson(String raw) {
        if (raw == null || raw.isBlank()) return new ArrayList<>();
        try {
            return objectMapper.readValue(raw, Object.class);
        } catch (Exception e) {
            return raw;
        }
    }

    private AiConversationResponseDto toDto(AiConversation entity) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return AiConversationResponseDto.builder()
                .conversationId(entity.getConversationId())
                .userId(entity.getUserId())
                .threadId(entity.getThreadId())
                .title(entity.getTitle())
                .messages(deserializeJson(entity.getMessagesJson()))
                .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt().format(fmt) : null)
                .updatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt().format(fmt) : null)
                .build();
    }
}
