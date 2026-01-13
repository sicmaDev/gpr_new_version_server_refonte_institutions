package com.sicmagroup.gpr.api.config.poste;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.dao.DataIntegrityViolationException;
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
import com.sicmagroup.gpr.domain.dto.PosteDto;
import com.sicmagroup.gpr.domain.model.Poste;
import com.sicmagroup.gpr.repository.PosteRepository;
import com.sicmagroup.gpr.service.poste.PosteServiceImpl;

import jakarta.annotation.security.RolesAllowed;
import lombok.RequiredArgsConstructor;

@RestController
@CrossOrigin

@RequestMapping("/api/v1/config/poste")
@RequiredArgsConstructor
@RolesAllowed("H12")
public class PosteController {

    private final ModelMapper modelMapper;
    private final PosteServiceImpl posteServiceImpl;
    private final PosteRepository posteRepository;

    @GetMapping("/list/{deleted}")
    public ResponseEntity<ApiResponseDto> getAll(@PathVariable(name = "deleted", required = false) boolean deleted) {
        ApiResponseDto apiResponseDto;
        if (deleted) {
            List<Poste> allPostes = posteServiceImpl.getAllDeleted();
            apiResponseDto = ApiResponseDto.builder()
                    .status(true)
                    .content(allPostes.stream().map(this::convertToDto).collect(Collectors.toList()))
                    .build();
            return ResponseEntity.ok(apiResponseDto);
        } else {
            List<Poste> allPostes = posteServiceImpl.getAll();
            apiResponseDto = ApiResponseDto.builder()
                    .status(true)
                    .content(allPostes.stream().map(this::convertToDto).collect(Collectors.toList()))
                    .build();
            return ResponseEntity.ok(apiResponseDto);
        }

    }

    @GetMapping("/{id}/{deleted}")
    public ResponseEntity<ApiResponseDto> get(@PathVariable(name = "id", required = true) Long id,
            @PathVariable(name = "deleted", required = false) boolean deleted) {
        ApiResponseDto apiResponseDto;
        Poste poste;
        try {
            poste = posteServiceImpl.getDeletedById(id, deleted);
            apiResponseDto = ApiResponseDto.builder()
                    .status(true)
                    .content(convertToDto(poste))
                    .build();
            return ResponseEntity.ok(apiResponseDto);
        } catch (NotFoundException e) {
            e.printStackTrace();
            apiResponseDto = ApiResponseDto
                    .builder()
                    .status(false)
                    .content(ErrorResponse.builder().title("NOT FOUND").message("Poste not found").build())
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
        }

    }

