package com.sicmagroup.gpr.domain.dto.reports;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimPerServicePoint {
    private Long id;
    private String libelle;
    private Long total;
}
