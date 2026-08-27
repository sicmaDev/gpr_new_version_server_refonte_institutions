package com.sicmagroup.gpr.domain.dto.wgpr;

import lombok.Data;

@Data
public class SendWithSurveyDto {
    private String to;
    private String message;
    private Long   claimId;
    private Long   solutionId;
    private Long   measurerId;
    // Note vocale facultative de l'agent présentant le sondage au client (accessibilité)
    private String surveyAudioBase64;
    private String surveyAudioMimeType;
}
