package com.sicmagroup.gpr.domain.model;

import java.time.LocalDateTime;
import java.util.List;

import com.sicmagroup.gpr.domain.enumeration.ClaimStatus;
import com.sicmagroup.gpr.domain.enumeration.Gender;

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
import jakarta.persistence.OneToMany;
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
@Table(name = "gps_suggestion")
public class Suggestion {
    
    @Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private String clientFirstAndLastName;
    @Column(unique = true)
	private String code;
     @Enumerated(EnumType.STRING)
	private Gender gender;
    private String address;
	private String tel;
	private String crew;
	private String folderCode;
    
    @ManyToOne
    @JoinColumn()
    private CollectionChannel canal;
    @ManyToOne
    @JoinColumn()
    private ServicePoint serviceIndexe;

    @ManyToOne
    @JoinColumn()
    private Product produit;


    @ManyToOne
	private Language langue;

    @Lob
    @Column(columnDefinition = "TEXT")
	private String content;

     @Enumerated(EnumType.STRING)
    private ClaimStatus status;

    private boolean accepted;

    @ManyToOne
    private User collecteur;

    @ManyToOne
    private User traiteur;

    @OneToMany
    private List<Media> files;

     @Lob
    @Column(columnDefinition = "TEXT")
	private String commentaire;


    private LocalDateTime createdAt;
    private LocalDateTime receiptDateTime;
    private LocalDateTime updatedAt;
    private LocalDateTime treatAt;
    private LocalDateTime onlineUploadDateTime;
    
}
