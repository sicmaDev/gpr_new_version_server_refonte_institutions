package com.sicmagroup.gpr.domain.model;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "gps_bot_media")
public class BotMedia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
   
    @Lob
    @Column(name = "path", columnDefinition = "TEXT")
    private String path;

    @Lob
    @Column(name = "type", columnDefinition = "TEXT")
    private String type;

    @ManyToOne
    private Claim claim;

    @ManyToOne
    private Suggestion suggestion;

}
