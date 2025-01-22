package com.sicmagroup.gpr.api.Media;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sicmagroup.gpr.api.config.documentation.DocumentationPathRequest;
import com.sicmagroup.gpr.domain.dto.ApiResponseDto;
import com.sicmagroup.gpr.domain.dto.ErrorResponse;
import com.sicmagroup.gpr.domain.model.Documentation;
import com.sicmagroup.gpr.domain.model.Media;
import com.sicmagroup.gpr.service.media.MediaServiceImpl;

import lombok.RequiredArgsConstructor;

import java.io.FileNotFoundException;

import javax.naming.NameNotFoundException;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;


@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
@RestController
public class MediaController {
    
    private final MediaServiceImpl service;

    @GetMapping(value="/download/{id}") 
    public ResponseEntity<Resource> downloadMedia(@PathVariable Long id) {

        Media media;
        try {
            media = service.getFile(id);
           
            String fileName = media.getName();
            return  ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName)
                .body(service.loadAsResource(id));
        } catch (FileNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
       
    }

    @PostMapping(value = "/download")
    public ResponseEntity<Resource> getDocumentByFileName(@RequestBody DocumentationPathRequest request) {
        Media media;
        String newPath = request.getPath();
        try {
            media = service.getFileByPath(newPath);
            String fileName = media.getName();
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName)
                    .body(service.loadAsResource(newPath));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }


    // @PostMapping(value = "/save")
    
}
