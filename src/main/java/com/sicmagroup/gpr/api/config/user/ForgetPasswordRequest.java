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
public class  ForgetPasswordRequest {
    private String email;
}
