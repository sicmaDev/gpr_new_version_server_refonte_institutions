package com.sicmagroup.gpr.api.synchro;

import java.time.LocalDateTime;

import org.springframework.web.multipart.MultipartFile;

import com.sicmagroup.gpr.domain.enumeration.ClaimStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncSuggestionRequest {
    
    private Long id;
    private String code;
    private String clientFirstAndLastName;
    private String gender;
    private String address;
    private String phone;
    private String crew;
    private String folderCode;
    private Long collectionChannelId;
    private Long servicePointId;
    private Long productId;
    private Long objetId;
    private Long languageId;
    private Long collectorId;
    private String content;
    private String receiptDateTime;
    private String createdAt;
    private ClaimStatus status;
    private LocalDateTime onlineUploadDateTime;
    private MultipartFile[] files;

    private Long treatorId;
    private boolean accepted;
    private String commentaire;
}
