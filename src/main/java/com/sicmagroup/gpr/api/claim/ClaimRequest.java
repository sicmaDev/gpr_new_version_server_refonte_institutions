package com.sicmagroup.gpr.api.claim;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.sicmagroup.gpr.domain.enumeration.ClaimStatus;
import com.sicmagroup.gpr.domain.model.Inbox;
import com.sicmagroup.gpr.domain.model.InboxMessage;

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
    private Boolean fromWhatsapp;
    private List<InboxMessage> filesWhatsapp;
    private Inbox inboxWhatsapp;
    private String folderCode;
    private Long collectionChannelId;
    private Long servicePointId;
    private Long productId;
    private Long objetId;
    private ClaimStatus status;
    private Long languageId;
    private Long collectorId;
    private String content;
    private String receiptDateTime;
    private String createdAt;
    private LocalDateTime onlineUploadDateTime;
    private String servicePointUuid;
    private String productUuid;
    private String objetUuid;
    private String languageUuid;

}
