package com.sicmagroup.gpr.api.denunciation;

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
public class DenunRequest {
    private Long id;
    private String code;
    private Long collectionChannelId;
    private String servicePointUuid;
    private String productUuid;
    private String objetUuid;
    private String languageUuid;
     private Boolean fromWhatsapp;
    private List<InboxMessage> filesWhatsapp;
    private Inbox inboxWhatsapp;
    private Long servicePointId;
    private Long productId;
    private Long objetId;
    private Long languageId;
    private Long collectorId;
    private String content;
    private ClaimStatus status;
    private String receiptDateTime;
    private String createdAt;
    private LocalDateTime onlineUploadDateTime;

}
