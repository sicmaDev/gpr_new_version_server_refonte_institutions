package com.sicmagroup.gpr.repository.projection.custom;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor 
@AllArgsConstructor
public class ClaimPerCanalAndSpPro {
    private Long canalId;
    private String canalLibelle;
    private Long spId;
    private String spLibelle;
    private Long total;
}
