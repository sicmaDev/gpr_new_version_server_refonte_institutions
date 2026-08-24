package com.sicmagroup.gpr.api.claimAudio;

import java.io.FileNotFoundException;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sicmagroup.gpr.domain.dto.ApiResponseDto;
import com.sicmagroup.gpr.domain.dto.ErrorResponse;
import com.sicmagroup.gpr.domain.model.ClaimAudio;
import com.sicmagroup.gpr.domain.model.Media;
import com.sicmagroup.gpr.domain.model.User;
import com.sicmagroup.gpr.service.claimAudio.ClaimAudioServiceImpl;
import com.sicmagroup.gpr.service.media.MediaServiceImpl;
import com.sicmagroup.gpr.utils.CurrentUserUtils;

import lombok.RequiredArgsConstructor;

@RequestMapping("/api/v1/claimaudio")
@RequiredArgsConstructor
@RestController
public class ClaimAudioController {
     private final ClaimAudioServiceImpl service;
     private final CurrentUserUtils userAuth;

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

    @DeleteMapping(value = "/{id}")
    public ResponseEntity<ApiResponseDto> deleteAudio(@PathVariable Long id) {
        ApiResponseDto apiResponseDto;
        try {
            User connectedUser = userAuth.getUser();
            service.deleteAudio(id, connectedUser);
            apiResponseDto = ApiResponseDto
                    .builder()
                    .status(true)
                    .content("Audio supprimé")
                    .build();
            return ResponseEntity.ok(apiResponseDto);
        } catch (FileNotFoundException e) {
            apiResponseDto = ApiResponseDto
                    .builder()
                    .status(false)
                    .content(ErrorResponse.builder().message(e.getMessage()).title("NOT FOUND").build())
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
        } catch (Exception e) {
            apiResponseDto = ApiResponseDto
                    .builder()
                    .status(false)
                    .content(ErrorResponse.builder().message(e.getMessage()).title("FORBIDDEN").build())
                    .build();
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(apiResponseDto);
        }
    }
}
