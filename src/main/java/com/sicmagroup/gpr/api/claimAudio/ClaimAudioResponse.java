package com.sicmagroup.gpr.api.claimAudio;

import java.net.URL;

import org.springframework.core.io.Resource;

import com.sicmagroup.gpr.domain.dto.ExtraContentResponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ClaimAudioResponse
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class ClaimAudioResponse {
    private String name;
    private Long id;
    private Long size;
    private boolean is_extra;
    private ExtraContentResponse extra;
    private String path;
    private byte[] data;

    // Transcription IA enregistrée pour cet audio (vide si l'audio n'a pas été transcrit).
    private boolean transcriptionAdded;
    private String transcriptionRaw;
    private String transcriptionCorrected;
    private String transcriptionStatus;
    private String transcriptionModel;
    private String transcriptionLanguage;
    private Double transcriptionDuration;
    private String segmentsJson;
    
}