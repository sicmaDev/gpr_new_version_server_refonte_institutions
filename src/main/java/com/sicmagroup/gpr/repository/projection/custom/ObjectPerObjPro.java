package com.sicmagroup.gpr.repository.projection.custom;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor 
@AllArgsConstructor
public class ObjectPerObjPro {
    private Long idObj;
    private Long total;
    private String libelleObj;
    private String libelleSp;
    private Long idSp;
}
