package com.sicmagroup.gpr.api.config.Objet;

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
import com.sicmagroup.gpr.domain.dto.CategorieObjetDto;
import com.sicmagroup.gpr.domain.dto.ErrorResponse;
import com.sicmagroup.gpr.domain.dto.ExistingSolutionResponse;
import com.sicmagroup.gpr.domain.dto.ObjetDto;
import com.sicmagroup.gpr.domain.dto.claimResponse.ObjetResponse;
import com.sicmagroup.gpr.domain.model.CategorieObjet;
import com.sicmagroup.gpr.domain.model.ExistingSolution;
import com.sicmagroup.gpr.domain.model.Objet;
import com.sicmagroup.gpr.service.categorieObjet.CategorieObjetServiceImpl;
import com.sicmagroup.gpr.service.objet.ObjetServcieImpl;

import jakarta.annotation.security.RolesAllowed;
import lombok.RequiredArgsConstructor;

@RestController
@CrossOrigin

@RequestMapping("/api/v1/config/objet")
@RequiredArgsConstructor
@RolesAllowed("H12")
public class ObjetController {
    private final ModelMapper modelMapper;
    private final ObjetServcieImpl service;
    private final CategorieObjetServiceImpl categorieObjetServiceImpl;

    @GetMapping("/list/{deleted}")
    public ResponseEntity<ApiResponseDto> getAll(@PathVariable(name = "deleted", required = false) boolean deleted) {
        ApiResponseDto apiResponseDto;
        if (deleted) {
            List<Objet> allObjets = service.getAllDeleted();
            apiResponseDto = ApiResponseDto.builder()
                    .status(true)
                    .content(allObjets.stream().map(this::convertToResponse).collect(Collectors.toList()))
                    .build();
            return ResponseEntity.ok(apiResponseDto);
        } else {
            List<Objet> allObjets = service.getAll();
            apiResponseDto = ApiResponseDto.builder()
                    .status(true)
                    .content(allObjets.stream().map(this::convertToResponse).collect(Collectors.toList()))
                    .build();
            return ResponseEntity.ok(apiResponseDto);
        }

    }

    @GetMapping("/{id}/{deleted}")
    public ResponseEntity<ApiResponseDto> get(@PathVariable(name = "id", required = true) Long id,
            @PathVariable(name = "deleted", required = false) boolean deleted) {
        ApiResponseDto apiResponseDto;
        Objet objet;
        try {
            objet = service.getDeletedById(id, deleted);
            apiResponseDto = ApiResponseDto.builder()
                    .status(true)
                    .content(convertToResponse(objet))
                    .build();
            return ResponseEntity.ok(apiResponseDto);
        } catch (NotFoundException e) {
            e.printStackTrace();
            apiResponseDto = ApiResponseDto
                    .builder()
                    .status(false)
                    .content(ErrorResponse.builder().title("NOT FOUND").message("L'objet choisi n'existe pas").build())
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
        }

    }

    @PostMapping("/add")
    public ResponseEntity<ApiResponseDto> addObjet(@RequestBody ObjetDto objetDto) {
        ApiResponseDto apiResponseDto;

        Objet objet;
        CategorieObjet categorieObjet;
        try {
            categorieObjet = categorieObjetServiceImpl.getOneById(objetDto.getCategorie());
        } catch (Exception e) {
            e.printStackTrace();
            apiResponseDto = ApiResponseDto
                    .builder()
                    .status(false)
                    .content(ErrorResponse.builder().title("NOT FOUND").message(e.getMessage()).build())
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
        }
        try {
            objet = service
                    .saveObjet(convertFromDtoToEntity(objetDto));
            objet.setCategorie(categorieObjet);
            apiResponseDto = ApiResponseDto.builder()
                    .status(true)
                    .content(convertToResponse(objet))
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
                    .content(ErrorResponse.builder().title("NOT FOUND").message("L'objet choisi n'existe pas").build())
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
        }

    }

    @PutMapping("/{id}/update")
    public ResponseEntity<ApiResponseDto> updateObjet(@PathVariable(name = "id") Long id,
            @RequestBody ObjetDto objetDto) {
        ApiResponseDto apiResponseDto;
        Long idParsed = id.longValue();
        Long objetId = objetDto.getId().longValue();
        if (!idParsed.equals(objetId)) {
            throw new IllegalArgumentException("Les ID ne correspondent pas");
        } else {
            Objet objet;
            try {
                objet = service
                        .updateObjet(convertFromDtoToEntity(objetDto));
                apiResponseDto = ApiResponseDto
                        .builder()
                        .status(true)
                        .content(convertToResponse(objet))
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
                        .content(ErrorResponse.builder().title("NOT FOUND").message("L'objet choisi n'existe pas")
                                .build())
                        .build();
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
            }

        }

    }

