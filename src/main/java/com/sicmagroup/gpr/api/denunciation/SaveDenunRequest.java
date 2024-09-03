package com.sicmagroup.gpr.api.denunciation;

import org.springframework.web.multipart.MultipartFile;

import com.sicmagroup.gpr.api.claim.ClaimRequest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaveDenunRequest {
       private DenunRequest claimRequest;
    private MultipartFile[] files;
    private MultipartFile[] audios;
    private String remoteAddress;
}
