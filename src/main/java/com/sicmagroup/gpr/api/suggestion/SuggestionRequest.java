package com.sicmagroup.gpr.api.suggestion;

import java.time.LocalDateTime;
import java.util.List;

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
public class SuggestionRequest {
    private Long id;
    private String code;
    private String codeClient;
    private ClaimStatus status;
    private String clientFirstAndLastName;
    private String gender;
    private String address;
    private String phone;
    private String email;
    private String crew;
     private Boolean fromWhatsapp;
    private List<InboxMessage> filesWhatsapp;
    private Inbox inboxWhatsapp;
    private String folderCode;
    private Long collectionChannelId;
    private Long servicePointId;
    private Long productId;
    private Long objetId;
    private Long languageId;
    private String servicePointUuid;
    private String productUuid;
    private String objetUuid;
    private String languageUuid;
    private Long collectorId;
    private String content;
    private String receiptDateTime;
    private String createdAt;
    private LocalDateTime onlineUploadDateTime;

}
