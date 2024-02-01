package com.sicmagroup.gpr.api.synchro;

import java.util.List;

import com.sicmagroup.gpr.domain.dto.ClaimDto;
import com.sicmagroup.gpr.domain.dto.SuggestionDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncAllGet {
    
    private List<ClaimDto> claimDto;
    private List<ClaimDto> denunDto;
    private List<SuggestionDto> suggestionDto;
}
