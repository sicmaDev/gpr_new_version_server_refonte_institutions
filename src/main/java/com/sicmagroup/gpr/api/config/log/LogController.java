package com.sicmagroup.gpr.api.config.log;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sicmagroup.gpr.domain.dto.ApiResponseDto;
import com.sicmagroup.gpr.service.log.LogServiceImpl;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


@RequestMapping("/api/v1/config/log")
@RestController
@RequiredArgsConstructor
public class LogController {
    private final LogServiceImpl service;

    @GetMapping(value="/all")
    public ResponseEntity<ApiResponseDto> getAllLog() {
        ApiResponseDto responseDto = ApiResponseDto
            .builder()
            .status(true)
            .content(service.getAllLogs())
            .build();
        return ResponseEntity.ok(responseDto);
    }

    @GetMapping(value="/exports")
    public ResponseEntity<ApiResponseDto> getExportLogs() {
        ApiResponseDto responseDto = ApiResponseDto
            .builder()
            .status(true)
            .content(service.getExportLogs())
            .build();
        return ResponseEntity.ok(responseDto);
    }

}
