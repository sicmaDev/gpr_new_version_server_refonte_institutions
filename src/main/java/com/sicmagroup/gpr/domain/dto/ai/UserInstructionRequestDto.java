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
public class UserInstructionRequestDto {
    private String id;
    private String instruction;
    private List<String> questions;
    private Boolean isActive;
}
