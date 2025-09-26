package com.sicmagroup.gpr.api.claimAudio;

import java.net.URL;

import org.springframework.core.io.Resource;

import com.sicmagroup.gpr.domain.dto.ExtraContentResponse;

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

public class ClaimAudioResponse {
    private String name;
    private Long id;
    private Long size;
    private boolean is_extra;
    private ExtraContentResponse extra;
    private String path;
    private byte[] data;
    
}