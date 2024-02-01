package com.sicmagroup.gpr.api.denunciation;

import java.time.LocalDateTime;

import com.sicmagroup.gpr.domain.enumeration.ClaimStatus;

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
