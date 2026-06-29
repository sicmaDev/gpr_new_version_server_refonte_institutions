package com.sicmagroup.gpr.api.config.setting;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sicmagroup.gpr.domain.dto.ApiResponseDto;
import com.sicmagroup.gpr.domain.dto.ErrorResponse;
import com.sicmagroup.gpr.domain.enumeration.ConfigExportEnum;
import com.sicmagroup.gpr.domain.model.ApiKey;
import com.sicmagroup.gpr.domain.model.Setting;
import com.sicmagroup.gpr.domain.model.User;
import com.sicmagroup.gpr.repository.ApiKeyRepository;
import com.sicmagroup.gpr.service.MailService;
import com.sicmagroup.gpr.service.auth.AuthenticationService;
import com.sicmagroup.gpr.service.setting.SettingServiceImpl;
import com.sicmagroup.gpr.utils.Constante;
import com.sicmagroup.gpr.utils.Utils;
import com.sicmagroup.gpr.domain.enumeration.LogTarget;
import com.sicmagroup.gpr.domain.enumeration.LogType;
import com.sicmagroup.gpr.domain.model.Log;
import com.sicmagroup.gpr.service.log.LogServiceImpl;

import jakarta.annotation.security.RolesAllowed;
import lombok.RequiredArgsConstructor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.apache.tomcat.util.bcel.classfile.Constant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/api/v1/config/setting")
@RequiredArgsConstructor
@RolesAllowed("H12")
public class SettingController {

