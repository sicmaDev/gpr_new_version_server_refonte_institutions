package com.sicmagroup.gpr.domain.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sicmagroup.gpr.domain.dto.claimResponse.UserResponse;
import com.sicmagroup.gpr.domain.enumeration.ClaimStatus;
import com.sicmagroup.gpr.domain.enumeration.ClaimType;
import com.sicmagroup.gpr.domain.enumeration.Gender;
import com.sicmagroup.gpr.domain.model.chat.Chat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;


//TODO: modify your package firstly
@Entity
@Data 
@AllArgsConstructor 
@NoArgsConstructor
@Builder
@Table(name = "gps_claim")
public class Claim {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
    @Column(length = 1024)
	private String clientFirstAndLastName;
    @Column(unique = true)
	private String code;
    @Column(nullable = true)
    private String codeClient;
    @Enumerated(EnumType.STRING)
	private Gender gender;
    @Enumerated(EnumType.STRING)
    private ClaimType type;
    @Column(length = 1024)
	private String address;
    @Column(length = 1024)
	private String tel;
    // Empreinte du téléphone (HMAC) pour rechercher une réclamation sans déchiffrer
    @JsonIgnore
    @Column(length = 1024)
    private String email;
	private String crew;
	private String folderCode;
    private boolean isInChatSession;

    @ManyToOne
    @JoinColumn(name = "collection_channel_id")
    private CollectionChannel collectionChannel;
    @ManyToOne
    @JoinColumn(name = "service_point_id")
    private ServicePoint servicePoint;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;
    @ManyToOne
    @JoinColumn(name = "objet_id")
    private Objet objet;

    @ManyToOne
	private Language language;

    @Lob
    @Column(columnDefinition = "MEDIUMTEXT")
	private String content;

    @ManyToOne
    private User collector;

    @ManyToOne
    private User treatmentAffectedBy;

    @ManyToOne
    private User treatBy;

    @ManyToOne
    private User treatmentAffectedTo;

    @OneToMany(fetch = FetchType.EAGER)
    private List<Solution> solutions;
    
    @OneToMany(fetch = FetchType.EAGER,mappedBy = "claim")
    private List<ExtraContent> extraContents;
    
    @Enumerated(EnumType.STRING)
    private ClaimStatus status;

    @OneToMany
    private List<Media> medias;

    @OneToMany
    private List<ClaimAudio> audios;

    @ManyToMany
    private List<ExternalRecourse> externalRecourses;

    @ManyToOne
    private User classedBy;
    private LocalDateTime createdAt;
    private LocalDateTime receiptDateTime;
    private LocalDateTime updatedAt;
    private LocalDateTime affectedAt;
    private Boolean affectedAnonymous;
    private LocalDateTime onlineUploadDateTime;
    private boolean isTransmitted;
    @ManyToOne
    private User transmittedTo;
    @ManyToOne
    private User transmittedBy;
    @Lob
    @Column(name = "transmission_comment", columnDefinition = "MEDIUMTEXT")
    private String transmissionComment;


    @Lob
    @Column(columnDefinition = "MEDIUMTEXT", name = "draft_solution", nullable = true)
    private String draftSolution;
    @Lob
    @Column(columnDefinition = "MEDIUMTEXT", name = "draft_commentaire", nullable = true)
    private String draftCommentaire;
    @Column(name = "draft_user_id", nullable = true)
    private Long draftUserId;
    @Column(name = "draft_saved_at", nullable = true)
    private LocalDateTime draftSavedAt;

    @Column(name = "is_deleted",columnDefinition = "boolean default false")
	private boolean isDeleted;
    @Column(name = "is_restored",columnDefinition = "boolean default false")
	private boolean isRestored;
    @ManyToOne
    @JoinColumn(name = "deleted_by", nullable = true)
    private User deletedBy;
    @ManyToOne
    @JoinColumn(name = "restored_by", nullable = true)
    private User restoredBy;
    @Column(name = "deleted_at", nullable = true)
    private LocalDateTime deletedAt;
    @Column(name = "restored_at", nullable = true)
    private LocalDateTime restoredAt;
    @Lob
    @Column(columnDefinition = "MEDIUMTEXT")
	private String delete_reason;


    @OneToOne()
    private Chat session;
	
    public boolean hasAffectedTreatment(){
        return treatmentAffectedBy != null;
    }

    
    private LocalDateTime convertedAt;
    
    @ManyToOne
    @JoinColumn(name = "converted_by_id")
    private User convertedBy;

    // Analyse IA à l'enregistrement (mêmes colonnes que l'ancienne version GPR).
    @Column(name = "ai_raisonnement_classification", columnDefinition = "TEXT")
    private String aiRaisonnementClassification;

    @Column(name = "ai_raisonnement_urgence", columnDefinition = "TEXT")
    private String aiRaisonnementUrgence;

    @Column(name = "ai_suggested_category")
    private String aiSuggestedCategory;

    @Column(name = "ai_suggested_motif")
    private String aiSuggestedMotif;

    @Column(name = "ai_urgency")
    private String aiUrgency;

    @Column(name = "ai_summary", columnDefinition = "TEXT")
    private String aiSummary;

    @Column(name = "ai_sentiment")
    private String aiSentiment;

    @Column(name = "ai_keywords", columnDefinition = "TEXT")
    private String aiKeywords;

    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    // Retour de l'agent sur l'IA au traitement (mêmes colonnes que l'ancienne version GPR).
    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    @Column(name = "ai_feedback_status", length = 20)
    private com.sicmagroup.gpr.domain.enumeration.AiFeedbackStatus aiFeedbackStatus;

    @Column(name = "ai_final_category")
    private String aiFinalCategory;

    @Column(name = "ai_final_motif")
    private String aiFinalMotif;

    @Column(name = "ai_final_urgency")
    private String aiFinalUrgency;

    @Column(name = "ai_retained_solution", columnDefinition = "TEXT")
    private String aiRetainedSolution;

    @Column(name = "ai_feedback_agent_id")
    private Long aiFeedbackAgentId;

    @Column(name = "ai_feedback_justification", columnDefinition = "TEXT")
    private String aiFeedbackJustification;

    @Column(name = "ai_feedback_satisfaction_status", length = 40)
    private String aiFeedbackSatisfactionStatus;

    @Column(name = "ai_feedback_date")
    private LocalDateTime aiFeedbackDate;

    @Column(name = "ai_quality_score")
    private Integer aiQualityScore;

    @Override
    public boolean equals(Object obj){
        if(this == obj)
            return true;
        if(obj == null || getClass() != obj.getClass())
            return false;

        Claim claim = (Claim) obj;

        return Objects.equals(claim.getCode(), code);
    }

    @Override
	public int hashCode() {
		return Objects.hash(code);
	}

    
}