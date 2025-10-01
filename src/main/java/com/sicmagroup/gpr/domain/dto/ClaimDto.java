package com.sicmagroup.gpr.domain.dto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.sicmagroup.gpr.api.Media.MediaResponse;
import com.sicmagroup.gpr.api.claimAudio.ClaimAudioResponse;
import com.sicmagroup.gpr.domain.dto.chat.ChatDto;
import com.sicmagroup.gpr.domain.dto.claimResponse.CollectionChannelResponse;
import com.sicmagroup.gpr.domain.dto.claimResponse.ExternalRecourseResponse;
import com.sicmagroup.gpr.domain.dto.claimResponse.LanguageResponse;
import com.sicmagroup.gpr.domain.dto.claimResponse.ObjetResponse;
import com.sicmagroup.gpr.domain.dto.claimResponse.ProductResponse;
import com.sicmagroup.gpr.domain.dto.claimResponse.ServicePointResponse;
import com.sicmagroup.gpr.domain.dto.claimResponse.UserResponse;
import com.sicmagroup.gpr.domain.enumeration.ClaimStatus;
import com.sicmagroup.gpr.domain.enumeration.ClaimType;
import com.sicmagroup.gpr.domain.enumeration.Gender;
import com.sicmagroup.gpr.domain.model.ClaimAudio;
import com.sicmagroup.gpr.domain.model.ExternalRecourse;
import com.sicmagroup.gpr.domain.model.Media;
import com.sicmagroup.gpr.domain.model.User;
import com.sicmagroup.gpr.utils.Utils;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimDto {
    private Long id;
    private String code;
    private String codeClient;
    private String clientFirstAndLastName;
    private Gender gender;
    private ClaimType type;
    private String address;
    private String tel;
    private String crew;
    private String folderCode;
    private CollectionChannelResponse collectionChannel;
    private ServicePointResponse servicePoint;
    private ProductResponse product;
    private ObjetResponse objet;
    private LanguageResponse language;
    private List<SolutionDto> solutionDtos;
    private ClaimStatus status;
    private List<MediaResponse> medias;
    private List<ClaimAudioResponse> audios;
    private String content;
    private UserResponse collector;
    private UserResponse treatmentAffectedBy;
    private UserResponse treatmentAffectedTo;
    private UserResponse treatBy;
    
    private UserResponse classedBy;
    private List<ExternalRecourseResponse> externalRecourses;


    private String affectedAt;
    private String createdAt;
    private String updatedAt;

    private String receiptDateTime;
    private Boolean affectedAnonymous;
    private String onlineUploadDateTime;

    private boolean isTransmitted;
    private UserResponse transmittedTo;

    private ChatDto session;
    private String declenchedDate;
    private Long retardDay;
    private List<ExtraContentResponse> extras;

    private String convertedAt;
    private UserResponse convertedBy;

    public String convertDate(LocalDateTime dateTime){
        
        return Utils.convertLocalDateTimeToString(dateTime);
    }   

}
