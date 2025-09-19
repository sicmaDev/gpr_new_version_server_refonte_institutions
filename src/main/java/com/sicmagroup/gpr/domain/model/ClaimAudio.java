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

    @ManyToOne
    private ExtraContent extraContent;

    @ManyToOne
    private Claim claim;

    @ManyToOne
    private Suggestion suggestion;
}
