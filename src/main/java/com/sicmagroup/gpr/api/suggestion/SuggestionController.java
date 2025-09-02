package com.sicmagroup.gpr.api.suggestion;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sicmagroup.gpr.api.Media.MediaResponse;
import com.sicmagroup.gpr.api.claimAudio.ClaimAudioResponse;
import com.sicmagroup.gpr.api.denunciation.DenunRequest;
import com.sicmagroup.gpr.domain.dto.ApiResponseDto;
import com.sicmagroup.gpr.domain.dto.ErrorResponse;
import com.sicmagroup.gpr.domain.dto.LicenceControl;
import com.sicmagroup.gpr.domain.dto.SuggestionDto;
import com.sicmagroup.gpr.domain.enumeration.ClaimStatus;
import com.sicmagroup.gpr.domain.enumeration.ClaimType;
import com.sicmagroup.gpr.domain.enumeration.Role;
import com.sicmagroup.gpr.domain.model.Claim;
import com.sicmagroup.gpr.domain.model.Media;
import com.sicmagroup.gpr.domain.model.Suggestion;
import com.sicmagroup.gpr.domain.model.User;
import com.sicmagroup.gpr.service.auth.AuthenticationServiceImpl;
import com.sicmagroup.gpr.service.claimAudio.ClaimAudioServiceImpl;
import com.sicmagroup.gpr.service.media.MediaServiceImpl;
import com.sicmagroup.gpr.service.suggestion.SuggestionServiceImpl;
import com.sicmagroup.gpr.utils.Utils;

import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/api/v1/suggestion")
@RequiredArgsConstructor
public class SuggestionController {

    private final SuggestionServiceImpl service;
    private final ModelMapper modelMapper;
    private final ClaimAudioServiceImpl claimAudioServiceImpl;
    private final AuthenticationServiceImpl authService;
    private final MediaServiceImpl mediaService;

    @GetMapping(value = "/all")
    public ResponseEntity<ApiResponseDto> getAllSuggestion() {
        List<Suggestion> suggestions = service.getAll();
        List<SuggestionDto> suggestionDtos = suggestions.stream().map(this::convertToDto).collect(Collectors.toList());

        ApiResponseDto apiResponseDto = ApiResponseDto
                .builder()
                .status(true)
                .content(suggestionDtos)
                .build();
        return ResponseEntity.ok(apiResponseDto);
    }

    @GetMapping(value = "/list")
    public ResponseEntity<ApiResponseDto> getList() {
        ApiResponseDto apiResponseDto = ApiResponseDto.builder().build();
        List<Suggestion> suggestions = service.getAllByStatusNot(ClaimStatus.TEMP_SAVED);
        List<SuggestionDto> suggestionDtos = suggestions.stream()
            .map(this::convertToDto)
            .sorted(Comparator.comparing(SuggestionDto::getCreatedAt).reversed())
            .collect(Collectors.toList());
            
        // List<Suggestion> suggestions = service.get
        apiResponseDto = ApiResponseDto
                .builder()
                .status(true)
                .content(suggestionDtos)
                .build();
        return ResponseEntity.ok(apiResponseDto);
    }

