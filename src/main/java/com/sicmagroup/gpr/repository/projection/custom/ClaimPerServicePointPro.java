package com.sicmagroup.gpr.repository.projection.custom;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor 
@AllArgsConstructor
public class ClaimPerServicePointPro  {
    private Long servicePointId;
    private String libelle;
    private Long total;
}
