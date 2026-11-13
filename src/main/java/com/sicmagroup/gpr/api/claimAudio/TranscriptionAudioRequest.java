package com.sicmagroup.gpr.api.claimAudio;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Transcription IA d'un audio à enregistrer (texte renvoyé par le Core IA, éventuellement corrigé). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TranscriptionAudioRequest {
    private String transcriptionRaw;
    private String transcriptionCorrected;
    private String transcriptionModel;
    private String transcriptionStatus;
    private String transcriptionLanguage;
    private Double transcriptionDuration;
    private String segmentsJson;
    private String speakersJson;
    private String transcriptionError;
    /** Vrai quand l'agent a ajouté le texte au contenu du dossier. */
    private Boolean transcriptionAdded;
}
