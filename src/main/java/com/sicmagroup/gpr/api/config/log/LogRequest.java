package com.sicmagroup.gpr.api.config.log;

import java.time.LocalDateTime;

import com.sicmagroup.gpr.domain.enumeration.LogTarget;
import com.sicmagroup.gpr.domain.enumeration.LogType;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogRequest {
  
    private Long id;

    private String libelle;

   
    private String content;

   
    private LogType type;

   
    private LogTarget target;

    private Long userId;

    private String userIpAddress;

     private LocalDateTime createdAt;
}
