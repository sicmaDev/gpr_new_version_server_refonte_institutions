package com.sicmagroup.gpr.api.synchro;

import java.util.List;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncAllOffline {
    List<SyncClaimRequest> claims;
    List<SyncClaimRequest> denuns;
    List<SyncSuggestionRequest> suggestions;
}
