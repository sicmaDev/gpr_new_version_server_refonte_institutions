package com.sicmagroup.gpr.api.config.collectionChannel;

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
import com.sicmagroup.gpr.domain.dto.CollectionChannelDto;
import com.sicmagroup.gpr.domain.dto.ErrorResponse;
import com.sicmagroup.gpr.domain.model.CollectionChannel;
import com.sicmagroup.gpr.service.collectionChannel.CollectionChannelServiceImpl;

import jakarta.annotation.security.RolesAllowed;
import lombok.RequiredArgsConstructor;

@RestController
@CrossOrigin

@RequestMapping("/api/v1/config/collection_channel")
@RequiredArgsConstructor
@RolesAllowed("H12")
public class CollectionChannelController {
    private final ModelMapper modelMapper;
    private final CollectionChannelServiceImpl service;

    @GetMapping("/list/{deleted}")
    public ResponseEntity<ApiResponseDto> getAll(@PathVariable(name = "deleted", required = false) boolean deleted) {
        ApiResponseDto apiResponseDto;
        if (deleted) {
            List<CollectionChannel> allCollectionChannels = service.getAllDeleted();
            apiResponseDto = ApiResponseDto.builder()
                    .status(true)
                    .content(allCollectionChannels.stream().map(this::convertToDto).collect(Collectors.toList()))
                    .build();
            return ResponseEntity.ok(apiResponseDto);
        } else {
            List<CollectionChannel> allCollectionChannels = service.getAll();
            apiResponseDto = ApiResponseDto.builder()
                    .status(true)
                    .content(allCollectionChannels.stream().map(this::convertToDto).collect(Collectors.toList()))
                    .build();
            return ResponseEntity.ok(apiResponseDto);
        }

    }

    @GetMapping("/{id}/{deleted}")
    public ResponseEntity<ApiResponseDto> get(@PathVariable(name = "id", required = true) Long id,
            @PathVariable(name = "deleted", required = false) boolean deleted) {
        ApiResponseDto apiResponseDto;
        CollectionChannel collectionChannel;
        try {
            collectionChannel = service.getDeletedById(id, deleted);
            apiResponseDto = ApiResponseDto.builder()
                    .status(true)
                    .content(convertToDto(collectionChannel))
                    .build();
            return ResponseEntity.ok(apiResponseDto);
        } catch (NotFoundException e) {
            e.printStackTrace();
            apiResponseDto = ApiResponseDto
                    .builder()
                    .status(false)
                    .content(ErrorResponse.builder().title("NOT FOUND").message("collectionChannel not found").build())
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
        }

    }

    @PostMapping("/add")
    public ResponseEntity<ApiResponseDto> addCollectionChannel(@RequestBody CollectionChannelDto collectionChannelDto) {
        ApiResponseDto apiResponseDto;

        CollectionChannel collectionChannel;
        try {
            collectionChannel = service
                    .saveCollectionChannel(convertFromDtoToEntity(collectionChannelDto));
            apiResponseDto = ApiResponseDto.builder()
                    .status(true)
                    .content(convertToDto(collectionChannel))
                    .build();

            return ResponseEntity.ok(apiResponseDto);
        } catch (NotFoundException e) {
            e.printStackTrace();
            apiResponseDto = ApiResponseDto
                    .builder()
                    .status(false)
                    .content(ErrorResponse.builder().title("NOT FOUND").message("collectionChannel not found").build())
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
        }

    }

    @PutMapping("/{id}/update")
    public ResponseEntity<ApiResponseDto> updateCollectionChannel(@PathVariable(name = "id") Long id,
            @RequestBody CollectionChannelDto collectionChannelDto) {
        ApiResponseDto apiResponseDto;
        Long idParsed = id.longValue();
        Long canalId = collectionChannelDto.getId().longValue();
        if (!idParsed.equals(canalId)) {
            throw new IllegalArgumentException("Les ID ne correspondent pas");
        } else {
            CollectionChannel collectionChannel;
            try {
                collectionChannel = service
                        .updateCollectionChannel(convertFromDtoToEntity(collectionChannelDto));
                apiResponseDto = ApiResponseDto
                        .builder()
                        .status(true)
                        .content(convertToDto(collectionChannel))
                        .build();

                return ResponseEntity.ok(apiResponseDto);
            } catch (NotFoundException e) {
                e.printStackTrace();
                apiResponseDto = ApiResponseDto
                        .builder()
                        .status(false)
                        .content(ErrorResponse.builder().title("NOT FOUND").message("collectionChannel not found")
                                .build())
                        .build();
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
            }

        }

    }

    @DeleteMapping("/{id}/delete_temp")
    public ResponseEntity<ApiResponseDto> deleteTempCollectionChannel(@PathVariable(name = "id") Long id) {
        ApiResponseDto apiResponseDto;
        try {
            CollectionChannel collectionChannel = service.deleteTempCollectionChannel(id);
            apiResponseDto = ApiResponseDto
                    .builder()
                    .status(true)
                    .content(convertToDto(collectionChannel))
                    .build();
            return ResponseEntity.ok(apiResponseDto);
        } catch (NotFoundException e) {

            apiResponseDto = ApiResponseDto
                    .builder()
                    .status(false)
                    .content(ErrorResponse.builder().title("NOT FOUND").message("collectionChannel not found").build())
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);

        }
    }

    @DeleteMapping("/{id}/delete")
    public ResponseEntity<ApiResponseDto> deleteCollectionChannel(@PathVariable(name = "id") Long id) {
        ApiResponseDto apiResponseDto;
        CollectionChannel collectionChannel = new CollectionChannel();
        try {
            collectionChannel = service.getById(id);
        } catch (NotFoundException e) {
            apiResponseDto = ApiResponseDto
                    .builder()
                    .status(false)
                    .content(ErrorResponse
                            .builder()
                            .title("NOT FOUND")
                            .message("Collection channel not found")
                            .build())
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
        }

        if (collectionChannel.getClaims().isEmpty()) {
            try {
                service.deleteCollectionChannel(collectionChannel);
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
                                    "Le canal de collecte choisi intervient dans une réclamation et ne peut donc pas être supprimé.")
                            .build())
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponseDto);
        }

    }

    private CollectionChannelDto convertToDto(CollectionChannel collectionChannel) {
        CollectionChannelDto collectionChannelDto = modelMapper.map(collectionChannel, CollectionChannelDto.class);
        return collectionChannelDto;
    }

    private CollectionChannel convertFromDtoToEntity(CollectionChannelDto collectionChannelDto)
            throws NotFoundException {
        CollectionChannel collectionChannel = modelMapper.map(collectionChannelDto, CollectionChannel.class);

        if (collectionChannel.getId() != null) {
            CollectionChannel oldCollectionChannel = service.getById(collectionChannel.getId());
            // ServicePoint oldServicePoint = serviceImpl.getById(servicePointDto.getId());
            collectionChannel.setUpdatedAt(LocalDateTime.now());
            collectionChannel.setCreatedAt(oldCollectionChannel.getCreatedAt());

        } else {
            collectionChannel.setCreatedAt(LocalDateTime.now());
            collectionChannel.setDeleted(false);
        }
        return collectionChannel;
    }
}
