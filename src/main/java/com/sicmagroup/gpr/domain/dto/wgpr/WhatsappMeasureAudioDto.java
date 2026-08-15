package com.sicmagroup.gpr.domain.dto.wgpr;

import lombok.Data;

@Data
public class WhatsappMeasureAudioDto {
    private Long   claimId;
    private Long   solutionId;
    private Long   measurerId;
    private String satisfactionStatus; // SATISFIED | PARTIAL | UNSATISFIED
    private String audioBase64;        // audio encodé en base64 (sans préfixe data:...)
    private String mimeType;           // ex: "audio/ogg; codecs=opus"
}
