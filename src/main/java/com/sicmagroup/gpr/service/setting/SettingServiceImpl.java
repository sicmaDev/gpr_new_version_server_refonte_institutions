package com.sicmagroup.gpr.service.setting;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.sicmagroup.gpr.api.config.setting.AddSettingRequest;
import com.sicmagroup.gpr.api.config.setting.BotRequest;
import com.sicmagroup.gpr.api.config.setting.InstitutionRequest;
import com.sicmagroup.gpr.api.config.setting.MailRequest;
import com.sicmagroup.gpr.api.config.setting.SmsRequest;
import com.sicmagroup.gpr.api.config.setting.UpdateSettingRequest;
import com.sicmagroup.gpr.utils.SettingSecrets;
import com.sicmagroup.gpr.domain.dto.CategorieObjetDto;
import com.sicmagroup.gpr.domain.dto.ClaimDto;
import com.sicmagroup.gpr.domain.dto.Client;
import com.sicmagroup.gpr.domain.dto.CollectionChannelDto;
import com.sicmagroup.gpr.domain.dto.ExistingSolutionResponse;
import com.sicmagroup.gpr.domain.dto.ExternalRecourseDto;
import com.sicmagroup.gpr.domain.dto.LanguageDto;
import com.sicmagroup.gpr.domain.dto.LicenseResponse;
import com.sicmagroup.gpr.domain.dto.PosteDto;
import com.sicmagroup.gpr.domain.dto.ProductDto;
import com.sicmagroup.gpr.domain.dto.ServicePointDto;
import com.sicmagroup.gpr.domain.dto.SuggestionDto;
import com.sicmagroup.gpr.domain.dto.UserDto;
import com.sicmagroup.gpr.domain.dto.claimResponse.ObjetResponse;
import com.sicmagroup.gpr.domain.enumeration.ClaimType;
import com.sicmagroup.gpr.domain.enumeration.ConfigExportEnum;
import com.sicmagroup.gpr.domain.model.CategorieObjet;
import com.sicmagroup.gpr.domain.model.Claim;
import com.sicmagroup.gpr.domain.model.CollectionChannel;
import com.sicmagroup.gpr.domain.model.ExistingSolution;
import com.sicmagroup.gpr.domain.model.ExternalRecourse;
import com.sicmagroup.gpr.domain.model.Language;
import com.sicmagroup.gpr.domain.model.Objet;
import com.sicmagroup.gpr.domain.model.Poste;
import com.sicmagroup.gpr.domain.model.Product;
import com.sicmagroup.gpr.domain.model.ServicePoint;
import com.sicmagroup.gpr.domain.model.Setting;
import com.sicmagroup.gpr.domain.model.Solution;
import com.sicmagroup.gpr.domain.model.Suggestion;
import com.sicmagroup.gpr.domain.model.User;
import com.sicmagroup.gpr.repository.CategorieObjetRepository;
import com.sicmagroup.gpr.repository.ClaimRepository;
import com.sicmagroup.gpr.repository.CollectionChannelRespository;
import com.sicmagroup.gpr.repository.ExistingSolutionRepository;
import com.sicmagroup.gpr.repository.ExternalRecourseRepository;
import com.sicmagroup.gpr.repository.LanguageRepository;
import com.sicmagroup.gpr.repository.LogRepository;
import com.sicmagroup.gpr.repository.ObjetRepository;
import com.sicmagroup.gpr.repository.PosteRepository;
import com.sicmagroup.gpr.repository.ProductRepository;
import com.sicmagroup.gpr.repository.ServicePointRepository;
import com.sicmagroup.gpr.repository.SettingRepository;
import com.sicmagroup.gpr.repository.SolutionRepository;
import com.sicmagroup.gpr.repository.SuggestionRepository;
import com.sicmagroup.gpr.repository.UserRepository;
import com.sicmagroup.gpr.service.auth.AuthenticationServiceImpl;
import com.sicmagroup.gpr.service.faq.FaqServiceImpl;
import com.sicmagroup.gpr.utils.Constante;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SettingServiceImpl implements SettingService {

    private final SettingRepository repository;
    private final PosteRepository posteRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final CategorieObjetRepository cObjetRepository;
    private final ObjetRepository objetRepository;
    private final SolutionRepository solutionRepository;
    private final LanguageRepository languageRepository;
    private final CollectionChannelRespository channelRespository;
    private final LogRepository logRepository;
    private final ExternalRecourseRepository externalRecourseRepository;
    private final ClaimRepository claimRepository;
    private final SuggestionRepository suggestionRepository;
    private final FaqServiceImpl faqServiceImpl;
    private final ServicePointRepository sPointRepository;
    private final ExistingSolutionRepository existingSolutionRepository;
    private final ModelMapper modelMapper;

    @Override
    public List<Setting> getAll() {
        return repository.findAll();
    }

    @Override
    public Setting save(AddSettingRequest request) {
        Setting setting = Setting
                .builder()
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .libelle(request.getLibelle())
                .value(request.getValue())

                .build();
        return repository.save(setting);
    }

    @Override
    public Setting update(UpdateSettingRequest request) throws Exception {
        Setting oldSetting = repository.findByLibelle(request.getLibelle())
                .orElseThrow(() -> new Exception("The choosen setting doesn't exist"));

        oldSetting.setUpdatedAt(LocalDateTime.now());
        // Mot de passe vide (le navigateur ne le reçoit plus) : on conserve l'ancien
        oldSetting.setValue(SettingSecrets.keepOldSecrets(request.getValue(), oldSetting.getValue()));
        return repository.save(oldSetting);
    }

    @Override
    public Setting getbySlug(String slug) throws Exception {
        Setting setting = repository.findByLibelle(slug)
                .orElseThrow(() -> new Exception("The choosen setting doesn't exist"));
        return setting;
    }

}
