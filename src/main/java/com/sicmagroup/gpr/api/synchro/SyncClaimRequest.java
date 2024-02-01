package com.sicmagroup.gpr.api.synchro;



import java.time.LocalDateTime;

import org.springframework.web.multipart.MultipartFile;

import com.sicmagroup.gpr.domain.enumeration.ClaimStatus;
import com.sicmagroup.gpr.domain.enumeration.SatisfactionStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncClaimRequest {
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
    private ClaimStatus status;
    private Long objetId;
    private Long languageId;
    private Long collectorId;
    private String content;
    private String receiptDateTime;
    private MultipartFile[] files;
    private String createdAt;

    private String solution;
    private Long treatorId;
    private String commentaire;
    private LocalDateTime onlineUploadDateTime;
    private SatisfactionStatus satisfactionStatus;

}
