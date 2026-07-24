package com.sicmagroup.gpr.domain.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data 
@AllArgsConstructor 
@NoArgsConstructor
@Builder
@Entity
@Table(name = "gps_setting")
public class Setting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true)
    private String libelle;

    @Lob
    @Column(columnDefinition = "MEDIUMTEXT")
    private String value;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}