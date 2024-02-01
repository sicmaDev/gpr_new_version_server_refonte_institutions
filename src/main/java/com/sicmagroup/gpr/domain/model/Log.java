package com.sicmagroup.gpr.domain.model;

import java.time.LocalDateTime;

import com.sicmagroup.gpr.domain.enumeration.LogTarget;
import com.sicmagroup.gpr.domain.enumeration.LogType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "gps_log")
public class Log {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String libelle;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(value = EnumType.STRING)
    private LogType type;

    @Enumerated(value = EnumType.STRING)
    private LogTarget target;

    private Long userId;

    private String userIpAddress;

    private LocalDateTime createdAt;
}
