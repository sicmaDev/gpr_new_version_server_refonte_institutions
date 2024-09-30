package com.sicmagroup.gpr.api.botkey;

import java.util.HashMap;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sicmagroup.gpr.api.claim.ClaimController;
import com.sicmagroup.gpr.api.claim.ClaimRequest;
import com.sicmagroup.gpr.api.claim.SaveRequest;
import com.sicmagroup.gpr.api.denunciation.DenunciationController;
import com.sicmagroup.gpr.api.suggestion.SuggestionAddRequest;
import com.sicmagroup.gpr.api.suggestion.SuggestionRequest;
import com.sicmagroup.gpr.domain.dto.ApiResponseDto;
import com.sicmagroup.gpr.domain.dto.ClaimDto;
import com.sicmagroup.gpr.domain.dto.ErrorResponse;
import com.sicmagroup.gpr.domain.dto.LicenceControl;
import com.sicmagroup.gpr.domain.dto.SuggestionDto;
import com.sicmagroup.gpr.domain.enumeration.ClaimType;
import com.sicmagroup.gpr.domain.enumeration.ConfigExportEnum;
import com.sicmagroup.gpr.service.auth.AuthenticationServiceImpl;
import com.sicmagroup.gpr.service.botkey.BotKeyServiceImpl;
import com.sicmagroup.gpr.utils.Utils;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/apikey")
@RequiredArgsConstructor
public class BotKeyController {
    private final BotKeyServiceImpl service;
    private final AuthenticationServiceImpl authService;
    private final DenunciationController denunciationController;
    private final ClaimController claimController;


    //Configuration
    @GetMapping("/setting")
    public ResponseEntity<ApiResponseDto> getConfiguration(HttpServletRequest request) {
        ApiResponseDto apiResponseDto;
        try {

            Boolean isAuth = service.checkApiKeyBoolean(request);
            if (isAuth == false) {
                throw new Exception("Vous n'etes pas authentifier");
            }
            HashMap<String, Object> configs = authService.exportConfig(ConfigExportEnum.configs);

            apiResponseDto = ApiResponseDto
                    .builder()
                    .status(true)
                    .content(configs.get("data"))
                    .build();
            return ResponseEntity.ok(apiResponseDto);
        } catch (Exception e) {
            apiResponseDto = ApiResponseDto
                    .builder()
                    .status(false)
                    .content(ErrorResponse.builder().message(e.getMessage()).title("EXCEPTION")
                            .build())
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
        }
    }


