package com.sicmagroup.gpr.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "wgpr_messages", indexes = {
    @Index(name = "idx_wm_from_number", columnList = "from_number"),
    @Index(name = "idx_wm_status",      columnList = "status"),
    @Index(name = "idx_wm_timestamp",   columnList = "timestamp")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WgprMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "wa_message_id", unique = true, nullable = false, length = 255)
    private String waMessageId;

    @Column(name = "from_number", nullable = false, length = 255)
    private String fromNumber;

    @Column(name = "from_name", length = 255)
    private String fromName;

    @Lob
    @Column(name = "body")
    private String body;

    @Column(name = "media_type", length = 50)
    private String mediaType;

    @Column(name = "media_path", length = 500)
    private String mediaPath;

    @Column(name = "timestamp", nullable = false)
    private Long timestamp;

    @Column(name = "wa_type", length = 20)
    @Builder.Default
    private String waType = "chat";

    @Column(name = "is_read", columnDefinition = "TINYINT(1) DEFAULT 0")
    private boolean read;

    @Column(name = "status", length = 20)
    @Builder.Default
    private String status = "new";

    @Column(name = "connected_number", length = 255)
    private String connectedNumber;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (status    == null) status    = "new";
    }
}