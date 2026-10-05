package com.sicmagroup.gpr.domain.model.chat;

import com.sicmagroup.gpr.domain.converter.EncryptedStringConverter;
import java.time.LocalDateTime;
import java.util.List;

import com.sicmagroup.gpr.domain.model.User;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "gps_vote")
public class Vote {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Lob
    @Convert(converter = EncryptedStringConverter.class)
    @Column(columnDefinition = "MEDIUMTEXT")
    private String contenu;

    @Lob
    @Convert(converter = EncryptedStringConverter.class)
    @Column(columnDefinition = "MEDIUMTEXT")
    private String commentaire;

    @ManyToOne
    private User author;
    private boolean isChoosed;

    @ManyToOne
    private Chat chat;

    @OneToMany(mappedBy = "vote", fetch = FetchType.EAGER)
    private List<UserVote> userVotes;

    @OneToOne(fetch = FetchType.EAGER)
    private Message message;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
