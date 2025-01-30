package com.sicmagroup.gpr.api.auth;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.sicmagroup.gpr.api.claim.botClaim.MessageRequest;
import com.sicmagroup.gpr.api.config.user.ForgetPasswordRequest;
import com.sicmagroup.gpr.domain.dto.ApiResponseDto;
import com.sicmagroup.gpr.domain.dto.Client;
import com.sicmagroup.gpr.domain.dto.ErrorResponse;
import com.sicmagroup.gpr.domain.dto.LicenceDto;
import com.sicmagroup.gpr.domain.dto.LicenseResponse;
import com.sicmagroup.gpr.domain.model.User;
import com.sicmagroup.gpr.service.auth.AuthenticationServiceImpl;
import com.sicmagroup.gpr.utils.Utils;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationServiceImpl authenticationServiceImpl;

    @PostMapping("/authenticate")
    public ResponseEntity<AuthenticationResponse> authenticate(@RequestBody AuthenticationRequest request) {
        return ResponseEntity.ok(authenticationServiceImpl.authenticate(request));
    }
    @GetMapping("/testy")
    public String testy() {
         try {
            // Le fichier d'entrée
            File file = new File("data.txt");

            // Créer l'objet File Reader
            FileReader fr = new FileReader(file);
         
            // Créer l'objet BufferedReader
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
                String url = "https://gpradmin.sicmagroup.com/api/v1/license/updateLicenceClientSide";
                RestTemplate restTemplate = new RestTemplate();
                LicenseResponse licenseResponse = new LicenseResponse();
              
                licenseResponse.setSerial(licenseObj.get("serial").asText());
                try {
                    ResponseEntity<String> response = restTemplate.postForEntity(url, licenseResponse, String.class);
                    String responseBody = response.getBody();
        
                    // Affiche la réponse brute pour vérifier sa structure
                    System.out.println("Réponse brute de l'API : " + responseBody);
                      // Utiliser ObjectMapper pour analyser la réponse brute en un JsonNode
                    ObjectMapper objectMapper = new ObjectMapper();
                    JsonNode rootNode = objectMapper.readTree(responseBody);

                    // Accéder à une valeur spécifique (par exemple, "serial")
                    JsonNode reponse = rootNode;  // Si c'est un tableau, accès au premier élément
                    if (reponse != null) {
                        System.out.println("Serial15 : " + reponse.get("serial").asText());
                       
                            try {
                                ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
                                HashMap<String, Object> licenseMap = new HashMap<>();
                                licenseMap.put("id", reponse.get("id").asText());
                                licenseMap.put("fullname", reponse.get("denomination").asText());
                                licenseMap.put("company", reponse.get("denomination").asText());
                                licenseMap.put("serial", reponse.get("serial").asText());
                                licenseMap.put("email", reponse.get("email").asText());
                                licenseMap.put("activationRequest", reponse.get("activation_request").asText());
                                licenseMap.put("createdAt", reponse.get("createdAt").asText());
                                licenseMap.put("updatedAt", reponse.get("updatedAt").asText());
                                System.out.println(licenseMap);
                                String json = ow.writeValueAsString(licenseMap);
        
                                FileWriter fw = new FileWriter("data.txt");
                                fw.write(json);
                                fw.close();
        
                                System.out.println("Le texte a été écrit avec succès");
                                return "Le texte a été écrit avec succès";
                             
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                       
                    } else {
                        System.out.println("Le texte n'a été pas été écrit avec succès");
                        return "Le texte n'a été pas été écrit avec succès";
                    }
                   
                } catch (Exception e) {
                    System.err.println("Erreur de désérialisation : " + e.getMessage());
                    e.printStackTrace();
                }
                // System.out.println(result);
              
            } else {
                System.out.println("Une erreur est survenue.");
                return "Une erreur est survenue.";
            }
        } catch (NullPointerException eNullPointerException) {
            eNullPointerException.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "lol";
    }
    @PostMapping("/forget/password")
    public ResponseEntity<ApiResponseDto> forgetPassword(@RequestBody ForgetPasswordRequest request) {
           return authenticationServiceImpl.forgetPassword(request);
    }


    @GetMapping("/check/token")
    public ResponseEntity<ApiResponseDto> getAuthData() {
           return authenticationServiceImpl.getAuthData();
    }

    @PutMapping("/update")
    public ResponseEntity<ApiResponseDto> updateUser(@RequestBody UpdateRequest request) {
        ApiResponseDto apiResponseDto = new ApiResponseDto();
        try {
            authenticationServiceImpl.updateAccountUser(request);
            apiResponseDto.setContent("Success");
            apiResponseDto.setStatus(true);
        } catch (Exception e) {
            apiResponseDto.setContent(ErrorResponse.builder().message(e.getMessage()).title("Une erreur est survenue"));
            apiResponseDto.setStatus(false);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResponseDto);
        }

        return ResponseEntity.ok(apiResponseDto);

    }

    @PutMapping("/update_pwd")
    public ResponseEntity<ApiResponseDto> updatePwdUser(@RequestBody UpdatePwdRequest request) {
        ApiResponseDto apiResponseDto = new ApiResponseDto();
        try {
            authenticationServiceImpl.updateAccountPwdUser(request);
            apiResponseDto.setContent("Success");
            apiResponseDto.setStatus(true);

        } catch (Exception e) {
            apiResponseDto.setContent(
                    ErrorResponse.builder().message(e.getMessage()).title("Une erreur est survenue").build());
            apiResponseDto.setStatus(false);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResponseDto);
        }

        return ResponseEntity.ok(apiResponseDto);
    }

    @GetMapping(value = "/dashboard")
    public ResponseEntity<ApiResponseDto> getDashboard() {
        ApiResponseDto apiResponseDto = new ApiResponseDto();
        apiResponseDto.setContent(authenticationServiceImpl.getDashboard());
        apiResponseDto.setStatus(true);
        return ResponseEntity.ok(apiResponseDto);
    }

    @PostMapping("/infoLicense")
    public ResponseEntity<ApiResponseDto> infoLicence() {
        String license = "";
        return ResponseEntity.ok(Utils.verifyLicence());

    }

    @PostMapping("/essai")
    public String test(@RequestBody MessageRequest tt){
        if ((tt.getType()).equals("chat") && !(tt.getChatId()).equals("status@broadcast")) {
            System.out.println(tt.getBody());
        } else {
            System.out.println("lol inh");
        }
        
        return "ahaha";
    }

}
