package com.sicmagroup.gpr.domain.model;

import java.time.LocalDateTime;

import com.sicmagroup.gpr.domain.enumeration.SolutionStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
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
@Table(name = "gps_solution")
public class Solution {
    
    @Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String content;
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "measure_id", referencedColumnName = "id")
    private SatisfactionMeasure satisfactionMeasure;
    @ManyToOne
    private Claim claim;

    @ManyToOne
    private User author;

    @Enumerated(EnumType.STRING)
    private SolutionStatus status;
    @Lob
    @Column(columnDefinition = "TEXT")
    private String commentaire;
    @Lob
    @Column(columnDefinition = "TEXT")
    private String motifDesaprobation;

    @ManyToOne
    private User approuver;

    private LocalDateTime approuvedAt;

    @ManyToOne
    private User unApprouver;

    private LocalDateTime unApprouvedAt;


    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @OneToOne(mappedBy = "solution")
    private ExistingSolution existingSolution;
}