    @GetMapping(value = "/list/{status}")
    public ResponseEntity<ApiResponseDto> getTreaTableList(@PathVariable ClaimStatus status) {
        ApiResponseDto apiResponseDto = ApiResponseDto.builder().build();
        List<Suggestion> suggestions = new ArrayList<>();
        User connectedUser = User.builder().build();
        UserDetails collectorDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication()
                    .getPrincipal();
           
        try {
            connectedUser = authService.getByEmail(collectorDetails.getUsername());
        } catch (Exception e) {
            apiResponseDto = ApiResponseDto
                    .builder()
                    .status(false)
                    .content(ErrorResponse.builder().message("Utilisateur introuvable")
                            .title("NOT FOUND EXCEPTION")
                            .build())
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
        }

        if (status == ClaimStatus.TEMP_SAVED) {
            
            User collector;
            try {
                collector = authService.getByEmail(collectorDetails.getUsername());
                suggestions = service.getAllByCollectorAndStatus(collector, status);

                List<Suggestion> filteredSuggestions = new ArrayList<>();
                //recuperer pour le pilote les suggestions du bot   *
                // System.out.println("tolotolo : "+connectedUser.getAdditionalrole() );              
                if (connectedUser.getAdditionalrole().equals(Role.PILOTE)) {
                    List<Suggestion> allSuggestions = service.getAllByStatusIn(Arrays.asList(status));
                    //    suggestions = allSuggestions;
                    for (Suggestion suggestion : allSuggestions) {
                        if (suggestion.getCode().startsWith("bot")) {
                            suggestions.add(suggestion);
                        }
                    }
                }


            } catch (Exception e) {
                apiResponseDto = ApiResponseDto
                        .builder()
                        .status(false)
                        .content(ErrorResponse.builder().message("Utilisateur introuvable").title("NOT FOUND EXCEPTION")
                                .build())
                        .build();
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
            }

        } else {
            suggestions = service.getAllByStatusIn(Arrays.asList(status));
        }
        List<SuggestionDto> suggestionDtos = suggestions.stream()
            .map(this::convertToDto)
            .sorted(Comparator.comparing(SuggestionDto::getCreatedAt).reversed())
            .collect(Collectors.toList());

        // List<Suggestion> suggestions = service.get
        apiResponseDto = ApiResponseDto
                .builder()
                .status(true)
                .content(suggestionDtos)
                .build();
        return ResponseEntity.ok(apiResponseDto);
    }

    @PostMapping(value = "/add", consumes = { MediaType.APPLICATION_OCTET_STREAM_VALUE,
            MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<ApiResponseDto> saveSuggestion(@RequestPart(name = "suggestion") String suggestionStr,
            @RequestPart(name = "files", required = false) MultipartFile[] files,
            @RequestPart(name = "audios", required = false) MultipartFile[] audios)
            throws JsonMappingException, JsonProcessingException {
        ApiResponseDto apiResponseDto;
        apiResponseDto = Utils.verifyLicence();
        ;
        if (apiResponseDto.isStatus() && apiResponseDto.getContent().getClass() == LicenceControl.class) {
            LicenceControl lc = (LicenceControl) apiResponseDto.getContent();
            if (lc.isActif()) {
                ObjectMapper mapper = new ObjectMapper();
                SuggestionRequest suggestionRequest = mapper.readValue(suggestionStr, SuggestionRequest.class);
                SuggestionAddRequest suggestionAddRequest = SuggestionAddRequest
                        .builder()
                        .suggestionRequest(suggestionRequest)
                        .files(files)
                        .audios(audios)
                        .build();
                try {
                    Suggestion suggestion = service.saveSuggestion(suggestionAddRequest, ClaimStatus.SAVED);
                    apiResponseDto = ApiResponseDto
                            .builder()
                            .status(true)
                            .content(convertToDto(suggestion))
                            .build();
                    return ResponseEntity.ok(apiResponseDto);
                } catch (Exception e) {
                    apiResponseDto = ApiResponseDto
                            .builder()
                            .status(false)
                            .content(ErrorResponse.builder().message(e.getMessage()).title("EXCEPTION TGHROW").build())
                            .build();
                    if (e.getMessage().contains("not found")) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
                    } else {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponseDto);
                    }
                }
            } else {
                apiResponseDto = ApiResponseDto
                        .builder()
                        .status(false)
                        .content(lc)
                        .build();

                return ResponseEntity.ok(apiResponseDto);
            }

        } else {

            return ResponseEntity.ok(apiResponseDto);
        }

    }

