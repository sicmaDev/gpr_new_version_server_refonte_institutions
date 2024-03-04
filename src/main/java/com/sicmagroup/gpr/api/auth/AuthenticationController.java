package com.sicmagroup.gpr.api.auth;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sicmagroup.gpr.domain.dto.ApiResponseDto;
import com.sicmagroup.gpr.domain.dto.ErrorResponse;
import com.sicmagroup.gpr.domain.dto.LicenceDto;
import com.sicmagroup.gpr.domain.dto.LicenseResponse;
import com.sicmagroup.gpr.domain.model.User;
import com.sicmagroup.gpr.service.auth.AuthenticationServiceImpl;
import com.sicmagroup.gpr.utils.Utils;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationServiceImpl authenticationServiceImpl;

    @PostMapping("/authenticate")
    public ResponseEntity<AuthenticationResponse> authenticate(@RequestBody AuthenticationRequest request) {
        return ResponseEntity.ok(authenticationServiceImpl.authenticate(request));
    }

    @PutMapping("/update")
    public ResponseEntity<ApiResponseDto> updateUser(@RequestBody UpdateRequest request) {
        ApiResponseDto apiResponseDto = new ApiResponseDto();
        try {
            authenticationServiceImpl.updateAccountUser(request);
            apiResponseDto.setContent("Success");
            apiResponseDto.setStatus(true);
        } catch (Exception e) {
            apiResponseDto.setContent(ErrorResponse.builder().message(e.getMessage()).title("Une erreur est survenue"));
            apiResponseDto.setStatus(false);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResponseDto);
        }

        return ResponseEntity.ok(apiResponseDto);

    }

    @PutMapping("/update_pwd")
    public ResponseEntity<ApiResponseDto> updatePwdUser(@RequestBody UpdatePwdRequest request) {
        ApiResponseDto apiResponseDto = new ApiResponseDto();
        try {
            authenticationServiceImpl.updateAccountPwdUser(request);
            apiResponseDto.setContent("Success");
            apiResponseDto.setStatus(true);

        } catch (Exception e) {
            apiResponseDto.setContent(
                    ErrorResponse.builder().message(e.getMessage()).title("Une erreur est survenue").build());
            apiResponseDto.setStatus(false);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResponseDto);
        }

        return ResponseEntity.ok(apiResponseDto);
    }

    @GetMapping(value = "/dashboard")
    public ResponseEntity<ApiResponseDto> getDashboard() {
        ApiResponseDto apiResponseDto = new ApiResponseDto();
        apiResponseDto.setContent(authenticationServiceImpl.getDashboard());
        apiResponseDto.setStatus(true);
        return ResponseEntity.ok(apiResponseDto);
    }

    @PostMapping("/infoLicense")
    public ResponseEntity<ApiResponseDto> infoLicence() {
        String license = "";
        return ResponseEntity.ok(Utils.verifyLicence());

    }

}
