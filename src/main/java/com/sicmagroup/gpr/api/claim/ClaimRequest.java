package com.sicmagroup.gpr.api.claim;

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
public class ClaimRequest {
    private Long id;
    private String code;
    private String clientFirstAndLastName;
    private String gender;
    private String address;
    private String phone;
    private String crew;
    private String folderCode;
    private Long collectionChannelId;
    private String servicePointUuid;
    private String productUuid;
    private String objetUuid;
    private ClaimStatus status;
    private String languageUuid;
    private Long collectorId;
    private String content;
    private String receiptDateTime;
    private String createdAt;
    private LocalDateTime onlineUploadDateTime;

}