    @PostMapping(value = "/save_temp", consumes = { MediaType.APPLICATION_OCTET_STREAM_VALUE,
            MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<ApiResponseDto> saveTempSuggestion(@RequestPart(name = "suggestion") String suggestionStr,
            @RequestPart(name = "files", required = false) MultipartFile[] files,
            @RequestPart(name = "audios", required = false) MultipartFile[] audios)
            throws JsonMappingException, JsonProcessingException {
        ApiResponseDto apiResponseDto;
        apiResponseDto = Utils.verifyLicence();
        
        if (apiResponseDto.isStatus() && apiResponseDto.getContent().getClass() == LicenceControl.class) {
            LicenceControl lc = (LicenceControl) apiResponseDto.getContent();
            if (lc.isActif()) {
                ObjectMapper mapper = new ObjectMapper();
                // System.out.println("suggestionStr");
                // System.out.println(suggestionStr);
                SuggestionRequest suggestionRequest = mapper.readValue(suggestionStr, SuggestionRequest.class);

                SuggestionAddRequest suggestionAddRequest = SuggestionAddRequest
                        .builder()
                        .suggestionRequest(suggestionRequest)
                        .files(files)
                        .audios(audios)
                        .build();
                try {
                    Suggestion suggestion = service.saveSuggestion(suggestionAddRequest, ClaimStatus.TEMP_SAVED);
                    apiResponseDto = ApiResponseDto
                            .builder()
                            .status(true)
                            .content(convertToDto(suggestion))
                            .build();
                    return ResponseEntity.ok(apiResponseDto);
                } catch (Exception e) {
                    apiResponseDto = ApiResponseDto
                            .builder()
                            .status(false)
                            .content(ErrorResponse.builder().message(e.getMessage()).title("EXCEPTION TGHROW").build())
                            .build();
                    if (e.getMessage().contains("not found")) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
                    } else {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponseDto);
                    }
                }
            } else {
                apiResponseDto = ApiResponseDto
                        .builder()
                        .status(false)
                        .content(lc)
                        .build();

                return ResponseEntity.ok(apiResponseDto);
            }

        } else {

            return ResponseEntity.ok(apiResponseDto);
        }
    }

        @GetMapping("/getAudiosBy/{suggestionId}")
    public ResponseEntity<List<ClaimAudioResponse>> getAllSuggestionAudioForASuggestion(
            @PathVariable(name = "suggestionId") Long suggestionId) {
        ApiResponseDto apiResponseDto = ApiResponseDto.builder().build();
        Suggestion suggestion = Suggestion.builder().build();
        try {
            suggestion = service.getById(suggestionId);

        } catch (NotFoundException e) {
            apiResponseDto = ApiResponseDto
                    .builder()
                    .status(false)
                    .content(ErrorResponse.builder().message("Claim not found").title("NOT FOUND EXCEPTION").build())
                    .build();
            return ResponseEntity.notFound().build();
        }
         catch (Exception e) {
            apiResponseDto = ApiResponseDto
                    .builder()
                    .status(false)
                    .content(ErrorResponse.builder().message("Claim not found").title("NOT FOUND EXCEPTION").build())
                    .build();
            return ResponseEntity.notFound().build();
        }
        List<ClaimAudioResponse> medias = claimAudioServiceImpl.getAudiosBySuggestion(suggestion);
        // List<MediaResponse> mediaResponses =
        // medias.stream().map(this::convertToResponse).collect(Collectors.toList());
        // System.out.println("medias.size");
        // System.out.println(medias.size());

        apiResponseDto = ApiResponseDto
                .builder()
                .status(true)
                .content(medias)
                .build();
        return ResponseEntity.ok(medias);
    }


