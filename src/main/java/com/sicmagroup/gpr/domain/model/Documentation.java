package com.sicmagroup.gpr.domain.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
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
@Table(name = "gps_documentation")
public class Documentation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Lob
    @Column(name = "name", columnDefinition = "TEXT")
    private String name;

    @Lob
    @Column(name = "libelle", columnDefinition = "TEXT")
    private String libelle;

    @Lob
    @Column(name = "path", columnDefinition = "TEXT")
    private String path;

    private Long size;

    @ManyToOne
    @JoinColumn()
    private User user;

        private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
