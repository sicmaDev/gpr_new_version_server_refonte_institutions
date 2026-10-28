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
public class SyncResultResponse {
    private int failedCount;
    private List<String> failedCodes;
    // Une ligne par élément refusé, au format "code : raison", pour que l'écran puisse l'afficher.
    private List<String> failureDetails;
}
