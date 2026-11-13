package com.sicmagroup.gpr.domain.dto.ai;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExportSuggestionDto {
    private Long id;
    private String code;
    private String codeClient;
    private String clientFirstAndLastName;
    private String tel;
    private String email;
    private String content;
    private String status;
    private String canal;
    private String serviceIndexe;
    private String produit;
    private String commentaire;
    private String aiSummary;
    private String aiSentiment;
    private String aiUrgency;
    private String impactLevel;
    private String origine;
    private Long objetId;
    private Long productId;
    private Long servicePointId;
    private Long collectionChannelId;
    private Long collectorId;
    private LocalDateTime createdAt;
    private LocalDateTime receiptDateTime;
    private LocalDateTime updatedAt;
    private LocalDateTime treatAt;
}
