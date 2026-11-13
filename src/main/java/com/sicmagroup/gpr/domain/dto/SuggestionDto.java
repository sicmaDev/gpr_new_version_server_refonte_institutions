    package com.sicmagroup.gpr.domain.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sicmagroup.gpr.domain.dto.claimResponse.CollectionChannelResponse;
import com.sicmagroup.gpr.domain.dto.claimResponse.LanguageResponse;
import com.sicmagroup.gpr.domain.dto.claimResponse.ProductResponse;
import com.sicmagroup.gpr.domain.dto.claimResponse.ServicePointResponse;
import com.sicmagroup.gpr.domain.dto.claimResponse.UserResponse;
import com.sicmagroup.gpr.domain.enumeration.ClaimStatus;
import com.sicmagroup.gpr.domain.enumeration.Gender;
import com.sicmagroup.gpr.utils.Utils;
import java.time.format.DateTimeFormatter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor 
@AllArgsConstructor
public class SuggestionDto {
    private Long id;
	private String clientFirstAndLastName;
    private String code;
    private String codeClient;
    private Gender gender;
    private String address;
	private String tel;
	private String crew;
	private String folderCode;
	private String email;
    private CollectionChannelResponse canal;
    private ServicePointResponse serviceIndexe;
    private ProductResponse produit;
    private LanguageResponse langue;
    private String content;
     private ClaimStatus status;
     private String commentaire;
    private boolean accepted;
    private UserResponse collecteur;
    private UserResponse traiteur;
    private String createdAt;
    private String receiptDateTime;
    private String updatedAt;
    private String treatAt;
    private List<ExtraContentResponse> extras;
    private String convertedAt;
    private UserResponse convertedBy;

    // Analyse IA faite à l'enregistrement (valeurs brutes du Core IA).
    private String aiSummary;
    private String aiSentiment;
    private String aiUrgency;
    private String aiKeywords;
    private String aiSuggestedCategory;
    private String aiSuggestedMotif;
    private String aiRaisonnementClassification;
    private String aiRaisonnementUrgence;
    private String correlationId;
    public String convertDate(LocalDateTime dateTime){
        
        return Utils.convertLocalDateTimeToString(dateTime);
    }   
}
