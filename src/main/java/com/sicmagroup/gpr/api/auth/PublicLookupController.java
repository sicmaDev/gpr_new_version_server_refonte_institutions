package com.sicmagroup.gpr.api.auth;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sicmagroup.gpr.domain.dto.PublicPosteDto;
import com.sicmagroup.gpr.domain.dto.PublicServicePointDto;
import com.sicmagroup.gpr.service.poste.PosteServiceImpl;
import com.sicmagroup.gpr.service.servicePoint.ServicePointServiceImpl;

import lombok.RequiredArgsConstructor;

// Endpoints PUBLICS (voir SecurityConfig, permitAll sur /api/v1/public/**),
// utilisés uniquement pour alimenter le formulaire d'inscription anonyme
// (SignUser.js) en listes "Poste" et "Point de service" — un visiteur qui n'a
// jamais eu de session ne peut pas lire /api/v1/config/** (réservé H12), donc
// on expose ici des DTOs volontairement allégés (jamais "habilitations").
@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
public class PublicLookupController {

    private final PosteServiceImpl posteService;
    private final ServicePointServiceImpl servicePointService;

    @GetMapping("/poste/list")
    public List<PublicPosteDto> listPostes() {
        return posteService.getAll().stream()
                .map(p -> PublicPosteDto.builder().id(p.getId()).libelle(p.getLibelle()).build())
                .collect(Collectors.toList());
    }

    @GetMapping("/service_point/list")
    public List<PublicServicePointDto> listServicePoints() {
        return servicePointService.getAll().stream()
                .map(sp -> PublicServicePointDto.builder()
                        .id(sp.getId())
                        .libelle(sp.getLibelle())
                        .type(sp.getType())
                        .build())
                .collect(Collectors.toList());
    }
}