    // Suggestion
    @PostMapping(value = "/suggestion/save", consumes = { MediaType.APPLICATION_OCTET_STREAM_VALUE,
            MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<ApiResponseDto> saveSuggestion(@RequestPart(name = "suggestion") String suggestionStr,
            @RequestPart(name = "files", required = false) MultipartFile[] files,
            @RequestPart(name = "audios", required = false) MultipartFile[] audios, HttpServletRequest request)
            throws JsonMappingException, JsonProcessingException {
        ApiResponseDto apiResponseDto;
        apiResponseDto = Utils.verifyLicence();

        if (apiResponseDto.isStatus() && apiResponseDto.getContent().getClass() == LicenceControl.class) {
            LicenceControl lc = (LicenceControl) apiResponseDto.getContent();
            if (lc.isActif()) {
                Boolean isAuth = service.checkApiKeyBoolean(request);

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
                    if (isAuth == false) {
                        throw new Exception("Vous n'etes pas authentifier");
                    }
                    SuggestionDto suggestion = service.saveSuggestion(suggestionAddRequest,
                            request.getHeader("API_KEY"));
                    apiResponseDto = ApiResponseDto
                            .builder()
                            .status(true)
                            .content(suggestion)
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

    @GetMapping("/suggestion/{code}")
    public ResponseEntity<ApiResponseDto> getSuggestion(@PathVariable(name = "code") String code,
            HttpServletRequest request) {
        ApiResponseDto apiResponseDto;
        try {

            Boolean isAuth = service.checkApiKeyBoolean(request);
            if (isAuth == false) {
                throw new Exception("Vous n'etes pas authentifier");
            }
            SuggestionDto suggestion = service.getSuggestion(code);

            apiResponseDto = ApiResponseDto
                    .builder()
                    .status(true)
                    .content(suggestion)
                    .build();
            return ResponseEntity.ok(apiResponseDto);
        } catch (Exception e) {
            apiResponseDto = ApiResponseDto
                    .builder()
                    .status(false)
                    .content(ErrorResponse.builder().message(e.getMessage()).title("EXCEPTION")
                            .build())
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
        }
    }

    @GetMapping("/suggestion/user/{usercode}")
    public ResponseEntity<ApiResponseDto> getSuggestions(@PathVariable(name = "usercode") String usercode,
            HttpServletRequest request) {
        ApiResponseDto apiResponseDto;
        try {

            Boolean isAuth = service.checkApiKeyBoolean(request);
            if (isAuth == false) {
                throw new Exception("Vous n'etes pas authentifier");
            }
            List<SuggestionDto> suggestion = service.getSuggestions(usercode);

            apiResponseDto = ApiResponseDto
                    .builder()
                    .status(true)
                    .content(suggestion)
                    .build();
            return ResponseEntity.ok(apiResponseDto);
        } catch (Exception e) {
            apiResponseDto = ApiResponseDto
                    .builder()
                    .status(false)
                    .content(ErrorResponse.builder().message(e.getMessage()).title("EXCEPTION")
                            .build())
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
        }
    }


    //Denunciation
    @PostMapping(value = "/denunciation/save", consumes = { MediaType.APPLICATION_OCTET_STREAM_VALUE,
            MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<ApiResponseDto> saveDenunciation(@RequestPart("claim") String claimRequest,
            @RequestPart(name = "files", required = false) MultipartFile[] files,
            @RequestPart(name = "audios", required = false) MultipartFile[] audios, HttpServletRequest request)
            throws JsonMappingException, JsonProcessingException {
        ApiResponseDto apiResponseDto;

        ObjectMapper mapper = new ObjectMapper();
        ClaimRequest claimRequest2 = mapper.readValue(claimRequest, ClaimRequest.class);

        apiResponseDto = Utils.verifyLicence();

        if (apiResponseDto.isStatus() && apiResponseDto.getContent().getClass() == LicenceControl.class) {
            LicenceControl lc = (LicenceControl) apiResponseDto.getContent();
            if (lc.isActif()) {
                try {
                    Boolean isAuth = service.checkApiKeyBoolean(request);

                    if (isAuth == false) {
                        throw new Exception("Vous n'etes pas authentifier");
                    }
                    SaveRequest saveRequest = SaveRequest.builder().claimRequest(claimRequest2).files(files)
                            .audios(audios)
                            .remoteAddress(request.getRemoteAddr()).build();

                    ClaimDto claim = service.saveClaim(saveRequest, request.getHeader("API_KEY"),
                            ClaimType.DENUNCIACION);
                    apiResponseDto = ApiResponseDto
                            .builder()
                            .status(true)
                            .content(claim)
                            .build();
                    return ResponseEntity.ok(apiResponseDto);
                } catch (Exception e) {
                    apiResponseDto = ApiResponseDto
                            .builder()
                            .status(false)
                            .content(ErrorResponse.builder().message(e.getMessage()).title("EXCEPTION THROW").build())
                            .build();
                    if (e.getMessage() != null && e.getMessage().contains("not found")) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
                    } else {
                        // System.out.println(claimRequest2.getContent());
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

    @GetMapping("/denunciation/{code}")
    public ResponseEntity<ApiResponseDto> getDenunciation(@PathVariable(name = "code") String code,
            HttpServletRequest request) {

        try {

            Boolean isAuth = service.checkApiKeyBoolean(request);
            if (isAuth == false || !code.startsWith("bot") || code.length() < 10) {
                throw new Exception("Un probleme est subvenu");
            }
            return denunciationController.getClaim(code);
        } catch (Exception e) {
            ApiResponseDto apiResponseDto = ApiResponseDto
                    .builder()
                    .status(false)
                    .content(ErrorResponse.builder().message(e.getMessage()).title("EXCEPTION")
                            .build())
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
        }
    }

    @GetMapping("/denunciation/user/{userCode}")
    public ResponseEntity<ApiResponseDto> getDenunciations(@PathVariable(name = "userCode") String userCode,
            HttpServletRequest request) {
        ApiResponseDto apiResponseDto;
        try {

            Boolean isAuth = service.checkApiKeyBoolean(request);
            if (isAuth == false) {
                throw new Exception("Vous n'etes pas authentifier");
            }
            List<ClaimDto> claims = service.getClaims(userCode, ClaimType.DENUNCIACION);

            apiResponseDto = ApiResponseDto
                    .builder()
                    .status(true)
                    .content(claims)
                    .build();
            return ResponseEntity.ok(apiResponseDto);
        } catch (Exception e) {
            apiResponseDto = ApiResponseDto
                    .builder()
                    .status(false)
                    .content(ErrorResponse.builder().message(e.getMessage()).title("EXCEPTION")
                            .build())
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
        }
    }



    //Claim
    @PostMapping(value = "/claim/save", consumes = { MediaType.APPLICATION_OCTET_STREAM_VALUE,
            MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<ApiResponseDto> saveClaim(@RequestPart("claim") String claimRequest,
            @RequestPart(name = "files", required = false) MultipartFile[] files,
            @RequestPart(name = "audios", required = false) MultipartFile[] audios, HttpServletRequest request)
            throws JsonMappingException, JsonProcessingException {
        ApiResponseDto apiResponseDto;

        ObjectMapper mapper = new ObjectMapper();
        ClaimRequest claimRequest2 = mapper.readValue(claimRequest, ClaimRequest.class);

        apiResponseDto = Utils.verifyLicence();

        if (apiResponseDto.isStatus() && apiResponseDto.getContent().getClass() == LicenceControl.class) {
            LicenceControl lc = (LicenceControl) apiResponseDto.getContent();
            if (lc.isActif()) {
                try {
                    Boolean isAuth = service.checkApiKeyBoolean(request);

                    if (isAuth == false) {
                        throw new Exception("Vous n'etes pas authentifier");
                    }
                    SaveRequest saveRequest = SaveRequest.builder().claimRequest(claimRequest2).files(files)
                            .audios(audios)
                            .remoteAddress(request.getRemoteAddr()).build();

                    ClaimDto claim = service.saveClaim(saveRequest, request.getHeader("API_KEY"),
                            ClaimType.CLAIM);
                    apiResponseDto = ApiResponseDto
                            .builder()
                            .status(true)
                            .content(claim)
                            .build();
                    return ResponseEntity.ok(apiResponseDto);
                } catch (Exception e) {
                    apiResponseDto = ApiResponseDto
                            .builder()
                            .status(false)
                            .content(ErrorResponse.builder().message(e.getMessage()).title("EXCEPTION THROW").build())
                            .build();
                    if (e.getMessage() != null && e.getMessage().contains("not found")) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
                    } else {
                        // System.out.println(claimRequest2.getContent());
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

    @GetMapping("/claim/{code}")
    public ResponseEntity<ApiResponseDto> getClaim(@PathVariable(name = "code") String code,
            HttpServletRequest request) {

        try {

            Boolean isAuth = service.checkApiKeyBoolean(request);
            if (isAuth == false || !code.startsWith("bot") || code.length() < 10) {
                throw new Exception("Un probleme est subvenu");
            }
            return claimController.getClaim(code);
        } catch (Exception e) {
            ApiResponseDto apiResponseDto = ApiResponseDto
                    .builder()
                    .status(false)
                    .content(ErrorResponse.builder().message(e.getMessage()).title("EXCEPTION")
                            .build())
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
        }
    }

    @GetMapping("/claim/user/{userCode}")
    public ResponseEntity<ApiResponseDto> getClaims(@PathVariable(name = "userCode") String userCode,
            HttpServletRequest request) {
        ApiResponseDto apiResponseDto;
        try {

            Boolean isAuth = service.checkApiKeyBoolean(request);
            if (isAuth == false) {
                throw new Exception("Vous n'etes pas authentifier");
            }
            List<ClaimDto> claims = service.getClaims(userCode, ClaimType.CLAIM);

            apiResponseDto = ApiResponseDto
                    .builder()
                    .status(true)
                    .content(claims)
                    .build();
            return ResponseEntity.ok(apiResponseDto);
        } catch (Exception e) {
            apiResponseDto = ApiResponseDto
                    .builder()
                    .status(false)
                    .content(ErrorResponse.builder().message(e.getMessage()).title("EXCEPTION")
                            .build())
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
        }
    }



}
