package com.sicmagroup.gpr.domain.model;

import java.time.LocalDateTime;

import com.sicmagroup.gpr.domain.enumeration.SatisfactionStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
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
@Table(name = "gps_satisfaction_measure")
public class SatisfactionMeasure {
    
    @Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING)
    private SatisfactionStatus status;
    @OneToOne(mappedBy = "satisfactionMeasure")
    private Solution solution;

    @ManyToOne
    private User measurer;
    @Lob
    @Column(columnDefinition = "TEXT")
    private String commentaire;

    private LocalDateTime measureDateTime;

    

}
