package com.sicmagroup.gpr.domain.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.sicmagroup.gpr.domain.dto.claimResponse.PosteResponse;
import com.sicmagroup.gpr.domain.dto.claimResponse.ServicePointResponse;
import com.sicmagroup.gpr.domain.enumeration.Role;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private Long id;
    private String code;
    private String firstAndLastName;
    private String email;
    private String tel;
    private Role additionalRole;
    private String habilitationUp;
    private boolean isEmailReceiver;
    private boolean isRa;
    private String titre;
    private boolean isDeleted;
    private ServicePointResponse servicePointDto;
    private PosteResponse posteDto;
    private LocalDateTime createdAt;
    
}
