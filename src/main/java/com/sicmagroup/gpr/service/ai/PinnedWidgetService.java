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
import com.sicmagroup.gpr.domain.dto.ai.PinnedWidgetRequestDto;
import com.sicmagroup.gpr.domain.dto.ai.PinnedWidgetResponseDto;
import com.sicmagroup.gpr.domain.model.PinnedWidget;
import com.sicmagroup.gpr.repository.PinnedWidgetRepository;

@Service
public class PinnedWidgetService {

    private static final Logger log = LoggerFactory.getLogger(PinnedWidgetService.class);

    private final PinnedWidgetRepository pinnedWidgetRepository;
    private final ObjectMapper objectMapper;

    public PinnedWidgetService(PinnedWidgetRepository pinnedWidgetRepository, ObjectMapper objectMapper) {
        this.pinnedWidgetRepository = pinnedWidgetRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<PinnedWidgetResponseDto> getWidgets(String userId) {
        if (userId == null || userId.isBlank()) {
            return List.of();
        }
        List<PinnedWidget> entities = pinnedWidgetRepository.findByUserIdOrderByDisplayOrderAsc(userId);
        return entities.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public PinnedWidgetResponseDto saveWidget(String userId, PinnedWidgetRequestDto dto) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("Identifiant utilisateur manquant ou invalide.");
        }
        if (dto == null || dto.getTitle() == null || dto.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Le titre du widget ne peut pas être vide.");
        }

        LocalDateTime now = LocalDateTime.now();
        String widgetId = (dto.getId() != null && !dto.getId().isBlank())
                ? dto.getId()
                : "widget_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 5);

        Optional<PinnedWidget> existingOpt = pinnedWidgetRepository.findByIdAndUserId(widgetId, userId);
        PinnedWidget entity;

        if (existingOpt.isPresent()) {
            entity = existingOpt.get();
            entity.setTitle(dto.getTitle().trim());
            if (dto.getDescription() != null) entity.setDescription(dto.getDescription());
            if (dto.getChartType() != null) entity.setChartType(dto.getChartType());
            if (dto.getSqlQuery() != null) entity.setSqlQuery(dto.getSqlQuery());
            if (dto.getData() != null) entity.setDataJson(serializeJson(dto.getData()));
            if (dto.getKpis() != null) entity.setKpisJson(serializeJson(dto.getKpis()));
            if (dto.getSummary() != null) entity.setSummary(dto.getSummary());
            if (dto.getDisplayOrder() != null) entity.setDisplayOrder(dto.getDisplayOrder());
            entity.setUpdatedAt(now);
            log.info("[AI-PINBOARD] Action=UPDATE UserId={} WidgetId={}", userId, widgetId);
        } else {
            // Identifiant déjà pris par une autre personne : nouvel identifiant (jamais d'écrasement).
            if (pinnedWidgetRepository.existsById(widgetId)) {
                widgetId = "widget_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 5);
            }
            int nextOrder = 0;
            List<PinnedWidget> current = pinnedWidgetRepository.findByUserIdOrderByDisplayOrderAsc(userId);
            if (!current.isEmpty()) {
                nextOrder = current.get(current.size() - 1).getDisplayOrder() + 1;
            }
            if (dto.getDisplayOrder() != null) {
                nextOrder = dto.getDisplayOrder();
            }

            entity = PinnedWidget.builder()
                    .id(widgetId)
                    .userId(userId)
                    .title(dto.getTitle().trim())
                    .description(dto.getDescription())
                    .chartType(dto.getChartType() != null ? dto.getChartType() : "bar")
                    .sqlQuery(dto.getSqlQuery())
                    .dataJson(serializeJson(dto.getData()))
                    .kpisJson(serializeJson(dto.getKpis()))
                    .summary(dto.getSummary())
                    .displayOrder(nextOrder)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            log.info("[AI-PINBOARD] Action=CREATE UserId={} WidgetId={}", userId, widgetId);
        }

        PinnedWidget saved = pinnedWidgetRepository.save(entity);
        return toDto(saved);
    }

    @Transactional
    public boolean deleteWidget(String userId, String widgetId) {
        if (userId == null || widgetId == null) return false;
        Optional<PinnedWidget> opt = pinnedWidgetRepository.findByIdAndUserId(widgetId, userId);
        if (opt.isPresent()) {
            pinnedWidgetRepository.deleteByIdAndUserId(widgetId, userId);
            log.info("[AI-PINBOARD] Action=DELETE UserId={} WidgetId={}", userId, widgetId);
            return true;
        }
        return false;
    }

    @Transactional
    public boolean clearWidgets(String userId) {
        if (userId == null || userId.isBlank()) return false;
        pinnedWidgetRepository.deleteByUserId(userId);
        log.info("[AI-PINBOARD] Action=CLEAR_ALL UserId={}", userId);
        return true;
    }

    @Transactional
    public boolean reorderWidgets(String userId, List<String> widgetIds) {
        if (userId == null || widgetIds == null || widgetIds.isEmpty()) return false;
        for (int i = 0; i < widgetIds.size(); i++) {
            String wId = widgetIds.get(i);
            Optional<PinnedWidget> opt = pinnedWidgetRepository.findByIdAndUserId(wId, userId);
            if (opt.isPresent()) {
                PinnedWidget w = opt.get();
                w.setDisplayOrder(i);
                pinnedWidgetRepository.save(w);
            }
        }
        log.info("[AI-PINBOARD] Action=REORDER UserId={} Count={}", userId, widgetIds.size());
        return true;
    }

    private String serializeJson(Object obj) {
        if (obj == null) return null;
        if (obj instanceof String) return (String) obj;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.warn("Erreur sérialisation JSON widget: {}", e.getMessage());
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

    private PinnedWidgetResponseDto toDto(PinnedWidget entity) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return PinnedWidgetResponseDto.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .chartType(entity.getChartType())
                .sqlQuery(entity.getSqlQuery())
                .data(deserializeJson(entity.getDataJson()))
                .kpis(deserializeJson(entity.getKpisJson()))
                .summary(entity.getSummary())
                .displayOrder(entity.getDisplayOrder())
                .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt().format(fmt) : null)
                .updatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt().format(fmt) : null)
                .build();
    }
}
