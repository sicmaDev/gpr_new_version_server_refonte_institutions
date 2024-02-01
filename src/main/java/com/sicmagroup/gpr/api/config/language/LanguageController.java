package com.sicmagroup.gpr.api.config.language;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sicmagroup.gpr.domain.dto.ApiResponseDto;
import com.sicmagroup.gpr.domain.dto.ErrorResponse;
import com.sicmagroup.gpr.domain.dto.LanguageDto;
import com.sicmagroup.gpr.domain.model.Language;
import com.sicmagroup.gpr.service.language.LanguageServiceImpl;

import jakarta.annotation.security.RolesAllowed;
import lombok.RequiredArgsConstructor;

@RestController
@CrossOrigin

@RequestMapping("/api/v1/config/language")
@RequiredArgsConstructor
@RolesAllowed("H12")
public class LanguageController {

    private final ModelMapper modelMapper;
    private final LanguageServiceImpl languageServiceImpl;

    @GetMapping("/list/{deleted}")
    public ResponseEntity<ApiResponseDto> getAll(@PathVariable(name = "deleted", required = false) boolean deleted) {
        ApiResponseDto apiResponseDto;
        if (deleted) {
            List<Language> allLanguages = languageServiceImpl.getAllDeleted();
            apiResponseDto = ApiResponseDto.builder()
                    .status(true)
                    .content(allLanguages.stream().map(this::convertToDto).collect(Collectors.toList()))
                    .build();
            return ResponseEntity.ok(apiResponseDto);
        } else {
            List<Language> allLanguages = languageServiceImpl.getAll();
            apiResponseDto = ApiResponseDto.builder()
                    .status(true)
                    .content(allLanguages.stream().map(this::convertToDto).collect(Collectors.toList()))
                    .build();
            return ResponseEntity.ok(apiResponseDto);
        }

    }

    @GetMapping("/{id}/{deleted}")
    public ResponseEntity<ApiResponseDto> get(@PathVariable(name = "id", required = true) Long id,
            @PathVariable(name = "deleted", required = false) boolean deleted) {
        ApiResponseDto apiResponseDto;
        Language language;
        try {
            language = languageServiceImpl.getDeletedById(id, deleted);
            apiResponseDto = ApiResponseDto.builder()
                    .status(true)
                    .content(convertToDto(language))
                    .build();
            return ResponseEntity.ok(apiResponseDto);
        } catch (NotFoundException e) {
            e.printStackTrace();
            apiResponseDto = ApiResponseDto
                    .builder()
                    .status(false)
                    .content(ErrorResponse.builder().title("NOT FOUND").message("La langue choisie n'existe pas")
                            .build())
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
        }

    }

    @PostMapping("/add")
    public ResponseEntity<ApiResponseDto> addLanguage(@RequestBody LanguageDto languageDto) {
        ApiResponseDto apiResponseDto;

        Language language;
        try {
            language = languageServiceImpl.saveLanguage(convertFromDtoToEntity(languageDto));
            apiResponseDto = ApiResponseDto.builder()
                    .status(true)
                    .content(convertToDto(language))
                    .build();

            return ResponseEntity.ok(apiResponseDto);
        } catch (NotFoundException e) {
            e.printStackTrace();
            apiResponseDto = ApiResponseDto
                    .builder()
                    .status(false)
                    .content(ErrorResponse.builder().title("NOT FOUND").message("La langue choisie n'existe pas")
                            .build())
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
        }

    }

    @PutMapping("/{id}/update")
    public ResponseEntity<ApiResponseDto> updateLanguage(@PathVariable(name = "id") Long id,
            @RequestBody LanguageDto languageDto) {
        ApiResponseDto apiResponseDto;
        if (id != languageDto.getId()) {
            throw new IllegalArgumentException("Les ID ne correspondent pas");
        } else {
            Language language;
            System.out.println(languageDto);
            try {
                language = languageServiceImpl.updateLanguage(convertFromDtoToEntity(languageDto));
                apiResponseDto = ApiResponseDto
                        .builder()
                        .status(true)
                        .content(convertToDto(language))
                        .build();

                return ResponseEntity.ok(apiResponseDto);
            } catch (NotFoundException e) {
                e.printStackTrace();
                apiResponseDto = ApiResponseDto
                        .builder()
                        .status(false)
                        .content(ErrorResponse.builder().title("NOT FOUND").message("La langue choisie n'existe pas")
                                .build())
                        .build();
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
            }

        }

    }

    @DeleteMapping("/{id}/delete_temp")
    public ResponseEntity<ApiResponseDto> deleteTempLanguage(@PathVariable(name = "id") Long id) {
        ApiResponseDto apiResponseDto;
        try {
            Language language = languageServiceImpl.deleteTempLanguage(id);
            apiResponseDto = ApiResponseDto
                    .builder()
                    .status(true)
                    .content(convertToDto(language))
                    .build();
            return ResponseEntity.ok(apiResponseDto);
        } catch (NotFoundException e) {

            apiResponseDto = ApiResponseDto
                    .builder()
                    .status(false)
                    .content(ErrorResponse.builder().title("NOT FOUND").message("La langue choisie n'existe pas")
                            .build())
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);

        }
    }

    @DeleteMapping("/{id}/delete")
    public ResponseEntity<ApiResponseDto> deleteLanguage(@PathVariable(name = "id") Long id) {
        Language language = new Language();
        ApiResponseDto apiResponseDto;
        try {
            language = languageServiceImpl.getById(id);
        } catch (Exception e) {
            apiResponseDto = ApiResponseDto
                    .builder()
                    .status(false)
                    .content(ErrorResponse
                            .builder()
                            .title("NOT FOUND")
                            .message("La langue choisie n'existe pas")
                            .build())
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
        }

        if (language.getClaims().isEmpty()) {
            try {
                languageServiceImpl.deleteLanguage(language);
                apiResponseDto = ApiResponseDto
                        .builder()
                        .status(true)
                        .content(true)
                        .build();
            } catch (Exception e) {
                apiResponseDto = ApiResponseDto
                        .builder()
                        .status(false)
                        .content(ErrorResponse.builder().title("Something wrong").message(e.getMessage()).build())
                        .build();
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponseDto);
            }

            return ResponseEntity.ok(apiResponseDto);
        } else {
            apiResponseDto = ApiResponseDto
                    .builder()
                    .status(false)
                    .content(ErrorResponse
                            .builder()
                            .title("Opération impossible")
                            .message(
                                    "La langue choisie intervient dans une réclamation et ne peut donc pas être supprimée.")
                            .build())
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponseDto);
        }

    }

    private LanguageDto convertToDto(Language language) {
        LanguageDto languageDto = modelMapper.map(language, LanguageDto.class);
        return languageDto;
    }

    private Language convertFromDtoToEntity(LanguageDto languageDto) throws NotFoundException {
        Language language = modelMapper.map(languageDto, Language.class);

        if (language.getId() != null) {
            Language oldLanguage = languageServiceImpl.getById(language.getId());
            language.setUpdatedAt(LocalDateTime.now());
            language.setCreatedAt(oldLanguage.getCreatedAt());

        } else {
            language.setCreatedAt(LocalDateTime.now());
            language.setDeleted(false);
        }
        return language;
    }
}
