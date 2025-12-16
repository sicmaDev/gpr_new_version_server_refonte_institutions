package com.sicmagroup.gpr.api.config.servicePoint;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
import com.sicmagroup.gpr.domain.dto.ServicePointDto;
import com.sicmagroup.gpr.domain.model.ServicePoint;
import com.sicmagroup.gpr.repository.ServicePointRepository;
import com.sicmagroup.gpr.repository.SettingRepository;
import com.sicmagroup.gpr.service.servicePoint.ServicePointServiceImpl;

import jakarta.annotation.security.RolesAllowed;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/config/service_point")
@RequiredArgsConstructor
public class ServicePointController {

    private final ServicePointServiceImpl serviceImpl;
    private final ModelMapper modelMapper;

    @GetMapping("/list")
    public ResponseEntity<ApiResponseDto> list() {
        ApiResponseDto apiResponseDto;

        List<ServicePoint> allServicePoints = serviceImpl.all();
        apiResponseDto = ApiResponseDto.builder()
                .status(true)
                .content(allServicePoints.stream().map(this::convertToDto).collect(Collectors.toList()))
                .build();
        return ResponseEntity.ok(apiResponseDto);

    }

    @DeleteMapping("/disabled/{id}/{isDisabled}")
    public ResponseEntity<ApiResponseDto> disabled(@PathVariable(name = "id", required = true) Long id,@PathVariable(name = "isDisabled", required = true) boolean isDisabled) {
        ApiResponseDto apiResponseDto;
        try {
            ServicePoint servicePoint;
            if(isDisabled){
                servicePoint = serviceImpl.deleteTempServicePoint(id);
                
            }else{
                servicePoint = serviceImpl.enableServicePoint(id);
            }
            apiResponseDto = ApiResponseDto.builder()
                    .status(true)
                    .content(convertToDto(servicePoint))
                    .build();
            return ResponseEntity.ok(apiResponseDto);
        } catch (Exception e) {
            apiResponseDto = ApiResponseDto
                    .builder()
                    .status(false)
                    .content(ErrorResponse.builder().title("NOT FOUND").message("Service Point not found").build())
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
        }

    }

    @GetMapping("/list/{deleted}")
    public ResponseEntity<ApiResponseDto> getAll(@PathVariable(name = "deleted", required = false) boolean deleted) {
        ApiResponseDto apiResponseDto;
        if (deleted) {
            List<ServicePoint> allServicePoints = serviceImpl.getAllDeleted();
            apiResponseDto = ApiResponseDto.builder()
                    .status(true)
                    .content(allServicePoints.stream().map(this::convertToDto).collect(Collectors.toList()))
                    .build();
            return ResponseEntity.ok(apiResponseDto);
        } else {
            List<ServicePoint> allServicePoints = serviceImpl.getAll();
            apiResponseDto = ApiResponseDto.builder()
                    .status(true)
                    .content(allServicePoints.stream().map(this::convertToDto).collect(Collectors.toList()))
                    .build();
            return ResponseEntity.ok(apiResponseDto);
        }

    }

    @GetMapping("/{id}/{deleted}")
    public ResponseEntity<ApiResponseDto> get(@PathVariable(name = "id", required = true) Long id,
            @PathVariable(name = "deleted", required = false) boolean deleted) {
        ApiResponseDto apiResponseDto;
        ServicePoint servicePoint;
        try {
            servicePoint = serviceImpl.getDeletedById(id, deleted);
            apiResponseDto = ApiResponseDto.builder()
                    .status(true)
                    .content(convertToDto(servicePoint))
                    .build();
            return ResponseEntity.ok(apiResponseDto);
        } catch (NotFoundException e) {
            e.printStackTrace();
            apiResponseDto = ApiResponseDto
                    .builder()
                    .status(false)
                    .content(ErrorResponse.builder().title("NOT FOUND").message("Service Point not found").build())
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
        }

    }

