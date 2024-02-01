package com.sicmagroup.gpr.api.config.documentation;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sicmagroup.gpr.domain.dto.ApiResponseDto;
import com.sicmagroup.gpr.domain.dto.DocumentationDto;
import com.sicmagroup.gpr.domain.dto.ErrorResponse;
import com.sicmagroup.gpr.domain.model.Documentation;
import com.sicmagroup.gpr.domain.model.User;
import com.sicmagroup.gpr.service.auth.AuthenticationServiceImpl;
import com.sicmagroup.gpr.service.documentation.DocumentationServiceImpl;

import lombok.RequiredArgsConstructor;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class DocumentationController {
    private final DocumentationServiceImpl serviceImpl;
    private final AuthenticationServiceImpl authService;
     private final ModelMapper modelMapper;

    @PostMapping(value = "/config/doc/store", consumes = { MediaType.APPLICATION_OCTET_STREAM_VALUE,
            MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<ApiResponseDto> saveDocumentatioEntity(@RequestPart(name = "libelles") String libelles,
            @RequestPart(name = "files") MultipartFile[] files) throws JsonMappingException, JsonProcessingException {

        // ObjectMapper mapper = new ObjectMapper();
        List<String> libellesGet = null;
      
        if (libelles.contains(",")) {
            libellesGet = Arrays.asList(libelles.split(","));
        } else {
            libellesGet = Arrays.asList(libelles);
        }
          System.out.println(libellesGet);
        if (libellesGet != null) {
            User connectedUser = User.builder().build();
            UserDetails collectorDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication()
                    .getPrincipal();
            try {
                connectedUser = authService.getByEmail(collectorDetails.getUsername());
            } catch (Exception e) {

                ApiResponseDto apiResponseDto = ApiResponseDto

                        .builder()
                        .status(true)
                        .content("Invalide connected user")
                        .build();
                return ResponseEntity.notFound().build();
            }

            List<Documentation> rDocumentations = serviceImpl.stores(files, libellesGet.toArray(new String[0]),
                    connectedUser);

                    System.out.println(rDocumentations);
            ApiResponseDto apiResponseDto = ApiResponseDto

                    .builder()
                    .status(true)
                    .content(rDocumentations.stream().map(this::convertToDto).collect(Collectors.toList()))
                    .build();

            return ResponseEntity.ok(apiResponseDto);
        } else {
            // Gérer le cas où libelles est vide ou invalide
            ApiResponseDto apiResponseDto = ApiResponseDto.builder()
                    .status(false)
                    .content("Invalid libelles")
                    .build();
            return ResponseEntity.badRequest().body(apiResponseDto);
        }

        //   ApiResponseDto apiResponseDto = ApiResponseDto.builder()
        //             .status(false)
        //             .content("Invalid libelles")
        //             .build();
        //     return ResponseEntity.badRequest().body(apiResponseDto);

    }

    @GetMapping(value = "/config/doc/list", consumes = { MediaType.APPLICATION_OCTET_STREAM_VALUE,
            MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<ApiResponseDto> listDocumentationEntity()
            throws JsonMappingException, JsonProcessingException {
        List<Documentation> rDocumentations = serviceImpl.list();
        ApiResponseDto apiResponseDto = ApiResponseDto

                .builder()
                .status(true)
                .content(rDocumentations.stream().map(this::convertToDto).collect(Collectors.toList()))
                .build();

        return ResponseEntity.ok(apiResponseDto);
    }

    @GetMapping(value = "/doc/get/{id}")
    public ResponseEntity<Resource> getDocument(@PathVariable Long id) {
        Documentation documentation;
        try {
            documentation = serviceImpl.getDocumentation(id);
            String fileName = documentation.getName();
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName)
                    .body(serviceImpl.loadAsResource(id));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping(value = "/config/doc/{id}/delete")
    public ResponseEntity<ApiResponseDto> deleteDocumentation(@PathVariable Long id) {
        try {
            serviceImpl.deleteDocumentation(id);
            ApiResponseDto apiResponseDto = ApiResponseDto
                    .builder()
                    .status(true)
                    .content("File deleted")
                    .build();
            return ResponseEntity.ok(apiResponseDto);
        } catch (FileNotFoundException e) {
            return ResponseEntity.notFound().build();
        }

    }

    private DocumentationDto convertToDto(Documentation documentation){
        DocumentationDto documentationDto =  modelMapper.map(documentation, DocumentationDto.class);
        return documentationDto;
    }

}
