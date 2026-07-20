package com.sicmagroup.gpr.domain.dto.claimResponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServicePointResponse {
    private Long id;
    private String uuid;
    private String libelle;
    private String description;
    private String type;
    private Long directionId;
}
