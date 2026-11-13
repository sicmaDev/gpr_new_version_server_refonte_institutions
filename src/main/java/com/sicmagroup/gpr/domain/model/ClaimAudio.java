package com.sicmagroup.gpr.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data 
@AllArgsConstructor 
@NoArgsConstructor
@Builder
@Table(name = "gps_claim_audio")
public class ClaimAudio {
        @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Lob
    @Column(name = "name", columnDefinition = "TEXT")
    private String name;

    @Lob
    @Column(name = "path", columnDefinition = "TEXT")
    private String path;

    private Long size;

    @Column(name="is_extra",columnDefinition = "boolean default false")
    private boolean is_extra;

    // Transcription IA de l'audio (mêmes colonnes que l'ancienne version GPR).
    @Column(name = "transcription_added", columnDefinition = "boolean default false")
    private boolean transcriptionAdded;

    @Column(name = "transcription_raw", columnDefinition = "LONGTEXT")
    private String transcriptionRaw;

    @Column(name = "transcription_corrected", columnDefinition = "LONGTEXT")
    private String transcriptionCorrected;

    @Column(name = "segments_json", columnDefinition = "LONGTEXT")
    private String segmentsJson;

    @Column(name = "speakers_json", columnDefinition = "LONGTEXT")
    private String speakersJson;

    @Column(name = "transcription_language", length = 20)
    private String transcriptionLanguage;

    @Column(name = "transcription_duration")
    private Double transcriptionDuration;

    @Column(name = "transcription_status", length = 40)
    private String transcriptionStatus;

    @Column(name = "transcription_error", columnDefinition = "TEXT")
    private String transcriptionError;

    @Column(name = "transcription_model", length = 150)
    private String transcriptionModel;

    @Column(name = "transcription_created_at")
    private java.time.Instant transcriptionCreatedAt;

    @Column(name = "transcription_updated_at")
    private java.time.Instant transcriptionUpdatedAt;

    @ManyToOne
    private ExtraContent extraContent;

    @ManyToOne
    private Claim claim;

    @ManyToOne
    private Suggestion suggestion;
}
