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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sicmagroup.gpr.domain.dto.ai.UserInstructionRequestDto;
import com.sicmagroup.gpr.domain.dto.ai.UserInstructionResponseDto;
import com.sicmagroup.gpr.domain.model.UserInstruction;
import com.sicmagroup.gpr.repository.UserInstructionRepository;

@Service
public class UserInstructionService {

    private static final Logger log = LoggerFactory.getLogger(UserInstructionService.class);
    public static final int MAX_PERSONAL_RULES = 10;
    public static final int MAX_CHAR_PER_RULE = 200;

    private final UserInstructionRepository userInstructionRepository;
    private final ObjectMapper objectMapper;

    public UserInstructionService(UserInstructionRepository userInstructionRepository, ObjectMapper objectMapper) {
        this.userInstructionRepository = userInstructionRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<UserInstructionResponseDto> getUserInstructions(String userId) {
        if (userId == null || userId.isBlank()) {
            return List.of();
        }
        List<UserInstruction> entities = userInstructionRepository.findByUserIdAndIsActiveTrueOrderByCreatedAtDesc(userId);
        return entities.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public UserInstructionResponseDto saveUserInstruction(String userId, UserInstructionRequestDto dto) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("Identifiant utilisateur manquant ou invalide.");
        }
        if (dto == null || dto.getInstruction() == null || dto.getInstruction().trim().isEmpty()) {
            throw new IllegalArgumentException("Le contenu de la consigne métier ne peut pas être vide.");
        }

        String trimmedInstruction = dto.getInstruction().trim();
        if (trimmedInstruction.length() > MAX_CHAR_PER_RULE) {
            throw new IllegalArgumentException("La consigne dépasse la limite autorisée de " + MAX_CHAR_PER_RULE + " caractères.");
        }

        LocalDateTime now = LocalDateTime.now();
        UserInstruction entity;

        if (dto.getId() != null && !dto.getId().isBlank()) {
            // Modification existante (avec contrôle anti-IDOR)
            Optional<UserInstruction> existingOpt = userInstructionRepository.findByIdAndUserId(dto.getId(), userId);
            if (existingOpt.isEmpty()) {
                throw new IllegalArgumentException("Consigne introuvable ou non autorisée pour cet utilisateur.");
            }
            entity = existingOpt.get();
            entity.setInstruction(trimmedInstruction);
            entity.setQuestions(serializeQuestions(dto.getQuestions()));
            if (dto.getIsActive() != null) {
                entity.setActive(dto.getIsActive());
            }
            entity.setUpdatedAt(now);
            log.info("[AI-INSTRUCTIONS] Action=UPDATE UserId={} Id={}", userId, entity.getId());
        } else {
            // Création avec contrôle strict de quota (10 règles max actives)
            long activeCount = userInstructionRepository.countByUserIdAndIsActiveTrue(userId);
            if (activeCount >= MAX_PERSONAL_RULES) {
                throw new IllegalStateException("Quota atteint : limite de " + MAX_PERSONAL_RULES + " règles personnelles atteinte.");
            }
            String newId = "user_inst_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 5);
            entity = UserInstruction.builder()
                    .id(newId)
                    .userId(userId)
                    .instruction(trimmedInstruction)
                    .questions(serializeQuestions(dto.getQuestions()))
                    .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            log.info("[AI-INSTRUCTIONS] Action=CREATE UserId={} Id={} ActiveCount={}/{}", userId, newId, activeCount + 1, MAX_PERSONAL_RULES);
        }

        UserInstruction saved = userInstructionRepository.save(entity);
        return toDto(saved);
    }

    @Transactional
    public boolean deleteUserInstruction(String userId, String instructionId) {
        if (userId == null || userId.isBlank() || instructionId == null || instructionId.isBlank()) {
            return false;
        }
        Optional<UserInstruction> existingOpt = userInstructionRepository.findByIdAndUserId(instructionId, userId);
        if (existingOpt.isEmpty()) {
            return false;
        }
        userInstructionRepository.deleteByIdAndUserId(instructionId, userId);
        log.info("[AI-INSTRUCTIONS] Action=DELETE UserId={} Id={}", userId, instructionId);
        return true;
    }

    private String serializeQuestions(List<String> questions) {
        if (questions == null || questions.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(questions);
        } catch (Exception e) {
            log.warn("Erreur sérialisation questions instruction:", e);
            return "[]";
        }
    }

    private List<String> deserializeQuestions(String questionsJson) {
        if (questionsJson == null || questionsJson.isBlank() || questionsJson.equals("[]")) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(questionsJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of(questionsJson);
        }
    }

    private UserInstructionResponseDto toDto(UserInstruction entity) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        return UserInstructionResponseDto.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .instruction(entity.getInstruction())
                .questions(deserializeQuestions(entity.getQuestions()))
                .isPersonal(true)
                .isDefault(false)
                .isActive(entity.isActive())
                .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt().format(formatter) : null)
                .updatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt().format(formatter) : null)
                .build();
    }
}
