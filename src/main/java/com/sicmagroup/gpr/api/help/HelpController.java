package com.sicmagroup.gpr.api.help;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sicmagroup.gpr.domain.dto.ApiResponseDto;
import com.sicmagroup.gpr.service.faq.FaqServiceImpl;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/help")
@RequiredArgsConstructor
public class HelpController {
        private final FaqServiceImpl serviceImpl;
     @GetMapping(value = "")
    public ResponseEntity<ApiResponseDto> getHelp() {
        ApiResponseDto apiResponseDto = ApiResponseDto
                .builder()
                .status(true)
                .content(serviceImpl.getHelp())
                .build();
        return ResponseEntity.ok(apiResponseDto);
    }

}
