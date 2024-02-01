package com.sicmagroup.gpr.api.claimAudio;

import java.io.FileNotFoundException;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sicmagroup.gpr.domain.model.ClaimAudio;
import com.sicmagroup.gpr.domain.model.Media;
import com.sicmagroup.gpr.service.claimAudio.ClaimAudioServiceImpl;
import com.sicmagroup.gpr.service.media.MediaServiceImpl;

import lombok.RequiredArgsConstructor;

@RequestMapping("/api/v1/claimaudio")
@RequiredArgsConstructor
@RestController
public class ClaimAudioController {
     private final ClaimAudioServiceImpl service;

    @GetMapping(value="/download/{id}") 
    public ResponseEntity<Resource> downloadMedia(@PathVariable Long id) {

        ClaimAudio media;
        try {
            media = service.getAudio(id);
           
            String fileName = media.getName();
            return  ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName)
                .body(service.loadAsResource(id));
        } catch (FileNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
       
    }
}
