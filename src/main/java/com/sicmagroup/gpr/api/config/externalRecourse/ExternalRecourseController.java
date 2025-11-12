package com.sicmagroup.gpr.api.config.externalRecourse;

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
import com.sicmagroup.gpr.domain.dto.ExternalRecourseDto;
import com.sicmagroup.gpr.domain.model.ExternalRecourse;
import com.sicmagroup.gpr.service.externalRecourse.ExternalRecourseServiceImpl;

import jakarta.annotation.security.RolesAllowed;
import lombok.RequiredArgsConstructor;

@RestController
@CrossOrigin

@RequestMapping("/api/v1/config/external_recourse")
@RequiredArgsConstructor
@RolesAllowed("H12")
public class ExternalRecourseController {
    private final ModelMapper modelMapper;
    private final ExternalRecourseServiceImpl service;

    @GetMapping("/list/{deleted}")
    public ResponseEntity<ApiResponseDto> getAll(@PathVariable(name = "deleted", required = false) boolean deleted) {
        ApiResponseDto apiResponseDto;
        if (deleted) {
            List<ExternalRecourse> allExternalRecourses = service.getAllDeleted();
            apiResponseDto = ApiResponseDto.builder()
                    .status(true)
                    .content(allExternalRecourses.stream().map(this::convertToDto).collect(Collectors.toList()))
                    .build();
            return ResponseEntity.ok(apiResponseDto);
        } else {
            List<ExternalRecourse> allExternalRecourses = service.getAll();
            apiResponseDto = ApiResponseDto.builder()
                    .status(true)
                    .content(allExternalRecourses.stream().map(this::convertToDto).collect(Collectors.toList()))
                    .build();
            return ResponseEntity.ok(apiResponseDto);
        }

    }

    @GetMapping("/{id}/{deleted}")
    public ResponseEntity<ApiResponseDto> get(@PathVariable(name = "id", required = true) Long id,
            @PathVariable(name = "deleted", required = false) boolean deleted) {
        ApiResponseDto apiResponseDto;
        ExternalRecourse externalRecourse;
        try {
            externalRecourse = service.getDeletedById(id, deleted);
            apiResponseDto = ApiResponseDto.builder()
                    .status(true)
                    .content(convertToDto(externalRecourse))
                    .build();
            return ResponseEntity.ok(apiResponseDto);
        } catch (NotFoundException e) {
            e.printStackTrace();
            apiResponseDto = ApiResponseDto
                    .builder()
                    .status(false)
                    .content(ErrorResponse.builder().title("NOT FOUND")
                            .message("Le recours externe choisi n'existe pas").build())
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
        }

    }

    @PostMapping("/add")
    public ResponseEntity<ApiResponseDto> addExternalRecourse(@RequestBody ExternalRecourseDto externalRecourseDto) {
        ApiResponseDto apiResponseDto;

        ExternalRecourse externalRecourse;
        try {
            externalRecourse = service
                    .saveExternalRecourse(convertFromDtoToEntity(externalRecourseDto));
            apiResponseDto = ApiResponseDto.builder()
                    .status(true)
                    .content(convertToDto(externalRecourse))
                    .build();

            return ResponseEntity.ok(apiResponseDto);
        } catch (NotFoundException e) {
            e.printStackTrace();
            apiResponseDto = ApiResponseDto
                    .builder()
                    .status(false)
                    .content(ErrorResponse.builder().title("NOT FOUND")
                            .message("Le recours externe choisi n'existe pas").build())
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
        }

    }

    @PutMapping("/{id}/update")
    public ResponseEntity<ApiResponseDto> updateExternalRecourse(@PathVariable(name = "id") Long id,
            @RequestBody ExternalRecourseDto externalRecourseDto) {
        ApiResponseDto apiResponseDto;
        Long idParsed = id.longValue();
        Long recoursId = externalRecourseDto.getId().longValue();
        if (!idParsed.equals(recoursId)) {
            throw new IllegalArgumentException("Les ID ne correspondent pas");
        } else {
            ExternalRecourse externalRecourse;
            try {
                externalRecourse = service
                        .updateExternalRecourse(convertFromDtoToEntity(externalRecourseDto));
                apiResponseDto = ApiResponseDto
                        .builder()
                        .status(true)
                        .content(convertToDto(externalRecourse))
                        .build();

                return ResponseEntity.ok(apiResponseDto);
            } catch (NotFoundException e) {
                e.printStackTrace();
                apiResponseDto = ApiResponseDto
                        .builder()
                        .status(false)
                        .content(ErrorResponse.builder().title("NOT FOUND")
                                .message("Le recours externe choisi n'existe pas").build())
                        .build();
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
            }

        }

    }

    @DeleteMapping("/{id}/delete_temp")
    public ResponseEntity<ApiResponseDto> deleteTempExternalRecourse(@PathVariable(name = "id") Long id) {
        ApiResponseDto apiResponseDto;
        try {
            ExternalRecourse externalRecourse = service.deleteTempExternalRecourse(id);
            apiResponseDto = ApiResponseDto
                    .builder()
                    .status(true)
                    .content(convertToDto(externalRecourse))
                    .build();
            return ResponseEntity.ok(apiResponseDto);
        } catch (NotFoundException e) {

            apiResponseDto = ApiResponseDto
                    .builder()
                    .status(false)
                    .content(ErrorResponse.builder().title("NOT FOUND")
                            .message("Le recours externe choisi n'existe pas").build())
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);

        }
    }

    @DeleteMapping("/{id}/delete")
    public ResponseEntity<ApiResponseDto> deleteExternalRecourse(@PathVariable(name = "id") Long id) {
        ApiResponseDto apiResponseDto;
        ExternalRecourse externalRecourse = new ExternalRecourse();
        try {
            externalRecourse = service.getById(id);
        } catch (Exception e) {
            apiResponseDto = ApiResponseDto
                    .builder()
                    .status(false)
                    .content(ErrorResponse
                            .builder()
                            .title("NOT FOUND")
                            .message("Le recours externe choisi n'existe pas")
                            .build())
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
        }

        if (externalRecourse.getClaims().isEmpty()) {
            try {
                service.deleteExternalRecourse(externalRecourse);
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
                                    "Le recours externe choisi intervient dans une réclamation et ne peut donc pas être supprimé.")
                            .build())
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponseDto);
        }

    }

    private ExternalRecourseDto convertToDto(ExternalRecourse externalRecourse) {
        ExternalRecourseDto externalRecourseDto = modelMapper.map(externalRecourse, ExternalRecourseDto.class);
        return externalRecourseDto;
    }

    private ExternalRecourse convertFromDtoToEntity(ExternalRecourseDto externalRecourseDto) throws NotFoundException {
        ExternalRecourse externalRecourse = modelMapper.map(externalRecourseDto, ExternalRecourse.class);

        if (externalRecourse.getId() != null) {
            ExternalRecourse oldExternalRecourse;
            try {
                oldExternalRecourse = service.getById(externalRecourse.getId());
                externalRecourse.setCreatedAt(oldExternalRecourse.getCreatedAt());
                externalRecourse.setUpdatedAt(LocalDateTime.now());
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        } else {
            externalRecourse.setCreatedAt(LocalDateTime.now());
            externalRecourse.setDeleted(false);
        }
        return externalRecourse;
    }

}
