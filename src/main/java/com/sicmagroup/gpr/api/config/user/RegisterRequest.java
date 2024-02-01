package com.sicmagroup.gpr.api.config.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import com.sicmagroup.gpr.domain.enumeration.Role;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    private Long id;
    private String firstAndLastName;
    private String additionalRole;
    private String habilitations;
    private String tel;
    private String email;
    private String password;
    private Long servicePointId;
    private Long posteId;

}
