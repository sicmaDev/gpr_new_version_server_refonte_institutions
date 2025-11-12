package com.sicmagroup.gpr.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sicmagroup.gpr.domain.enumeration.Habilitation;
import com.sicmagroup.gpr.domain.enumeration.Role;
import com.sicmagroup.gpr.domain.model.chat.Chat;
import com.sicmagroup.gpr.domain.model.chat.Message;
import com.sicmagroup.gpr.domain.model.chat.UserVote;
import com.sicmagroup.gpr.domain.model.chat.Vote;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "gps_user")
public class User implements UserDetails {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@Column(unique = true)
	private String code;
	private String firstandlastname;
	@Column(unique = true)
	private String email;
	private String password;
	private String tel;
	@Enumerated(EnumType.STRING)
	private Role additionalrole;

	private String habilitationUp;

	@ManyToOne
	@JoinColumn(name = "service_point_id")
	private ServicePoint servicePoint;

	@ManyToOne
	@JoinColumn(name = "poste_id")
	private Poste poste;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	private LocalDateTime deletedAt;

	@Column(columnDefinition = "boolean default false")
	private boolean isDeleted;
	@Column(columnDefinition = "boolean default false")
	private boolean isRattached;

	@OneToMany(mappedBy = "collector")
	private List<Claim> claimsCollect;

	@OneToMany(mappedBy = "treatmentAffectedBy")
	private List<Claim> claimsAffectedTreatment;

	@OneToMany(mappedBy = "treatBy")
	private List<Claim> claimsTreat;

	@OneToMany(mappedBy = "treatmentAffectedTo")
	private List<Claim> claimsReceivedTreatment;

	@OneToMany(mappedBy = "author")
	private List<Solution> proposedSolutions;

	@OneToMany(mappedBy = "measurer")
	private List<SatisfactionMeasure> satisfactionsMeasure;

	@OneToMany(mappedBy = "classedBy")
	private List<Claim> classedClaims;

	@OneToMany(mappedBy = "transmittedTo")
	private List<Claim> transmittedTo;

	@OneToMany(mappedBy = "transmittedBy")
	private List<Claim> transmittedBy;

	@OneToMany(mappedBy = "approuver")
	private List<Solution> approuvedSolutions;

	@OneToMany(mappedBy = "unApprouver")
	private List<Solution> unapprouvedSolutions;

	@OneToMany(mappedBy = "traiteur")
	private List<Suggestion> suggestionsTreat;

	@OneToMany(mappedBy = "collecteur")
	private List<Suggestion> suggestionsCollect;

	@OneToMany(mappedBy = "user")
	private List<Documentation> documentations;

	@OneToMany(mappedBy = "createdBy")
	@JsonIgnore
	private List<Chat> chatsCreated;

	@ManyToMany
	@JsonIgnore
	private List<Chat> chatsMember;
	@ManyToMany(fetch = FetchType.EAGER)
	@JsonIgnore
	private List<Chat> chatsGuest;

	private boolean isEmailReceiver;
	private boolean isCoodonateur;
	private boolean isRa;
	private String titre;

	@OneToMany
	@JsonIgnore
	private List<Message> messages;

	@OneToMany
	@JsonIgnore
	private List<Vote> vote;

	@OneToMany(mappedBy = "user")
	private List<UserVote> userVotes;

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		List<SimpleGrantedAuthority> authorities = new ArrayList<>();
		String habilitationP = poste.getHabilitations();
		String[] h = habilitationP.split(",");
		for (int i = 0; i < h.length; i++) {
			authorities.add(new SimpleGrantedAuthority(h[i].trim()));
		}
		authorities.add(new SimpleGrantedAuthority(additionalrole.name()));
		// System.out.println(authorities.get(0));
		return authorities;
	}

	@Override
	public String getUsername() {
		return this.email;
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

	@Override
	public String getPassword() {
		return password;
	}

	public boolean canTreatHighRiskClaim() {
		return this.getPoste().getHabilitations().contains("H4");
	}

	public boolean canTreatMiddleRiskClaim() {
		return this.getPoste().getHabilitations().contains("H3");
	}

	public boolean canTreatMinorRiskClaim() {
		return this.getPoste().getHabilitations().contains("H2");
	}

	public boolean canMeasureClaim() {
		return this.getPoste().getHabilitations().contains("H5");
	}

	public boolean canAffectTreatment() {
		return this.getPoste().getHabilitations().contains("H6");
	}

	public boolean canSeeAll() {
		return this.getPoste().getHabilitations().contains("H14");
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;
		User user = (User) obj;
		return Objects.equals(code, user.code) && Objects.equals(email, user.getEmail());
	}

	@Override
	public int hashCode() {
		return Objects.hash(code);
	}

}