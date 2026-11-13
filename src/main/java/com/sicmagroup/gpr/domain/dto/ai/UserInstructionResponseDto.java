package com.sicmagroup.gpr.domain.dto.ai;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInstructionResponseDto {
    private String id;
    private String userId;
    private String instruction;
    private List<String> questions;
    private boolean isPersonal;
    private boolean isDefault;
    private boolean isActive;
    private String createdAt;
    private String updatedAt;
}
