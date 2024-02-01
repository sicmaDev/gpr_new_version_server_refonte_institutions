package com.sicmagroup.gpr.repository.projection.custom;

import com.sicmagroup.gpr.domain.enumeration.GravityLevel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor 
@AllArgsConstructor
public class ClaimPerObjLevelPro {
    private Long objId;
    private String objLibelle;
    private GravityLevel objNiveau;
    private Long total;
}
