package com.sicmagroup.gpr.domain.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
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
@Table(name = "gpr_ai_pinned_widgets")
public class PinnedWidget {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "user_id", length = 64, nullable = false)
    private String userId;

    @Column(name = "title", length = 255, nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "chart_type", length = 32, nullable = false)
    @Builder.Default
    private String chartType = "bar";

    @Column(name = "sql_query", columnDefinition = "TEXT")
    private String sqlQuery;

    @Column(name = "data_json", columnDefinition = "LONGTEXT")
    private String dataJson;

    @Column(name = "kpis_json", columnDefinition = "TEXT")
    private String kpisJson;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private int displayOrder = 0;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
