package com.sicmagroup.gpr.domain.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

import java.util.List;
import com.sicmagroup.gpr.domain.enumeration.AiFeedbackStatus;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExportClaimDto {
    private Long id;
    private String code;
    private String codeClient;
    private String claimType;
    private String objet_categorie;
    private String statut_final;
    private String texte_plainte;
    private String texte_solution;
    private String modalite_depot;
    private String motif_reclamation;
    private String produit_service;
    private String point_service_indexe;
    private LocalDateTime date_creation;
    private LocalDateTime receiptDateTime;
    private LocalDateTime slaDueAt;
    private LocalDateTime updatedAt;
    private LocalDateTime affectedAt;
    private LocalDateTime resolvedAt;
    private Long objetId;
    private Long productId;
    private Long servicePointId;
    private Long collectionChannelId;
    private Long collectorId;
    private String aiUrgency;
    private String aiSentiment;
    private String aiSuggestedCategory;
    private String aiSuggestedMotif;
    private String aiSummary;
    private String agence;
    private String equipe;
    private String riskLevel;
    private AiFeedbackStatus aiFeedbackStatus;
    private String aiFinalCategory;
    private String aiFinalMotif;
    private String aiFinalUrgency;
    private String aiRetainedSolution;
    private Long aiFeedbackAgentId;
    private String aiFeedbackSatisfactionStatus;
    private LocalDateTime aiFeedbackDate;
    private Integer aiQualityScore;
    
    private List<MediaDto> medias;
    private List<AudioDto> audios;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MediaDto {
        private Long id;
        private String name;
        private String path;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AudioDto {
        private Long id;
        private String name;
        private String path;
    }
}
