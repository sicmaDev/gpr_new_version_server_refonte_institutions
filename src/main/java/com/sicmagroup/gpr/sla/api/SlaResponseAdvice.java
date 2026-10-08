package com.sicmagroup.gpr.sla.api;

import java.util.List;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import com.sicmagroup.gpr.api.claim.ClaimController;
import com.sicmagroup.gpr.api.denunciation.DenunciationController;
import com.sicmagroup.gpr.domain.dto.ApiResponseDto;
import com.sicmagroup.gpr.domain.dto.ClaimDto;
import com.sicmagroup.gpr.sla.service.SlaInfoService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Ajoute le bloc « sla » à toutes les listes de plaintes renvoyées par les contrôleurs existants, sans
 * modifier ces contrôleurs. Sans effet quand le SLA est désactivé. Une erreur ici ne casse jamais la réponse.
 */
@Slf4j
@RestControllerAdvice(assignableTypes = { ClaimController.class, DenunciationController.class })
@RequiredArgsConstructor
public class SlaResponseAdvice implements ResponseBodyAdvice<Object> {

    private final SlaInfoService infoService;

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request,
            ServerHttpResponse response) {
        try {
            if (body instanceof ApiResponseDto dto) {
                if (dto.getContent() instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof ClaimDto) {
                    infoService.attach((List<ClaimDto>) list);
                } else if (dto.getContent() instanceof ClaimDto one) {
                    // détail d'une plainte (en-tête de traitement)
                    infoService.attach(List.of(one));
                }
            }
        } catch (Exception e) {
            log.warn("SLA : bloc sla non ajouté à la liste : {}", e.toString());
        }
        return body;
    }
}
