package com.sicmagroup.gpr.domain.dto;

import java.time.LocalDateTime;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Data 
@AllArgsConstructor 
@NoArgsConstructor
@Builder
public class ExistingSolutionResponse {
    
    private Long id;

    private String content;
    private Long compteur;
     private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
