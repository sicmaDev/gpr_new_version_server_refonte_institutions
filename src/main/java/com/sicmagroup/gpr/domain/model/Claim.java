package com.sicmagroup.gpr.domain.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import com.sicmagroup.gpr.domain.dto.claimResponse.UserResponse;
import com.sicmagroup.gpr.domain.enumeration.ClaimStatus;
import com.sicmagroup.gpr.domain.enumeration.ClaimType;
import com.sicmagroup.gpr.domain.enumeration.Gender;
import com.sicmagroup.gpr.domain.model.chat.Chat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;


//TODO: modify your package firstly
@Entity
@Data 
@AllArgsConstructor 
@NoArgsConstructor
@Builder
@Table(name = "gps_claim")
public class Claim {


	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private String clientFirstAndLastName;
    @Column(unique = true)
	private String code;
    @Column(nullable = true)
    private String codeClient;
    @Enumerated(EnumType.STRING)
	private Gender gender;
    @Enumerated(EnumType.STRING)
    private ClaimType type;
	private String address;
	private String tel;
	private String crew;
	private String folderCode;
    private boolean isInChatSession;

    @ManyToOne
    @JoinColumn(name = "collection_channel_id")
    private CollectionChannel collectionChannel;
    @ManyToOne
    @JoinColumn(name = "service_point_id")
    private ServicePoint servicePoint;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;
    @ManyToOne
    @JoinColumn(name = "objet_id")
    private Objet objet;

    @ManyToOne
	private Language language;

    @Lob
    @Column(columnDefinition = "TEXT")
	private String content;

    @ManyToOne
    private User collector;

    @ManyToOne
    private User treatmentAffectedBy;

    @ManyToOne
    private User treatBy;

    @ManyToOne
    private User treatmentAffectedTo;

    @OneToMany(fetch = FetchType.EAGER)
    private List<Solution> solutions;

    @Enumerated(EnumType.STRING)
    private ClaimStatus status;

    @OneToMany
    private List<Media> medias;

    @OneToMany
    private List<ClaimAudio> audios;

    @ManyToMany
    private List<ExternalRecourse> externalRecourses;

    @ManyToOne
    private User classedBy;
    private LocalDateTime createdAt;
    private LocalDateTime receiptDateTime;
    private LocalDateTime updatedAt;
    private LocalDateTime affectedAt;
    private Boolean affectedAnonymous;
    private LocalDateTime onlineUploadDateTime;
    private boolean isTransmitted;
    @ManyToOne
    private User transmittedTo;

    @OneToOne()
    private Chat session;
	
    public boolean hasAffectedTreatment(){
        return treatmentAffectedBy != null;
    }

    @Override
    public boolean equals(Object obj){
        if(this == obj)
            return true;
        if(obj == null || getClass() != obj.getClass())
            return false;

        Claim claim = (Claim) obj;

        return Objects.equals(claim.getCode(), code);
    }

    @Override
	public int hashCode() {
		return Objects.hash(code);
	}


}