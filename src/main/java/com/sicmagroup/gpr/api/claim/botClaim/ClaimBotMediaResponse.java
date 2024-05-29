package com.sicmagroup.gpr.api.claim.botClaim;
import java.net.URL;

import org.springframework.core.io.Resource;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ClaimAudioResponse
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class ClaimBotMediaResponse {
    private Long id;
    private String type;
    private String path;
    private byte[] data;
    
}
