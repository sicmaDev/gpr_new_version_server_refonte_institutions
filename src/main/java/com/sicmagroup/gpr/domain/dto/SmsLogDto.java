package com.sicmagroup.gpr.domain.dto;

import java.time.LocalDateTime;

import com.sicmagroup.gpr.domain.enumeration.SmsStatus;
import com.sicmagroup.gpr.domain.enumeration.SmsType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SMS archivé tel que l'API l'expose : en plus de l'identifiant technique de la plainte ou de la suggestion liée,
 * on renvoie son code client (ex. « REC-a1b2 »), directement lisible à l'écran.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmsLogDto {
    private Long id;
    private SmsType type;
    private Long claimId;
    private String claimCode;
    private Long suggestId;
    private String suggestCode;
    private String numeroDestinataire;
    private String content;
    private SmsStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime sendAt;
    private LocalDateTime updatedAt;
    private boolean isDeleted;
}
