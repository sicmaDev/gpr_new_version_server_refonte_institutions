package com.sicmagroup.gpr.repository.projection.custom;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor 
@AllArgsConstructor
public class ObjectPerCanalPro {
    long id;
    String libelle;
    long total;
}