    @PostMapping("/add")
    public ResponseEntity<ApiResponseDto> addPoste(@RequestBody PosteDto posteDto) {
        ApiResponseDto apiResponseDto;

        Poste poste;
        try {
            poste = posteServiceImpl.savePoste(convertFromDtoToEntity(posteDto));
            apiResponseDto = ApiResponseDto.builder()
                    .status(true)
                    .content(convertToDto(poste))
                    .build();
            return ResponseEntity.ok(apiResponseDto);
        } catch (DataIntegrityViolationException e) {

            apiResponseDto = ApiResponseDto
                .builder()
                .status(false)
                .content(ErrorResponse.builder()
                        .title("Erreur de duplication")
                        .message("Une configuration avec le même libellé existe déjà.")
                        .build())
                .build();

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponseDto);
        } catch (NotFoundException e) {
            e.printStackTrace();
            apiResponseDto = ApiResponseDto
                    .builder()
                    .status(false)
                    .content(ErrorResponse.builder().title("NOT FOUND").message("Poste not found").build())
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
        }

    }

    @PutMapping("/{id}/update")
    public ResponseEntity<ApiResponseDto> updatePoste(@PathVariable(name = "id") Long id,
            @RequestBody PosteDto posteDto) {
        ApiResponseDto apiResponseDto;
        Long idParsed = id.longValue();
        Long posteId = posteDto.getId().longValue();
        if (!idParsed.equals(posteId)) {
            throw new IllegalArgumentException("Les ID ne correspondent pas");
        } else {
            Poste poste;
            try {
                poste = posteServiceImpl.getById(id);
             } catch (DataIntegrityViolationException e) {

                apiResponseDto = ApiResponseDto
                    .builder()
                    .status(false)
                    .content(ErrorResponse.builder()
                            .title("Erreur de duplication")
                            .message("Une configuration avec le même libellé existe déjà.")
                            .build())
                    .build();

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponseDto);
            } catch (Exception e) {
                apiResponseDto = ApiResponseDto
                        .builder()
                        .status(false)
                        .content(ErrorResponse
                                .builder()
                                .title("NOT FOUND")
                                .message("Poste introuvable")
                                .build())
                        .build();
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
            }

            // Vérification habilitation H12
            if (poste.getHabilitations() != null && poste.getHabilitations().contains("H12")) {
                try {
                    long countH12 = posteRepository.countByHabilitationsContaining("H12");
                    if (countH12 <= 1 && !posteDto.getHabilitations().contains("H12")) {
                        throw new IllegalStateException("Impossible de modifier le dernier poste avec l’habilitation H12 pour cette institution.");
                    }
                } catch (IllegalStateException e) {
                    apiResponseDto = ApiResponseDto.builder()
                            .status(false)
                            .content(ErrorResponse.builder()
                                    .title("Opération impossible")
                                    .message(e.getMessage())
                                    .build())
                            .build();
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponseDto);
                }
            }

            try {
                poste = posteServiceImpl.updatePoste(convertFromDtoToEntity(posteDto));
                apiResponseDto = ApiResponseDto
                        .builder()
                        .status(true)
                        .content(convertToDto(poste))
                        .build();

                return ResponseEntity.ok(apiResponseDto);
            } catch (NotFoundException e) {
                e.printStackTrace();
                apiResponseDto = ApiResponseDto
                        .builder()
                        .status(false)
                        .content(ErrorResponse.builder().title("NOT FOUND").message("Poste not found").build())
                        .build();
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
            }

        }

    }

    @DeleteMapping("/{id}/delete_temp")
    public ResponseEntity<ApiResponseDto> deleteTempProduct(@PathVariable(name = "id") Long id) {
        ApiResponseDto apiResponseDto;
        try {
            Poste poste = posteServiceImpl.deleteTempPoste(id);
            apiResponseDto = ApiResponseDto
                    .builder()
                    .status(true)
                    .content(convertToDto(poste))
                    .build();
            return ResponseEntity.ok(apiResponseDto);
        } catch (NotFoundException e) {

            apiResponseDto = ApiResponseDto
                    .builder()
                    .status(false)
                    .content(ErrorResponse.builder().title("NOT FOUND").message("Poste not found").build())
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);

        }
    }

    @DeleteMapping("/{id}/delete")
    public ResponseEntity<ApiResponseDto> deletePoste(@PathVariable(name = "id") Long id) {
        Poste poste = new Poste();
        ApiResponseDto apiResponseDto;
        try {
            poste = posteServiceImpl.getById(id);
        } catch (Exception e) {
            apiResponseDto = ApiResponseDto
                    .builder()
                    .status(false)
                    .content(ErrorResponse
                            .builder()
                            .title("NOT FOUND")
                            .message("Poste introuvable")
                            .build())
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
        }

        // Vérification habilitation H12
        if (poste.getHabilitations() != null && poste.getHabilitations().contains("H12")) {
            try {
                long countH12 = posteRepository.countByHabilitationsContaining("H12");
                if (countH12 <= 1) {
                    throw new IllegalStateException("Impossible de supprimer le dernier poste avec l’habilitation H12 pour cette institution.");
                }
            } catch (IllegalStateException e) {
                apiResponseDto = ApiResponseDto.builder()
                        .status(false)
                        .content(ErrorResponse.builder()
                                .title("Opération impossible")
                                .message(e.getMessage())
                                .build())
                        .build();
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponseDto);
            }
        }

        if (poste.getUsers().isEmpty()) {
            try {
                posteServiceImpl.deletePoste(poste);
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
                            .message("Le poste est affecté à un utilisateur et ne peut donc pas être supprimé.")
                            .build())
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponseDto);
        }

    }

    private PosteDto convertToDto(Poste poste1) {
        PosteDto posteDto = modelMapper.map(poste1, PosteDto.class);
        return posteDto;
    }

    private Poste convertFromDtoToEntity(PosteDto posteDto) throws NotFoundException {
        Poste poste = modelMapper.map(posteDto, Poste.class);

        if (poste.getId() != null) {
            Poste oldPoste = posteServiceImpl.getById(posteDto.getId());
            poste.setCreatedAt(oldPoste.getCreatedAt());
            poste.setUpdatedAt(LocalDateTime.now());

            if (posteDto.isDeleted()) {
                poste.setDeleted(posteDto.isDeleted());
                poste.setDeletedAt(LocalDateTime.now());
            }

        } else {
            poste.setCreatedAt(LocalDateTime.now());
            poste.setDeleted(false);
        }
        return poste;
    }

}
