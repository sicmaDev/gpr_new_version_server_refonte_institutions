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
@Builder
@NoArgsConstructor 
@AllArgsConstructor
@Entity
@Table(name = "gps_faq")
public class Faq {
    @Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Lob
    @Column(columnDefinition = "TEXT")
    private String libelle;
    @Lob
    @Column(columnDefinition = "TEXT")
    private String answer;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
