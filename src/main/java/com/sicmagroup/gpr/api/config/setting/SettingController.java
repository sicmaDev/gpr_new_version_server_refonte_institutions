package com.sicmagroup.gpr.api.config.setting;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sicmagroup.gpr.domain.dto.ApiResponseDto;
import com.sicmagroup.gpr.domain.dto.ErrorResponse;
import com.sicmagroup.gpr.domain.model.Setting;
import com.sicmagroup.gpr.service.setting.SettingServiceImpl;
import com.sicmagroup.gpr.utils.Constante;

import jakarta.annotation.security.RolesAllowed;
import lombok.RequiredArgsConstructor;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.apache.tomcat.util.bcel.classfile.Constant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/v1/config/setting")
@RequiredArgsConstructor
@RolesAllowed("H12")
public class SettingController {

    private final SettingServiceImpl serviceImpl;

    // @PostMapping(value="/institution/save")
    // public ResponseEntity<ApiResponseDto> configInstit(@RequestBody SomeEnityData
    // entity) {
    // //TODO: process POST request

    // return entity;
    // }

    @PostMapping(value = "/others")
    public ResponseEntity<ApiResponseDto> config(@RequestBody AddSettingRequest request) {
        Setting setting = serviceImpl.save(request);
        ApiResponseDto apiResponseDto = ApiResponseDto
                .builder()
                .status(true)
                .content(setting)
                .build();
        return ResponseEntity.ok(apiResponseDto);
    }

    @PostMapping(value = "/others/institution/create")
    public ResponseEntity<ApiResponseDto> configInstitution(@RequestBody InstitutionRequest request) {
        ObjectMapper Obj = new ObjectMapper();

        try {
            Setting settingOld = serviceImpl.getbySlug(Constante.INSTITUTION_SLUG);
            String jsonStr = Obj.writeValueAsString(request);
            UpdateSettingRequest majSettingRequest = UpdateSettingRequest.builder()
                    .libelle(Constante.INSTITUTION_SLUG)
                    .value(jsonStr)
                    .build();
            Setting setting = serviceImpl.update(majSettingRequest);
            ApiResponseDto apiResponseDto = ApiResponseDto
                    .builder()
                    .status(true)
                    .content(request)
                    .build();
            return ResponseEntity.ok(apiResponseDto);

        }

        // Catch block to handle exceptions
        catch (IOException e) {
            ApiResponseDto apiResponseDto = ApiResponseDto
                    .builder()
                    .status(true)
                    .content(ErrorResponse.builder().title("Une erreur est survenue").message(e.getMessage()).build())
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponseDto);
        } catch (Exception e) {
            if (e.getMessage().equals("The choosen setting doesn't exist")) {
                // Getting organisation object as a json string
                String jsonStr;
                try {
                    jsonStr = Obj.writeValueAsString(request);
                    AddSettingRequest addSettingRequest = AddSettingRequest.builder()
                            .libelle(Constante.INSTITUTION_SLUG)
                            .value(jsonStr)
                            .build();
                    Setting setting = serviceImpl.save(addSettingRequest);
                    ApiResponseDto apiResponseDto = ApiResponseDto
                            .builder()
                            .status(true)
                            .content(request)
                            .build();
                    return ResponseEntity.ok(apiResponseDto);
                } catch (JsonProcessingException e1) {
                    ApiResponseDto apiResponseDto = ApiResponseDto
                            .builder()
                            .status(true)
                            .content(ErrorResponse.builder().title("Une erreur est survenue").message(e.getMessage())
                                    .build())
                            .build();
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponseDto);
                }

            } else {
                ApiResponseDto apiResponseDto = ApiResponseDto
                        .builder()
                        .status(true)
                        .content(ErrorResponse.builder().title("Une erreur est survenue").message(e.getMessage())
                                .build())
                        .build();
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponseDto);
            }

        }

    }

    @PostMapping(value = "/others/institution/maj")
    public ResponseEntity<ApiResponseDto> majInstitution(@RequestBody InstitutionRequest request) {
        ObjectMapper Obj = new ObjectMapper();
        try {
            // Getting organisation object as a json string
            String jsonStr = Obj.writeValueAsString(request);
            UpdateSettingRequest majSettingRequest = UpdateSettingRequest.builder()
                    .libelle(Constante.INSTITUTION_SLUG)
                    .value(jsonStr)
                    .build();
            Setting setting = serviceImpl.update(majSettingRequest);
            ApiResponseDto apiResponseDto = ApiResponseDto
                    .builder()
                    .status(true)
                    .content(setting)
                    .build();
            return ResponseEntity.ok(apiResponseDto);
        }

        // Catch block to handle exceptions
        catch (IOException e) {
            ApiResponseDto apiResponseDto = ApiResponseDto
                    .builder()
                    .status(true)
                    .content(ErrorResponse.builder().title("Une erreur est survenue").message(e.getMessage()).build())
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponseDto);
        } catch (Exception e) {
            ApiResponseDto apiResponseDto = ApiResponseDto
                    .builder()
                    .status(true)
                    .content(ErrorResponse.builder().title("Une erreur est survenue").message(e.getMessage()).build())
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponseDto);
        }

    }

    @PutMapping(value = "/others/{app-slug}")
    public ResponseEntity<ApiResponseDto> configUpdate(@RequestParam(value = "app-slug") String slug,
            @RequestBody UpdateSettingRequest request) {
        Setting setting;
        try {
            setting = serviceImpl.update(request);
            ApiResponseDto apiResponseDto = ApiResponseDto
                    .builder()
                    .status(true)
                    .content(setting)
                    .build();
            return ResponseEntity.ok(apiResponseDto);
        } catch (Exception e) {
            ApiResponseDto apiResponseDto = ApiResponseDto
                    .builder()
                    .status(false)
                    .content(ErrorResponse.builder().title(slug).message(e.getMessage()).build())
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
        }

    }

    @PostMapping("/license/createFile")
    public String createLicenseFile(@RequestBody String data)
            throws FileNotFoundException, UnsupportedEncodingException {

        try {
            // ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
            // String json = ow.writeValueAsString(data);

            FileWriter fw = new FileWriter("data.txt");
            fw.write(data);
            fw.close();
            return "Le texte a été écrit avec succès";
        } catch (IOException e) {
            e.printStackTrace();
            return e.getMessage();
        }

    }

}
