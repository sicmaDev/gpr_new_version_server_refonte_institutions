package com.sicmagroup.gpr.api.claim;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.sicmagroup.gpr.domain.enumeration.Gender;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaveRequest {
    private ClaimRequest claimRequest;
    private MultipartFile[] files;
    private MultipartFile[] audios;
    private String remoteAddress;
    
}
