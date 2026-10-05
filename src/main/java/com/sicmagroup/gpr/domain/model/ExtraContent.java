package com.sicmagroup.gpr.domain.model;

import com.sicmagroup.gpr.domain.converter.EncryptedStringConverter;
import java.time.LocalDateTime;

import com.sicmagroup.gpr.domain.enumeration.ClaimStatus;
import com.sicmagroup.gpr.domain.enumeration.ClaimType;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "gps_extra_content")
public class ExtraContent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Lob
    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "contenu", columnDefinition = "MEDIUMTEXT")
    private String contenu;
    
    @Enumerated(EnumType.STRING)
    private ClaimStatus status;
    
    @Enumerated(EnumType.STRING)
    private ClaimType type;

    @Column(name = "is_file", columnDefinition = "boolean default false")
    private boolean file;

    @ManyToOne
    private Claim claim;

    @ManyToOne
    private Suggestion suggestion;

    @ManyToOne
    private User user;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