    @DeleteMapping("/{id}/delete_temp")
    public ResponseEntity<ApiResponseDto> deleteTempObjet(@PathVariable(name = "id") Long id) {
        ApiResponseDto apiResponseDto;
        try {
            Objet objet = service.deleteTempObjet(id);
            apiResponseDto = ApiResponseDto
                    .builder()
                    .status(true)
                    .content(convertToDto(objet))
                    .build();
            return ResponseEntity.ok(apiResponseDto);
        } catch (NotFoundException e) {

            apiResponseDto = ApiResponseDto
                    .builder()
                    .status(false)
                    .content(ErrorResponse.builder().title("NOT FOUND").message("L'objet choisi n'existe pas").build())
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);

        }
    }

    @DeleteMapping("/{id}/delete")
    public ResponseEntity<ApiResponseDto> deleteProduct(@PathVariable(name = "id") Long id) {
        ApiResponseDto apiResponseDto;
        Objet objet = new Objet();

        try {
            objet = service.getById(id);
        } catch (Exception e) {
            apiResponseDto = ApiResponseDto
                    .builder()
                    .status(false)
                    .content(ErrorResponse
                            .builder()
                            .title("NOT FOUND")
                            .message("L'objet choisi n'existe pas")
                            .build())
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
        }

        if (objet.getClaims().isEmpty()) {
            try {
                service.deleteObjet(objet);
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
                            .message("L'Objet choisi intervient dans une réclamation et ne peut pas être supprimé")
                            .build())
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponseDto);
        }

    }

    private ObjetDto convertToDto(Objet objet) {
        ObjetDto objetDto = new ObjetDto();
        if (objet.getCategorie() != null) {
            objetDto.setCategorie(objet.getCategorie().getId());
        }

        objetDto.setCreatedAt(objet.getCreatedAt());
        objetDto.setUpdatedAt(objet.getUpdatedAt());
        objetDto.setDescription(objet.getDescription());
        objetDto.setLibelle(objet.getLibelle());
        objetDto.setRisqueLevel(objet.getRisqueLevel());
        objetDto.setProcessingTime(objet.getProcessingTime());
        objetDto.setId(objet.getId());
        return objetDto;
    }

    private ObjetResponse convertToResponse(Objet objet) {
        ObjetResponse objetResponse = modelMapper.map(objet, ObjetResponse.class);
        if (objet.getExistingSolutions() != null) {
            objetResponse.setExistingSolutions(
                    objet.getExistingSolutions().stream().map(this::convertToResponse).collect(Collectors.toList()));
        }

        if (objet.getCategorie() != null) {
            objetResponse.setCategorie(null);
            objetResponse.setCategorie(convertToDto(objet.getCategorie()));
        }

        return objetResponse;
    }

    private ExistingSolutionResponse convertToResponse(ExistingSolution exSolution) {
        ExistingSolutionResponse existingSolutionResponse = modelMapper.map(exSolution, ExistingSolutionResponse.class);
        return existingSolutionResponse;
    }

    private CategorieObjetDto convertToDto(CategorieObjet categorieObjet) {
        CategorieObjetDto dto = modelMapper.map(categorieObjet, CategorieObjetDto.class);
        return dto;
    }

    private Objet convertFromDtoToEntity(ObjetDto objetDto) throws NotFoundException {
        Objet objet = modelMapper.map(objetDto, Objet.class);
        if (objetDto.getCategorie() != null) {
            try {
                objet.setCategorie(categorieObjetServiceImpl.getOneById(objetDto.getCategorie()));
            } catch (Exception e) {
                e.printStackTrace();
                throw new NotFoundException();
            }
        }
        if (objet.getId() != null) {
            Objet oldObjet = service.getById(objet.getId());
            objet.setCreatedAt(oldObjet.getCreatedAt());
            objet.setUpdatedAt(LocalDateTime.now());

        } else {
            objet.setCreatedAt(LocalDateTime.now());
            objet.setDeleted(false);
        }
        return objet;
    }

}
