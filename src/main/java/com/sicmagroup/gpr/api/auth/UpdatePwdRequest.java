package com.sicmagroup.gpr.api.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePwdRequest {
    
    private Long id;
    private String oldPassword;
    private String newPassword;
}
