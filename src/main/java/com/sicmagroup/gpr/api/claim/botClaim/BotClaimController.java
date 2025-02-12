package com.sicmagroup.gpr.api.claim.botClaim;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sicmagroup.gpr.api.claim.ClaimRequest;
import com.sicmagroup.gpr.api.claim.SaveRequest;
import com.sicmagroup.gpr.api.config.user.RegisterRequest;
import com.sicmagroup.gpr.domain.dto.ApiResponseDto;
import com.sicmagroup.gpr.domain.dto.ErrorResponse;
import com.sicmagroup.gpr.domain.enumeration.ClaimType;
import com.sicmagroup.gpr.domain.model.Claim;
import com.sicmagroup.gpr.service.claim.ClaimServiceImpl;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/v1/bot/claim")
@RequiredArgsConstructor
public class BotClaimController {
    private final ClaimServiceImpl service;

    @PostMapping("/save")
    public ResponseEntity<ApiResponseDto> postSaveBotClaim(@RequestBody MessageRequest entity,
            HttpServletRequest request) {
        ApiResponseDto apiResponseDto;
        if ((entity.getType()).equals("chat") && !entity.getFromMe() && !entity.getIsGroupMsg()
                && !(entity.getChatId()).equals("status@broadcast")) {
            ClaimRequest claimRequest = ClaimRequest.builder()
                    .clientFirstAndLastName(entity.getNotifyName())
                    .phone(entity.getFrom())
                    .content(entity.getBody())
                    .code("bot-" + entity.getChatId() +"-"+UUID.randomUUID().toString().substring(0, 5))
                    .build();
            SaveRequest saveRequest = SaveRequest.builder().claimRequest(claimRequest)
                    .remoteAddress(request.getRemoteAddr()).build();

            try {
                Claim claim = service.saveBotClaim(saveRequest,
                        ClaimType.CLAIM);
                apiResponseDto = ApiResponseDto
                        .builder()
                        .status(true)
                        .content(claim)
                        .build();
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
            return ResponseEntity.ok(apiResponseDto);

        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponseDto
                    .builder()
                    .status(false)
                    .content(ErrorResponse.builder().message("Invalide message").title("EXCEPTION TGHROW").build())
                    .build());
        }

    }

    @PostMapping("/save/form")
    public ResponseEntity<ApiResponseDto> postSaveBotClaimFromForm(@RequestBody ClaimRequest claimRequest,
            HttpServletRequest request) {
        ApiResponseDto apiResponseDto;
        SaveRequest saveRequest = SaveRequest.builder().claimRequest(claimRequest)
                .remoteAddress(request.getRemoteAddr()).build();

        try {
            Claim claim = service.saveClaim(saveRequest,
                    ClaimType.CLAIM);
            apiResponseDto = ApiResponseDto
                    .builder()
                    .status(true)
                    .content(claim)
                    .build();
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

        return ResponseEntity.ok(apiResponseDto);
    }

    // @PostMapping("/saved/")
    // public  ResponseEntity<ApiResponseDto>  saveClaim(@RequestBody RegisterRequest request) {
    //     ApiResponseDto apiResponseDto;
        
    //     SaveRequest saveRequest = SaveRequest.builder().claimRequest(claimRequest)
    //             .remoteAddress(request.getRemoteAddr()).build();

    //     try {
    //         Claim claim = service.saveClaim(saveRequest,
    //                 ClaimType.CLAIM);
    //         apiResponseDto = ApiResponseDto
    //                 .builder()
    //                 .status(true)
    //                 .content(claim)
    //                 .build();
    //     } catch (Exception e) {
    //         apiResponseDto = ApiResponseDto
    //                 .builder()
    //                 .status(false)
    //                 .content(ErrorResponse.builder().message(e.getMessage()).title("EXCEPTION TGHROW").build())
    //                 .build();
    //         if (e.getMessage().contains("not found")) {
    //             return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
    //         } else {
    //             return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponseDto);
    //         }
    //     }
    // }


}
