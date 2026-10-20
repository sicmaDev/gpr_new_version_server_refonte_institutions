package com.sicmagroup.gpr.domain.model;

import java.time.LocalDateTime;
import java.time.LocalTime;

import com.sicmagroup.gpr.domain.enumeration.ReportFrequency;
import com.sicmagroup.gpr.domain.enumeration.ReportRecipientType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Planification des relances de retard par e-mail : une configuration par type de destinataire. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "gps_scheduled_report_config")
public class ScheduledReportConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "recipient_type", nullable = false, unique = true)
    private ReportRecipientType recipientType;

    @Column(nullable = false)
    private boolean enabled;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportFrequency frequency;

    @Column(name = "day_of_week")
    private Integer dayOfWeek;

    @Column(name = "days_of_week")
    private String daysOfWeek;

    @Column(name = "day_of_month")
    private Integer dayOfMonth;

    @Column(name = "week_of_month")
    private Integer weekOfMonth;

    @Column(name = "month_of_quarter")
    private Integer monthOfQuarter;

    @Column(name = "send_time", nullable = false)
    private LocalTime sendTime;

    /**
     * Fuseau horaire (identifiant IANA, ex. « Africa/Porto-Novo ») dans lequel l'heure d'envoi et les jours
     * planifiés sont interprétés. Choisi une seule fois pour toute la planification : les deux configurations
     * (AGENTS, PILOTE) gardent toujours la même valeur.
     */
    @Column(name = "timezone")
    private String timezone;

    /** Dernier passage du cron sur cette configuration (sert au « une fois par jour »), même sans mail envoyé. */
    @Column(name = "last_execution_at")
    private LocalDateTime lastExecutionAt;

    /** Dernier mail réellement envoyé : c'est lui qui s'affiche comme « Dernière relance ». */
    @Column(name = "last_sent_at")
    private LocalDateTime lastSentAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Transient
    private LocalDateTime nextExecutionAt;
}
