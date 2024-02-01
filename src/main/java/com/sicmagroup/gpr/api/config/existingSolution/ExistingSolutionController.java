package com.sicmagroup.gpr.api.config.existingSolution;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sicmagroup.gpr.domain.dto.ApiResponseDto;
import com.sicmagroup.gpr.domain.dto.ErrorResponse;
import com.sicmagroup.gpr.domain.dto.ExistingSolutionDto;
import com.sicmagroup.gpr.domain.dto.ObjetDto;
import com.sicmagroup.gpr.domain.model.ExistingSolution;
import com.sicmagroup.gpr.domain.model.Objet;
import com.sicmagroup.gpr.service.existingSolution.ExistingSolutionServiceImpl;

import lombok.RequiredArgsConstructor;

import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
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
@RequestMapping("api/v1/config/existing_solution")
@RequiredArgsConstructor

public class ExistingSolutionController {
    private final ExistingSolutionServiceImpl serviceImpl;
        private final ModelMapper modelMapper;


    @GetMapping(value="/list")
    public ResponseEntity<ApiResponseDto> getExistingSolutionList() {
        ApiResponseDto apiResponseDto = ApiResponseDto
            .builder()
            .status(true)
            .content(serviceImpl.getAll().stream().map(this::convertToDto).collect(Collectors.toList()))
            .build();
            // System.err.println(apiResponseDto.getContent().);
        return ResponseEntity.ok(apiResponseDto);
    }


    @PostMapping(value="/add")
    public ResponseEntity<ApiResponseDto> saveExistingSolution(@RequestBody AddExistingSolutionRequest request) {
        
        ApiResponseDto apiResponseDto;
        try {
            ExistingSolution existingSolution = serviceImpl.saveOne(request);
            apiResponseDto = ApiResponseDto
                .builder()
                .status(true)
                .content(this.convertToDto(existingSolution) )
                .build();
                return ResponseEntity.ok(apiResponseDto);

        } catch (Exception e) {
            apiResponseDto = ApiResponseDto
                .builder()
                .status(false)
                .content(ErrorResponse.builder().message(e.getMessage()).title("Something wrong").build())
                .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
        }
        
        
    }

    @PutMapping(value="/{id}/update")
    public ResponseEntity<ApiResponseDto>  updateExistingSolution(@RequestBody UpdateExistingSolutionRequest request) {
        
        
         ApiResponseDto apiResponseDto;
        try {
            ExistingSolution existingSolution = serviceImpl.updateOne(request);
            apiResponseDto = ApiResponseDto
                .builder()
                .status(true)
                .content(this.convertToDto(existingSolution))
                .build();

        } catch (Exception e) {
            apiResponseDto = ApiResponseDto
                .builder()
                .status(false)
                .content(ErrorResponse.builder().message(e.getMessage()).title("Something wrong").build())
                .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
        }
        
        return ResponseEntity.ok(apiResponseDto);
    }
    

    @DeleteMapping(value = "/{id}/delete")
    public ResponseEntity<ApiResponseDto> deleteExistingSolution(@PathVariable Long id){
          ApiResponseDto apiResponseDto;  
        try {
            serviceImpl.removeOne(id);
           apiResponseDto = ApiResponseDto
                .builder()
                .status(true)
                .content("Solution supprimée avec succès")
                .build();

        } catch (Exception e) {
            apiResponseDto = ApiResponseDto
                .builder()
                .status(false)
                .content(ErrorResponse.builder().message(e.getMessage()).title("Something wrong").build())
                .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
        }
      

          return ResponseEntity.ok(apiResponseDto);
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

       private ExistingSolutionDto convertToDto(ExistingSolution solution) {
//        System.out.println(solution);
        ExistingSolutionDto existingSolutionDto = modelMapper.map(solution, ExistingSolutionDto.class);
        if (solution.getObjet() != null) {
            existingSolutionDto.setObjetDto(convertToDto(solution.getObjet()));
        }
        
        return existingSolutionDto;
    }
    
}
