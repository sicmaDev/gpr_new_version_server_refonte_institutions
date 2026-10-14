package com.sicmagroup.gpr.api.extra;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sicmagroup.gpr.domain.dto.ApiResponseDto;
import com.sicmagroup.gpr.domain.dto.ErrorResponse;
import com.sicmagroup.gpr.domain.model.User;
import com.sicmagroup.gpr.service.auth.AuthenticationServiceImpl;
import com.sicmagroup.gpr.service.extra.ExtraContentService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/extra")
@RequiredArgsConstructor
public class ExtraContentController {

    private final ExtraContentService service;
    private final AuthenticationServiceImpl authService;

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseDto> deleteExtraContent(@PathVariable Long id) {
        try {
            UserDetails principal = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            User connectedUser = authService.getByEmail(principal.getUsername());
            service.deleteExtraContent(id, connectedUser);
            return ResponseEntity.ok(ApiResponseDto.builder().status(true).content("Contenu supprimé avec succès.").build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ApiResponseDto.builder()
                    .status(false)
                    .content(ErrorResponse.builder().title("Erreur").message(e.getMessage()).build())
                    .build()
            );
        }
    }
}