    @PostMapping("/add")
    public ResponseEntity<ApiResponseDto> addServicePoint(@RequestBody ServicePointDto servicePointDto) {
        ApiResponseDto apiResponseDto;
        ServicePoint servicePoint;
        try {
            servicePoint = serviceImpl.saveServicePoint(convertFromDtoToEntity(servicePointDto));
            apiResponseDto = ApiResponseDto.builder()
                    .status(true)
                    .content(convertToDto(servicePoint))
                    .build();
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
                    .content(ErrorResponse.builder().title("NOT FOUND").message("flemme").build())
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
        }

        return ResponseEntity.ok(apiResponseDto);
    }

    @PutMapping("/{id}/update")
    public ResponseEntity<ApiResponseDto> updateServicePoint(@PathVariable(name = "id") Long id,
            @RequestBody ServicePointDto servicePointDto) {
        ApiResponseDto apiResponseDto;
        Long idParsed = id.longValue();
        Long serviceId = servicePointDto.getId().longValue();
        if (!idParsed.equals(serviceId)) {
            throw new IllegalArgumentException("Les ID ne correspondent pas");
        } else {
            ServicePoint servicePoint;
            try {
                servicePoint = serviceImpl.updateServicePoint(convertFromDtoToEntity(servicePointDto));
                apiResponseDto = ApiResponseDto
                        .builder()
                        .status(true)
                        .content(convertToDto(servicePoint))
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
                        .content(ErrorResponse.builder().title("NOT FOUND").message("Service Point not foundz").build())
                        .build();
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
            }

        }

    }

    @DeleteMapping("/{id}/delete_temp")
    public ResponseEntity<ApiResponseDto> deleteTempServicePoint(@PathVariable(name = "id") Long id) {
        ApiResponseDto apiResponseDto;
        try {
            ServicePoint servicePoint = serviceImpl.deleteTempServicePoint(id);
            apiResponseDto = ApiResponseDto
                    .builder()
                    .status(true)
                    .content(convertToDto(servicePoint))
                    .build();
            return ResponseEntity.ok(apiResponseDto);
        } catch (NotFoundException e) {

            apiResponseDto = ApiResponseDto
                    .builder()
                    .status(false)
                    .content(ErrorResponse.builder().title("NOT FOUND").message("Service Point not found").build())
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
        }
    }

    @DeleteMapping("/{id}/delete")
    public ResponseEntity<ApiResponseDto> deleteServicePoint(@PathVariable(name = "id") Long id) {
        ApiResponseDto apiResponseDto;
        ServicePoint servicePoint = new ServicePoint();

        try {
            servicePoint = serviceImpl.getById(id);
        } catch (Exception e) {
            apiResponseDto = ApiResponseDto
                    .builder()
                    .status(false)
                    .content(ErrorResponse
                            .builder()
                            .title("NOT FOUND")
                            .message("Point de service introuvable")
                            .build())
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
        }

        if (servicePoint.getClaims().isEmpty() && servicePoint.getUsers().isEmpty()) {
            try {
                serviceImpl.deleteServicePoint(servicePoint);
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
                                    "Le point de service intervient dans d'autres objets : Utilisateurs et/ou réclamations")
                            .build())
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponseDto);
        }

    }

    private ServicePointDto convertToDto(ServicePoint servicepoint1) {
        ServicePointDto servicePointDto = modelMapper.map(servicepoint1, ServicePointDto.class);
        return servicePointDto;
    }

    private ServicePoint convertFromDtoToEntity(ServicePointDto servicePointDto) throws Exception {
        ServicePoint servicePoint = modelMapper.map(servicePointDto, ServicePoint.class);

        if (servicePointDto.getId() != null) {
            ServicePoint oldServicePoint = serviceImpl.getById(servicePointDto.getId());
            servicePoint.setUuid(oldServicePoint.getUuid());
            servicePoint.setCreatedAt(oldServicePoint.getCreatedAt());
            servicePoint.setUpdatedAt(LocalDateTime.now());

        } else {
            servicePoint.setCreatedAt(LocalDateTime.now());
            servicePoint.setDeleted(false);
            // servicePoint.setDirection_id(servicePointDto.getDirection_id());
        }
        return servicePoint;
    }

}