    @PutMapping(value = "/treatSuggestion")
    public ResponseEntity<ApiResponseDto> treatSuggestion(@RequestBody TreatSuggestionRequest request) {
        ApiResponseDto apiResponseDto;
        apiResponseDto = Utils.verifyLicence();
        ;
        if (apiResponseDto.isStatus() && apiResponseDto.getContent().getClass() == LicenceControl.class) {
            LicenceControl lc = (LicenceControl) apiResponseDto.getContent();
            if (lc.isActif()) {
                User treator;
                try {
                    treator = authService.getById(request.getTreatorId());
                } catch (Exception e) {
                    apiResponseDto = ApiResponseDto
                            .builder()
                            .status(false)
                            .content(ErrorResponse.builder().message(e.getMessage()).title("EXCEPTION TGHROW").build())
                            .build();

                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);

                }

                Suggestion suggestion;

                try {
                    suggestion = service.getById(request.getId());
                } catch (Exception e) {
                    apiResponseDto = ApiResponseDto
                            .builder()
                            .status(false)
                            .content(ErrorResponse.builder().message(e.getMessage()).title("EXCEPTION TGHROW").build())
                            .build();

                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
                }

                try {
                    suggestion = service.treatSuggestion(suggestion, treator, request);
                } catch (Exception e) {
                    apiResponseDto = ApiResponseDto
                            .builder()
                            .status(false)
                            .content(ErrorResponse.builder().message(e.getMessage()).title("EXCEPTION TGHROW").build())
                            .build();

                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
                }

                apiResponseDto = ApiResponseDto
                        .builder()
                        .status(true)
                        .content(convertToDto(suggestion))
                        .build();
                return ResponseEntity.ok(apiResponseDto);
            } else {
                apiResponseDto = ApiResponseDto
                        .builder()
                        .status(false)
                        .content(lc)
                        .build();

                return ResponseEntity.ok(apiResponseDto);
            }

        } else {

            return ResponseEntity.ok(apiResponseDto);
        }
    }

    @GetMapping("/getFilesBy/{suggestionId}")
    public ResponseEntity<ApiResponseDto> getAllFilesForAClaim(@PathVariable(name = "suggestionId") Long suggestionId) {
        ApiResponseDto apiResponseDto = ApiResponseDto.builder().build();
        Suggestion suggestion = Suggestion.builder().build();
        try {
            suggestion = service.getById(suggestionId);

        } catch (Exception e) {
            apiResponseDto = ApiResponseDto
                    .builder()
                    .status(false)
                    .content(ErrorResponse.builder().message("Suggestion not found").title("NOT FOUND EXCEPTION")
                            .build())
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
        }
        List<Media> medias = mediaService.getFilesBySuggestion(suggestion);
        List<MediaResponse> mediaResponses = medias.stream().map(this::convertToResponse).collect(Collectors.toList());
        System.out.println("medias.size");
        System.out.println(medias.size());

        apiResponseDto = ApiResponseDto
                .builder()
                .status(true)
                .content(mediaResponses)
                .build();
        return ResponseEntity.ok(apiResponseDto);
    }

    private SuggestionDto convertToDto(Suggestion suggestion) {
        SuggestionDto suggestionDto = modelMapper.map(suggestion, SuggestionDto.class);
        if (suggestion.getCreatedAt() != null) {
            suggestionDto.setCreatedAt(suggestion.getCreatedAt().toString());
            // System.out.println(suggestionDto.getCreatedAt());
        }
        if (suggestion.getUpdatedAt() != null) {
            suggestionDto.setUpdatedAt(suggestion.getUpdatedAt().toString());
            // System.out.println(suggestionDto.getUpdatedAt());
        }

        if (suggestion.getReceiptDateTime() != null) {
            suggestionDto.setReceiptDateTime(suggestion.getReceiptDateTime().toString());
        }

        if (suggestion.getTreatAt() != null) {
            suggestionDto.setTreatAt(suggestion.getTreatAt().toString());
        }

        return suggestionDto;
    }

    private MediaResponse convertToResponse(Media media) {
        MediaResponse mediaResponse = modelMapper.map(media, MediaResponse.class);
        mediaResponse.setSize(media.getSize());
        return mediaResponse;
    }

}