    private final SettingServiceImpl serviceImpl;
    private final AuthenticationService authService;
    private final PasswordEncoder passwordEncoder;
    private final ApiKeyRepository apiKeyRepository;
    private final MailService mailService;
    private final LogServiceImpl logService;
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
            File file = new File("data.txt");
            if (file.exists()) {

                FileReader fr = new FileReader(file);
                BufferedReader br = new BufferedReader(fr);
                StringBuffer sb = new StringBuffer();
                String line;
                while ((line = br.readLine()) != null) {
                    // ajoute la ligne au buffer
                    sb.append(line);
                    sb.append("\n");
                }
                fr.close();
                String license = sb.toString();
                ObjectMapper mapper = new ObjectMapper();
                JsonNode licenseObj = mapper.readTree("" + license + "");
                if (licenseObj != null && license != "") {
                    String oldEmail = licenseObj.get("email").asText();
                    if (data.contains(oldEmail)) {
                        FileWriter fw = new FileWriter(file);

                        fw.write(data);
                        fw.close();
                        return "Le texte a été écrit avec succès";
                    } else {
                        return "Information de licence invalide";
                    }
                } else {
                    return "Information de licence invalide";
                }

            } else {
                FileWriter fw = new FileWriter("data.txt");

                fw.write(data);
                fw.close();
                return "Le texte a été écrit avec succès";
            }

        } catch (IOException e) {
            e.printStackTrace();
            return e.getMessage();
        }

    }

    @PostMapping(value = "/others/mail/create")
    public ResponseEntity<ApiResponseDto> configMail(@RequestBody MailRequest request) {
        ObjectMapper Obj = new ObjectMapper();

        try {
            Setting settingOld = serviceImpl.getbySlug(Constante.MAIL_SLUG);
            String jsonStr = Obj.writeValueAsString(request);
            UpdateSettingRequest majSettingRequest = UpdateSettingRequest.builder()
                    .libelle(Constante.MAIL_SLUG)
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
                            .libelle(Constante.MAIL_SLUG)
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

    @PostMapping(value = "/others/sms/create")
    public ResponseEntity<ApiResponseDto> configSms(@RequestBody SmsRequest request) {
        ObjectMapper Obj = new ObjectMapper();

        try {
            Setting settingOld = serviceImpl.getbySlug(Constante.SMS_SLUG);
            String jsonStr = Obj.writeValueAsString(request);
            UpdateSettingRequest majSettingRequest = UpdateSettingRequest.builder()
                    .libelle(Constante.SMS_SLUG)
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
                            .libelle(Constante.SMS_SLUG)
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

    @PostMapping(value = "/others/sms/test")
    public ResponseEntity<ApiResponseDto> testSms(@RequestBody SmsTestRequest request) {
        ObjectMapper Obj = new ObjectMapper();

        try {

            Boolean isSuccess = Utils.testSmsConfig(request.getPhone(), request.getMessage(), serviceImpl);
            if (!isSuccess) {
                throw new Exception("SMS non envoyé");
            }
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
            ApiResponseDto apiResponseDto = ApiResponseDto
                    .builder()
                    .status(true)
                    .content(ErrorResponse.builder().title("Une erreur est survenue").message(e.getMessage()).build())
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponseDto);

        }

    }

    @PostMapping(value = "/others/sms/sendSmsToClient")
    public ResponseEntity<ApiResponseDto> sendSmsToClient(@RequestBody SmsTestRequest request) {
        ObjectMapper Obj = new ObjectMapper();

        System.out.println("sendSmsToClient called with request: " + request);
        try {
            Boolean isSuccess = Utils.sendSmsToClient(request.getPhone(), request.getMessage(), serviceImpl);
            if (!isSuccess) {
                throw new Exception("SMS non envoyé");
            }
            
            System.out.println("sendSmsToClient2 called with request: " + request);
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
            ApiResponseDto apiResponseDto = ApiResponseDto
                    .builder()
                    .status(true)
                    .content(ErrorResponse.builder().title("Une erreur est survenue").message(e.getMessage()).build())
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponseDto);

        }
    }

    @PostMapping(value = "/others/mail/sendMailToClient")
    public ResponseEntity<ApiResponseDto> sendMailToClient(@RequestBody EmailRequest request) {
        System.out.println("sendMailToClient called with request: " + request);
        try {
           
            mailService.sendMail(
                request.getEmail(),
                request.getSubject(),
                request.getMessage(),
                null
            );

            ApiResponseDto apiResponseDto = ApiResponseDto
                    .builder()
                    .status(true)
                    .content(request)
                    .build();
            return ResponseEntity.ok(apiResponseDto);

        } catch (Exception e) {
            ApiResponseDto apiResponseDto = ApiResponseDto
                    .builder()
                    .status(false)
                    .content(ErrorResponse.builder()
                        .title("Une erreur est survenue")
                        .message(e.getMessage())
                        .build())
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponseDto);
        }
    }
   

    @PostMapping(value = "/others/mail/test")
    public ResponseEntity<ApiResponseDto> testMail(@RequestBody MailTestRequest request) {

        try {
            Boolean isSuccess = Utils.testMailConfig(request.getTo(), request.getSubject(), request.getMessage(), null,
                    " ", serviceImpl);
            if (!isSuccess) {
                throw new Exception("Mail non envoyé");
            }
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
            ApiResponseDto apiResponseDto = ApiResponseDto
                    .builder()
                    .status(true)
                    .content(ErrorResponse.builder().title("Une erreur est survenue").message(e.getMessage()).build())
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponseDto);

        }

    }

    @PostMapping(value = "/others/bot/create")
    public ResponseEntity<ApiResponseDto> botSms(@RequestBody BotRequest request) {
        ObjectMapper Obj = new ObjectMapper();

        try {
            Setting settingOld = serviceImpl.getbySlug(Constante.BOT_SLUG);
            String jsonStr = Obj.writeValueAsString(request);
            UpdateSettingRequest majSettingRequest = UpdateSettingRequest.builder()
                    .libelle(Constante.BOT_SLUG)
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
                            .libelle(Constante.BOT_SLUG)
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

    @GetMapping(value = "/export/{type}")
    public ResponseEntity<ApiResponseDto> exportConfig(
            @PathVariable(name = "type", required = true) ConfigExportEnum type) {
        try {
            HashMap<String, Object> settingExport = authService.exportConfig(type);

            try {
                org.springframework.security.core.Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                String email = auth != null ? auth.getName() : null;
                String displayName = email != null ? authService.getUserDisplayName(email) : null;
                logService.saveLog(Log.builder()
                    .libelle("Exportation " + type.name().toUpperCase())
                    .content(type.name())
                    .target(LogTarget.EXPORT)
                    .type(LogType.INFO)
                    .userIpAddress(displayName)
                    .createdAt(java.time.LocalDateTime.now())
                    .build());
            } catch (Exception ignored) {}

            ApiResponseDto apiResponseDto = ApiResponseDto
                    .builder()
                    .status(true)
                    .content(settingExport)
                    .build();
            return ResponseEntity.ok(apiResponseDto);

        } catch (Exception e) {
            ApiResponseDto apiResponseDto = ApiResponseDto
                    .builder()
                    .status(true)
                    .content(ErrorResponse.builder().title("Une erreur est survenue").message(e.getMessage())
                            .build())
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponseDto);
        }
    }

    @GetMapping(value = "/key")
    public ResponseEntity<ApiResponseDto> apiKeys() {

        List<ApiKey> apiKeys = apiKeyRepository.findAll();
        try {

           

            ApiResponseDto apiResponseDto = ApiResponseDto
                    .builder()
                    .status(true)
                    .content(apiKeys)
                    .build();
            return ResponseEntity.ok(apiResponseDto);

        }

        catch (Exception e) {

            ApiResponseDto apiResponseDto = ApiResponseDto
                    .builder()
                    .status(true)
                    .content(ErrorResponse.builder().title("Une erreur est survenue").message(e.getMessage())
                            .build())
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponseDto);

        }

    }
    
    @PostMapping(value = "/key/generate")
    public ResponseEntity<ApiResponseDto> generateApiKey(@RequestBody ApiKeyRequest request) {

        ApiKey apiKey = ApiKey.builder().build();
        try {

            String api_key = generateRandomString(6);
            String api_secret = generateRandomString(6);

            apiKey.setCle(api_key);
            apiKey.setName(request.getLibelle());
            apiKey.setDescription(request.getDescription());
            apiKey.setSecret(passwordEncoder.encode(api_secret));

            apiKeyRepository.save(apiKey);

            HashMap<String, String> data = new HashMap<>();
            data.put("api_key", api_key);
            data.put("api_secret", api_secret);

            ApiResponseDto apiResponseDto = ApiResponseDto
                    .builder()
                    .status(true)
                    .content(data)
                    .build();
            return ResponseEntity.ok(apiResponseDto);

        }

        catch (Exception e) {

            ApiResponseDto apiResponseDto = ApiResponseDto
                    .builder()
                    .status(true)
                    .content(ErrorResponse.builder().title("Une erreur est survenue").message(e.getMessage())
                            .build())
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponseDto);

        }

    }

    @DeleteMapping(value = "key/{id}/delete")
    public ResponseEntity<ApiResponseDto> deleteApiKey(@PathVariable(name = "id") Long id) {
        ApiResponseDto apiResponseDto;
        try {
            ApiKey apiKey = apiKeyRepository.findById(id).orElseThrow(() -> new Exception("No found"));

            apiKeyRepository.delete(apiKey);
            apiResponseDto = ApiResponseDto
                    .builder()
                    .status(true)
                    .content("Api Key supprimée")
                    .build();

        } catch (Exception e) {
            apiResponseDto = ApiResponseDto
                    .builder()
                    .status(false)
                    .content(e.getMessage())
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponseDto);
        }
        return ResponseEntity.ok(apiResponseDto);
    }

    @PutMapping(value = "key/generate/{id}")
    public ResponseEntity<ApiResponseDto> regenerateApiKey(@PathVariable(name = "id") Long id) {
         
        try {
            ApiKey apiKey = apiKeyRepository.findById(id).orElseThrow(() -> new Exception("No found"));
            
            String api_secret = generateRandomString(6);
            apiKey.setSecret(passwordEncoder.encode(api_secret));

            apiKeyRepository.save(apiKey);

            HashMap<String, String> data = new HashMap<>();
            data.put("api_key", apiKey.getCle());
            data.put("api_secret", api_secret);

            ApiResponseDto apiResponseDto = ApiResponseDto
                    .builder()
                    .status(true)
                    .content(data)
                    .build();
            return ResponseEntity.ok(apiResponseDto);

        } catch (Exception e) {
            ApiResponseDto apiResponseDto = ApiResponseDto
                    .builder()
                    .status(false)
                    .content(e.getMessage())
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponseDto);
        }
    }

    private static String generateRandomString(int count) {
        String SALTCHARS = "ABCDEFGHIJKLMNOPQRST_abcdefghijkklmnopqrstuv:@uUVWXYZ1234567890";
        StringBuilder salt = new StringBuilder();
        Random rnd = new Random();
        while (salt.length() < count) { // length of the random string.
            int index = (int) (rnd.nextFloat() * SALTCHARS.length());
            salt.append(SALTCHARS.charAt(index));
        }
        String saltStr = salt.toString();
        return saltStr;

    }

}
