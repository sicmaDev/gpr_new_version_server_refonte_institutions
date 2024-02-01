package com.sicmagroup.gpr.repository.projection.custom;

import com.sicmagroup.gpr.domain.enumeration.Gender;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuggesPerGenderAndAgencePro {
     private Object id;
    private Object libelle;
    private Long total;
    private Object gender;
}
