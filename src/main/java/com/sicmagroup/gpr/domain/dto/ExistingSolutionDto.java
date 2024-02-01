package com.sicmagroup.gpr.domain.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sicmagroup.gpr.domain.model.Objet;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Data 
@AllArgsConstructor 
@NoArgsConstructor
@Builder
public class ExistingSolutionDto {
   
    private Long id;

    private String content;

    private ObjetDto objetDto;
    private Long compteur;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
