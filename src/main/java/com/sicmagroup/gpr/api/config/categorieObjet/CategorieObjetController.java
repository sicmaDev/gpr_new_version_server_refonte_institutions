package com.sicmagroup.gpr.api.config.categorieObjet;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sicmagroup.gpr.domain.dto.ApiResponseDto;
import com.sicmagroup.gpr.domain.dto.CategorieObjetDto;
import com.sicmagroup.gpr.domain.dto.ErrorResponse;
import com.sicmagroup.gpr.domain.model.CategorieObjet;
import com.sicmagroup.gpr.service.categorieObjet.CategorieObjetServiceImpl;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/api/v1/config/categorie_objet")
@RequiredArgsConstructor
public class CategorieObjetController {
    private final CategorieObjetServiceImpl service;
    private final ModelMapper modelMapper;

    @GetMapping("/list")
    public ResponseEntity<ApiResponseDto> getAllCategorie() {
        List<CategorieObjet> allCategorieObjets = service.getAll();
        ApiResponseDto apiResponseDto = ApiResponseDto
                .builder()
                .status(true)
                .content(allCategorieObjets.stream().map(this::convertToDto).collect(Collectors.toList()))
                .build();
        return ResponseEntity.ok(apiResponseDto);
    }

    @PostMapping("/add")
    public ResponseEntity<ApiResponseDto> saveCategorie(@RequestBody CategorieObjetRequest request) {
        ApiResponseDto apiResponseDto;
        CategorieObjet categorieObjet;
        try {           
            categorieObjet = service.saveOne(request);

            apiResponseDto = ApiResponseDto
                    .builder()
                    .status(true)
                    .content(this.convertToDto(categorieObjet))
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
        } catch (Exception e) {
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
    public ResponseEntity<ApiResponseDto> updateCategorie(@PathVariable(name = "id") Long id,
            @RequestBody UpdateCategorieObjetRequest request) {
        ApiResponseDto apiResponseDto;
        try {
            request.setId(id);
            CategorieObjet categorieObjet = service.updateOne(request);
            apiResponseDto = ApiResponseDto
                    .builder()
                    .status(true)
                    .content(this.convertToDto(categorieObjet))
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
        } catch (Exception e) {
            e.printStackTrace();
            apiResponseDto = ApiResponseDto
                    .builder()
                    .status(false)
                    .content(ErrorResponse.builder().title("NOT FOUND").message(e.getMessage())
                            .build())
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
        }

    }

    @DeleteMapping("/{id}/delete")
    public ResponseEntity<ApiResponseDto> removeCategorie(@PathVariable(name = "id") Long id) {
        ApiResponseDto apiResponseDto;
        try {
            service.removeOne(id);
            apiResponseDto = ApiResponseDto
                    .builder()
                    .status(true)
                    .content("Suppression réussie")
                    .build();
            return ResponseEntity.ok(apiResponseDto);
        } catch (Exception e) {
            e.printStackTrace();
            apiResponseDto = ApiResponseDto
                    .builder()
                    .status(false)
                    .content(ErrorResponse.builder().title("NOT FOUND").message(e.getMessage())
                            .build())
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
        }

    }

    private CategorieObjetDto convertToDto(CategorieObjet categorieObjet) {
        CategorieObjetDto dto = modelMapper.map(categorieObjet, CategorieObjetDto.class);
        return dto;
    }
}
