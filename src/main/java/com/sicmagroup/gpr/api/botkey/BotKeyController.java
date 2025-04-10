package com.sicmagroup.gpr.api.botkey;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Comparator;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sicmagroup.gpr.api.claim.ClaimController;
import com.sicmagroup.gpr.api.claim.ClaimRequest;
import com.sicmagroup.gpr.api.claim.MeasureSatisfactionBotRequest;
import com.sicmagroup.gpr.api.claim.SaveRequest;
import com.sicmagroup.gpr.api.denunciation.DenunRequest;
import com.sicmagroup.gpr.api.denunciation.DenunciationController;
import com.sicmagroup.gpr.api.denunciation.SaveDenunRequest;
import com.sicmagroup.gpr.api.suggestion.SuggestionAddRequest;
import com.sicmagroup.gpr.api.suggestion.SuggestionRequest;
import com.sicmagroup.gpr.domain.dto.ApiResponseDto;
import com.sicmagroup.gpr.domain.dto.ClaimDto;
import com.sicmagroup.gpr.domain.dto.ErrorResponse;
import com.sicmagroup.gpr.domain.dto.LicenceControl;
import com.sicmagroup.gpr.domain.dto.SolutionDto;
import com.sicmagroup.gpr.domain.dto.SuggestionDto;
import com.sicmagroup.gpr.domain.dto.botkey.BotKeyConfigResponse;
import com.sicmagroup.gpr.domain.dto.claimResponse.CollectionChannelResponse;
import com.sicmagroup.gpr.domain.dto.claimResponse.LanguageResponse;
import com.sicmagroup.gpr.domain.dto.claimResponse.ObjetResponse;
import com.sicmagroup.gpr.domain.dto.claimResponse.ProductResponse;
import com.sicmagroup.gpr.domain.dto.claimResponse.ServicePointResponse;
import com.sicmagroup.gpr.domain.dto.claimResponse.UserResponse;
import com.sicmagroup.gpr.domain.enumeration.ClaimStatus;
import com.sicmagroup.gpr.domain.enumeration.ClaimType;
import com.sicmagroup.gpr.domain.enumeration.ConfigExportEnum;
import com.sicmagroup.gpr.domain.enumeration.Gender;
import com.sicmagroup.gpr.domain.enumeration.Role;
import com.sicmagroup.gpr.domain.model.Claim;
import com.sicmagroup.gpr.domain.model.ServicePoint;
import com.sicmagroup.gpr.domain.model.Solution;
import com.sicmagroup.gpr.domain.model.User;
import com.sicmagroup.gpr.service.auth.AuthenticationServiceImpl;
import com.sicmagroup.gpr.service.botkey.BotKeyServiceImpl;
import com.sicmagroup.gpr.service.claim.ClaimServiceImpl;
import com.sicmagroup.gpr.service.language.LanguageServiceImpl;
import com.sicmagroup.gpr.service.product.ProductServiceImpl;
import com.sicmagroup.gpr.service.servicePoint.ServicePointServiceImpl;
import com.sicmagroup.gpr.service.solution.SolutionServiceImpl;
import com.sicmagroup.gpr.utils.Utils;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/apikey")
@RequiredArgsConstructor
public class BotKeyController {
    private final BotKeyServiceImpl service;
    private final AuthenticationServiceImpl authService;
    private final DenunciationController denunciationController;
    private final ClaimController claimController;
    private final ClaimServiceImpl claimService;
    private final ServicePointServiceImpl pointDeServiceService;
    private final LanguageServiceImpl languageServiceImpl;
    private final ProductServiceImpl productServiceImpl;
    private final BotKeyConfigResponse BotKeyConfigResponse;
    private final SolutionServiceImpl solutionServiceImpl;
    //Configuration
    @GetMapping("/setting")
    public ResponseEntity<ApiResponseDto> getConfiguration(HttpServletRequest request) {
        ApiResponseDto apiResponseDto;
        try {

            Boolean isAuth = service.checkApiKeyBoolean(request);
            if (isAuth == false) {
                throw new Exception("Vous n'etes pas authentifier");
            }
            HashMap<String, Object> configs = authService.exportConfig(ConfigExportEnum.configs);

            apiResponseDto = ApiResponseDto
                    .builder()
                    .status(true)
                    .content(configs.get("data"))
                    .build();
            return ResponseEntity.ok(apiResponseDto);
        } catch (Exception e) {
            apiResponseDto = ApiResponseDto
                    .builder()
                    .status(false)
                    .content(ErrorResponse.builder().message(e.getMessage()).title("EXCEPTION")
                            .build())
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
        }
    }


    // Suggestion
    @PostMapping(value = "/suggestion/save", consumes = { MediaType.APPLICATION_OCTET_STREAM_VALUE,
            MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<ApiResponseDto> saveSuggestion(@RequestPart(name = "suggestion") String suggestionStr,
            @RequestPart(name = "files", required = false) MultipartFile[] files,
            @RequestPart(name = "audios", required = false) MultipartFile[] audios, HttpServletRequest request)
            throws JsonMappingException, JsonProcessingException {
        ApiResponseDto apiResponseDto;
        apiResponseDto = Utils.verifyLicence();

        if (apiResponseDto.isStatus() && apiResponseDto.getContent().getClass() == LicenceControl.class) {
            LicenceControl lc = (LicenceControl) apiResponseDto.getContent();
            // if (lc.isActif()) {
                Boolean isAuth = service.checkApiKeyBoolean(request);

                ObjectMapper mapper = new ObjectMapper();
                // System.out.println("suggestionStr");
                // System.out.println(suggestionStr);
                SuggestionRequest suggestionRequest = mapper.readValue(suggestionStr, SuggestionRequest.class);

                SuggestionAddRequest suggestionAddRequest = SuggestionAddRequest
                        .builder()
                        .suggestionRequest(suggestionRequest)
                        .files(files)
                        .audios(audios)
                        .build();
                try {
                    if (isAuth == false) {
                        throw new Exception("Vous n'etes pas authentifier");
                    }
                    SuggestionDto suggestion = service.saveSuggestion(suggestionAddRequest,
                            request.getHeader("API_KEY"));
                    apiResponseDto = ApiResponseDto
                            .builder()
                            .status(true)
                            .content(suggestion)
                            .build();
                    return ResponseEntity.ok(apiResponseDto);
                } catch (Exception e) {
                    apiResponseDto = ApiResponseDto
                            .builder()
                            .status(false)
                            .content(ErrorResponse.builder().message(e.getMessage()).title("EXCEPTION TGHROW").build())
                            .build();
                    if (e.getMessage().contains("not found")) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
                    } else {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponseDto);
                    }
                }
            // } else {
            //     apiResponseDto = ApiResponseDto
            //             .builder()
            //             .status(false)
            //             .content(lc)
            //             .build();

            //     return ResponseEntity.ok(apiResponseDto);
            // }

        } else {

            return ResponseEntity.ok(apiResponseDto);
        }
    }

    @GetMapping("/suggestion/{code}")
    public ResponseEntity<ApiResponseDto> getSuggestion(@PathVariable(name = "code") String code,
            HttpServletRequest request) {
        ApiResponseDto apiResponseDto;
        try {

            Boolean isAuth = service.checkApiKeyBoolean(request);
            if (isAuth == false) {
                throw new Exception("Vous n'etes pas authentifier");
            }
            // if (!code.startsWith("bot")) {
            //     return null;
            // }
            SuggestionDto suggestion = service.getSuggestion(code);

            String contenu="";
            String statut="";

            if (suggestion != null) {
                switch ((suggestion.getStatus()).toString()) {
                    case "TEMP_SAVED":
                        statut = "En attente";
                        contenu = "La suggestion portant le code  "+suggestion.getCodeClient()+ " est en attente !!!";
                        break;
                    case "SAVED":
                        statut = "En cours";
                        contenu = "La suggestion portant le code  "+suggestion.getCodeClient()+" est en cours de traitement !!!";
                        break;
                    case "TREAT":
                        statut = "Traitée";
                        String stat= suggestion.isAccepted() ? "Prise en compte" : "Non pris en compte";
                        contenu = "La suggestion portant le code : "+suggestion.getCodeClient()+ ". Statut : "+stat;
                        break;
            
                    default:
                        break;
                }
            }else{
                contenu = "La suggestion portant le code "+code+" est introuvable !!!";
            }

            // Formatez le JSON manuellement
            String jsonContent = String.format("{\"statut\": \"%s\", \"message\": \"%s\"}", statut, contenu);


            apiResponseDto = ApiResponseDto
                    .builder()
                    .status(true)
                    .content(jsonContent)
                    .build();
            return ResponseEntity.ok(apiResponseDto);
        } catch (Exception e) {
            apiResponseDto = ApiResponseDto
                    .builder()
                    .status(false)
                    .content(ErrorResponse.builder().message(e.getMessage()).title("EXCEPTION")
                            .build())
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
        }
    }

    // @GetMapping("/suggestion/user/{usercode}")
    // public ResponseEntity<ApiResponseDto> getSuggestions(@PathVariable(name = "usercode") String usercode,
    //         HttpServletRequest request) {
    //     ApiResponseDto apiResponseDto;
    //     try {

    //         Boolean isAuth = service.checkApiKeyBoolean(request);
    //         if (isAuth == false) {
    //             throw new Exception("Vous n'etes pas authentifier");
    //         }
    //         List<SuggestionDto> suggestion = service.getSuggestions(usercode);

    //         apiResponseDto = ApiResponseDto
    //                 .builder()
    //                 .status(true)
    //                 .content(suggestion)
    //                 .build();
    //         return ResponseEntity.ok(apiResponseDto);
    //     } catch (Exception e) {
    //         apiResponseDto = ApiResponseDto
    //                 .builder()
    //                 .status(false)
    //                 .content(ErrorResponse.builder().message(e.getMessage()).title("EXCEPTION")
    //                         .build())
    //                 .build();
    //         return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
    //     }
    // }


    //Denunciation
    
    @PostMapping(value = "/denunciation/save", consumes = { MediaType.APPLICATION_OCTET_STREAM_VALUE,
        MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<ApiResponseDto> saveDenunciation(@RequestPart("denun")String denunRequest,
        @RequestPart(name = "files", required = false) MultipartFile[] files,
        @RequestPart(name = "audios", required = false) MultipartFile[] audios, HttpServletRequest request)
        throws JsonMappingException, JsonProcessingException {

    ApiResponseDto apiResponseDto;
    ObjectMapper mapper = new ObjectMapper();
   DenunRequest denunRequest2 = mapper.readValue(denunRequest, DenunRequest.class);
    apiResponseDto = Utils.verifyLicence();
    if (apiResponseDto.isStatus() && apiResponseDto.getContent().getClass() == LicenceControl.class) {
        LicenceControl lc = (LicenceControl) apiResponseDto.getContent();
        // if (lc.isActif()) {
            try {
                Boolean isAuth = service.checkApiKeyBoolean(request);
               
                if (isAuth == false) {
                    throw new Exception("Vous n'etes pas authentifier");
                }
                
                SaveDenunRequest saveRequest = SaveDenunRequest.builder().claimRequest(denunRequest2).files(files)
                        .audios(audios)
                        .remoteAddress(request.getRemoteAddr()).build();
                     
                ClaimDto claim = service.saveDenunciation(saveRequest, request.getHeader("API_KEY"),
                        ClaimType.DENUNCIACION);
                apiResponseDto = ApiResponseDto
                        .builder()
                        .status(true)
                        .content(claim)
                        .build();
                return ResponseEntity.ok(apiResponseDto);
            } catch (Exception e) {
                apiResponseDto = ApiResponseDto
                .builder()
                .status(false)
                .content(ErrorResponse.builder().message(e.getMessage()).title("EXCEPTION THROW").build())
                .build();
                if (e.getMessage() != null && e.getMessage().contains("not found")) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
                } else {
                    //System.out.println("CC");
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponseDto);
                }
            }
        // } else {
        //     apiResponseDto = ApiResponseDto
        //             .builder()
        //             .status(false)
        //             .content(lc)
        //             .build();

        //     return ResponseEntity.ok(apiResponseDto);
        // }

    } else {

        return ResponseEntity.ok(apiResponseDto);
    }
}


    @GetMapping("/denunciation/{code}")
    public ResponseEntity<ApiResponseDto> getDenunciation(@PathVariable(name = "code") String code,
            HttpServletRequest request) {
                ApiResponseDto apiResponseDto;
                try {

                    Boolean isAuth = service.checkApiKeyBoolean(request);
                    if (isAuth == false) {
                        throw new Exception("Vous n'etes pas authentifier");
                    }
                    // if (!code.startsWith("bot")) {
                    //     return null;
                    // }
                    ClaimDto claim = denunciationController.getClaimClient(code);
                    
                    // ClaimDto claimDto = convert
                    String contenu="";
                    String statut="";
                    if (claim != null) {
                        
                        switch ((claim.getStatus()).toString()) {
                            case "TEMP_SAVED":
                                statut = "En attente";
                                contenu = "La dénonciation portant le code  "+claim.getCodeClient()+ " est en attente !!!";
                                break;
                            case "AFFECTED":
                                statut = "En cours";
                                contenu = "La dénonciation portant le code  "+claim.getCodeClient()+ " est en cours de traitement !!!";
                            break;
                            case "DESAPPROUVED":
                                statut = "En cours";
                                contenu = "La dénonciation portant le code  "+claim.getCodeClient()+ " est en cours de traitement !!!";
                                break;
                            case "TRANSMITTED":
                                statut = "En cours";
                                contenu = "La dénonciation portant le code  "+claim.getCodeClient()+ " est en cours de traitement !!!";
                                break;
                            case "SAVED":
                                statut = "En cours";
                                contenu = "La dénonciation portant le code  "+claim.getCodeClient()+" est en cours de traitement !!!";
                                break;
                            case "TREAT":
                                statut = "Traitée";
                                contenu = "La dénonciation portant le code : "+ claim.getCodeClient() +
                                " a été traitée." +
                                " Solution : " + claim.getSolutionDtos().get(((claim.getSolutionDtos()).size()) - 1).getContent();
                            
                                // contenu = solution;
                                break;
                            default:
                                break;
                        }
                    
                    }else{
                        statut = "Introuvable";
                        contenu = "La dénonciation portant le code "+code+" est introuvable !!!";
                    }
        
                   // Formatez le JSON manuellement
                   String jsonContent = String.format("{\"statut\": \"%s\", \"message\": \"%s\"}", statut, contenu);
        
                    apiResponseDto = ApiResponseDto
                            .builder()
                            .status(true)
                            .content(jsonContent)
                            .build();
                    return ResponseEntity.ok(apiResponseDto);
                } catch (Exception e) {
                    apiResponseDto = ApiResponseDto
                            .builder()
                            .status(false)
                            .content(ErrorResponse.builder().message(e.getMessage()).title("EXCEPTION")
                                    .build())
                            .build();
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
                }
    }

    // @GetMapping("/denunciation/user/{userCode}")
    // public ResponseEntity<ApiResponseDto> getDenunciations(@PathVariable(name = "userCode") String userCode,
    //         HttpServletRequest request) {
    //     ApiResponseDto apiResponseDto;
    //     try {

    //         Boolean isAuth = service.checkApiKeyBoolean(request);
    //         if (isAuth == false) {
    //             throw new Exception("Vous n'etes pas authentifier");
    //         }
    //         List<ClaimDto> claims = service.getClaims(userCode, ClaimType.DENUNCIACION);

    //         apiResponseDto = ApiResponseDto
    //                 .builder()
    //                 .status(true)
    //                 .content(claims)
    //                 .build();
    //         return ResponseEntity.ok(apiResponseDto);
    //     } catch (Exception e) {
    //         apiResponseDto = ApiResponseDto
    //                 .builder()
    //                 .status(false)
    //                 .content(ErrorResponse.builder().message(e.getMessage()).title("EXCEPTION")
    //                         .build())
    //                 .build();
    //         return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
    //     }
    // }



    //Claim
    @PostMapping(value = "/claim/save", consumes = { MediaType.APPLICATION_OCTET_STREAM_VALUE,
            MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<ApiResponseDto> saveClaim(@RequestPart("claim") String claimRequest,
            @RequestPart(name = "files", required = false) MultipartFile[] files,
            @RequestPart(name = "audios", required = false) MultipartFile[] audios, HttpServletRequest request)
            throws JsonMappingException, JsonProcessingException {
        ApiResponseDto apiResponseDto;
        
        ObjectMapper mapper = new ObjectMapper();
        ClaimRequest claimRequest2 = mapper.readValue(claimRequest, ClaimRequest.class);

        apiResponseDto = Utils.verifyLicence();

        if (apiResponseDto.isStatus() && apiResponseDto.getContent().getClass() == LicenceControl.class) {
            LicenceControl lc = (LicenceControl) apiResponseDto.getContent();
            // if (lc.isActif()) {
                try {
                    Boolean isAuth = service.checkApiKeyBoolean(request);

                    if (isAuth == false) {
                        throw new Exception("Vous n'etes pas authentifier");
                    }
                    SaveRequest saveRequest = SaveRequest.builder().claimRequest(claimRequest2).files(files)
                            .audios(audios)
                            .remoteAddress(request.getRemoteAddr()).build();
                            
                    ClaimDto claim = service.saveClaim(saveRequest, request.getHeader("API_KEY"),
                            ClaimType.CLAIM);
                    apiResponseDto = ApiResponseDto
                            .builder()
                            .status(true)
                            .content(claim)
                            .build();
                    return ResponseEntity.ok(apiResponseDto);
                } catch (Exception e) {
                    apiResponseDto = ApiResponseDto
                            .builder()
                            .status(false)
                            .content(ErrorResponse.builder().message(e.getMessage()).title("EXCEPTION THROW").build())
                            .build();
                    if (e.getMessage() != null && e.getMessage().contains("not found")) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
                    } else {
                        // System.out.println(claimRequest2.getContent());
                          return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponseDto);
                    }
                }
            // } else {
            //     apiResponseDto = ApiResponseDto
            //             .builder()
            //             .status(false)
            //             .content(lc)
            //             .build();

            //     return ResponseEntity.ok(apiResponseDto);
            // }

        } else {

            return ResponseEntity.ok(apiResponseDto);
        }
    }

    @GetMapping("/claim/{code}")
    public ResponseEntity<ApiResponseDto> getClaim(@PathVariable(name = "code") String code,
        HttpServletRequest request) {
        ApiResponseDto apiResponseDto;
        try {

            Boolean isAuth = service.checkApiKeyBoolean(request);
            if (isAuth == false) {
                throw new Exception("Vous n'etes pas authentifier");
            }
            // if (!code.startsWith("bot")) {
            //     return null;
            // }
            ClaimDto claim = claimController.getClaimClient(code);
            List<SolutionDto> solutions = new ArrayList<>(claim.getSolutionDtos());

            SolutionDto derniereSolution = null;
            if (!solutions.isEmpty()) {
                solutions.sort(Comparator.comparing(SolutionDto::getCreatedAt));
                derniereSolution = solutions.get(solutions.size() - 1);
            }
           
            // solutions.sort(Comparator.comparing(SolutionDto::getCreatedAt));
            // SolutionDto derniereSolution = solutions.get(solutions.size() - 1);
            // ClaimDto claimDto = convert
            String contenu="";
            String statut ="";
            String solution="";

            String  clientFirstAndLastName = claim.getClientFirstAndLastName();
            String adresse = claim.getAddress();
            Gender  gender = claim.getGender();
            String tel = claim.getTel();
            ObjetResponse Objet = claim.getObjet();
            String libelleObjet = Objet.getLibelle();
            ProductResponse Produit = claim.getProduct();
            String libelleProduit = Produit.getLibelle();
            ServicePointResponse ServicePoint = claim.getServicePoint();
            String libellePoinservice = ServicePoint.getLibelle();
            LanguageResponse Language = claim.getLanguage();
            String libelleLanguage = Language.getLibelle();
            String libelleCollectionChannel = "";
            // String folderCode = claim.getFolderCode();
            Long retardDay= claim.getRetardDay();
            // String  declenchedDate = claim.getDeclenchedDate();
            String receiptDateTime = claim.getReceiptDateTime();
            String createdAt= claim.getCreatedAt();
            String content = claim.getContent();
           

            
            if (claim != null) {
                switch ((claim.getStatus()).toString()) {
                    case "TEMP_SAVED":
                        statut = "En attente";
                        contenu = "La réclamation portant le code  "+claim.getCodeClient()+ " est en attente !!!";
                        solution="";
                        break;
                    case "AFFECTED":
                        statut = "En cours";
                        contenu = "La réclamation portant le code  "+claim.getCodeClient()+ " est en cours de traitement !!!";
                        solution="";
                    break;
                    case "DESAPPROUVED":
                        statut = "En cours";
                        contenu = "La réclamation portant le code  "+claim.getCodeClient()+ " est en cours de traitement !!!";
                        solution="";
                        break;
                    case "TRANSMITTED":
                        statut = "En cours";
                        contenu = "La réclamation portant le code  "+claim.getCodeClient()+ " est en cours de traitement !!!";
                        solution="";
                        break;
                    case "SAVED":
                        statut = "En cours";
                        contenu = "La réclamation portant le code  "+claim.getCodeClient()+" est en cours de traitement !!!";
                        solution="";
                        break;
                    case "TREAT":
                        statut = "Traitée";
                        contenu = "La réclamation portant le code : "+ claim.getCodeClient() +
                        " a été traitée." ;
                        solution=derniereSolution.getContent();
                        // contenu = solution;
                        break;
                    case "SATISFIED":
                        statut = "Mesurée et  satisfait";
                        contenu = "La réclamation portant le code :  "+ claim.getCodeClient() +
                        "  a été traitée et mesurée.  Vous êtes satisfait de la solution proposée, et nous nous réjouissons d’avoir répondu à vos attentes. N’hésitez pas à nous contacter pour toute autre demande.";
                        solution=derniereSolution.getContent();
                        // contenu = solution;
                        break;
                    case "PARTIAL_SATISFIED":
                        statut = "Mesurée et partiellement satisfait";
                        contenu = "La réclamation portant le code : "+ claim.getCodeClient() +
                        " a été traitée et mesurée. Vous êtes partiellement satisfait de la solution proposée, et notre entreprise s’engage à prendre les mesures nécessaires pour vous offrir une nouvelle solution.";
                        solution=derniereSolution.getContent();
                        // contenu = solution;
                        break;
                    case "UNSATISFIED":
                        statut = "Mesurée et non satisfait";
                        contenu = "La réclamation portant le code : "+ claim.getCodeClient() +
                        " a été traitée et mesurée. Vous n'êtes pas satisfait de la solution proposée, et notre entreprise s’engage à prendre les mesures nécessaires pour vous apporter une nouvelle solution adaptée à vos besoins.";
                        solution=derniereSolution.getContent();
                        // contenu = solution;
                        break;
                    default:
                        break;
                }
            }else{
                statut = "Introuvable";
                contenu = "La réclamation portant le code "+code+" est introuvable !!!";
            }


            // Formatez le JSON manuellement
            String jsonContent = String.format("{\"statut\": \"%s\", \"message\": \"%s\", \"identite\": \"%s\",\"adresse\": \"%s\", \"genre\": \"%s\", \"telephone\": \"%s\", \"objet\": \"%s\",\"produit\": \"%s\", \"servicePoint\": \"%s\",\"langue\":\"%s\", \"canal\": \"%s\",\"retard\": \"%s\",\"content\": \"%s\",\"DateSoumission\": \"%s\",\"DateEnregistrement\": \"%s\",\"solution\": \"%s\"}", statut, contenu, clientFirstAndLastName,adresse,gender,tel,libelleObjet,libelleProduit,libellePoinservice,libelleLanguage,libelleCollectionChannel,retardDay,content,createdAt,receiptDateTime,solution);

            apiResponseDto = ApiResponseDto
                    .builder()
                    .status(true)
                    .content(jsonContent)
                    .build();
            return ResponseEntity.ok(apiResponseDto);
        } catch (Exception e) {
            apiResponseDto = ApiResponseDto
                    .builder()
                    .status(false)
                    .content(ErrorResponse.builder().message(e.getMessage()).title("EXCEPTION")
                            .build())
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
        }
    }

    // @GetMapping("/claim/user/{userCode}")
    // public ResponseEntity<ApiResponseDto> getClaims(@PathVariable(name = "userCode") String userCode,
    //         HttpServletRequest request) {
    //     ApiResponseDto apiResponseDto;
    //     try {

    //         Boolean isAuth = service.checkApiKeyBoolean(request);
    //         if (isAuth == false) {
    //             throw new Exception("Vous n'etes pas authentifier");
    //         }
    //         List<ClaimDto> claims = service.getClaims(userCode, ClaimType.CLAIM);

    //         apiResponseDto = ApiResponseDto
    //                 .builder()
    //                 .status(true)
    //                 .content(claims)
    //                 .build();
    //         return ResponseEntity.ok(apiResponseDto);
    //     } catch (Exception e) {
    //         apiResponseDto = ApiResponseDto
    //                 .builder()
    //                 .status(false)
    //                 .content(ErrorResponse.builder().message(e.getMessage()).title("EXCEPTION")
    //                         .build())
    //                 .build();
    //         return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
    //     }
    // }

    // @PostMapping("/claim/mesure/{code}/{satisfaction}")
    // public ResponseEntity<ApiResponseDto> mesureClaim(@PathVariable(name = "code") String code,@PathVariable(name = "satisfaction") String satisfaction,
    //     HttpServletRequest request) {
    //     ApiResponseDto apiResponseDto;
    //     try {

    //         Boolean isAuth = service.checkApiKeyBoolean(request);
    //         if (isAuth == false) {
    //             throw new Exception("Vous n'etes pas authentifier");
    //         }
           
    //         ClaimDto claim = claimController.getStatus();
    //         // ClaimDto claimDto = convert
    //         String contenu="";
    //         String statut="";

    //         if (claim != null) {
                
    //             switch ((claim.getStatus()).toString()) {
    //                 case "TEMP_SAVED":
    //                     statut = "En attente";
    //                     contenu = "La réclamation portant le code  "+claim.getCodeClient()+ " est en attente !!!";
    //                     break;
    //                     case "AFFECTED":
    //                     statut = "En cours";
    //                     contenu = "La réclamation portant le code  "+claim.getCodeClient()+ " est en cours de traitement !!!";
    //                     break;
    //                 case "DESAPPROUVED":
    //                     statut = "En cours";
    //                     contenu = "La réclamation portant le code  "+claim.getCodeClient()+ " est en cours de traitement !!!";
    //                     break;
    //                 case "TRANSMITTED":
    //                     statut = "En cours";
    //                     contenu = "La réclamation portant le code  "+claim.getCodeClient()+ " est en cours de traitement !!!";
    //                     break;
    //                 case "SAVED":
    //                     statut = "En cours";
    //                     contenu = "La réclamation portant le code  "+claim.getCodeClient()+" est en cours de traitement !!!";
    //                     break;
    //                 case "TREAT":
    //                     statut = "Traitée";
    //                     contenu = "La réclamation portant le code : "+ claim.getCodeClient() +
    //                     " a été traitée." +
    //                     " Solution : " + claim.getSolutionDtos().get(((claim.getSolutionDtos()).size()) - 1).getContent();
                    
    //                     // contenu = solution;
    //                     break;
    //                 default:
    //                     break;
    //             }
            
    //         }else{
    //             statut = "Introuvable";
    //             contenu = "La réclamation portant le code "+code+" est introuvable !!!";
    //         }

    //        // Formatez le JSON manuellement
    //        String jsonContent = String.format("{\"statut\": \"%s\", \"message\": \"%s\"}", statut, contenu);

    //         apiResponseDto = ApiResponseDto
    //                 .builder()
    //                 .status(true)
    //                 .content(jsonContent)
    //                 .build();
    //         return ResponseEntity.ok(apiResponseDto);
    //     } catch (Exception e) {
    //         apiResponseDto = ApiResponseDto
    //                 .builder()
    //                 .status(false)
    //                 .content(ErrorResponse.builder().message(e.getMessage()).title("EXCEPTION")
    //                         .build())
    //                 .build();
    //         return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
    //     }
    // }

    @PostMapping("/claim/mesure")
    public ResponseEntity<ApiResponseDto> measureSatisfactionBotEntity(@RequestBody MeasureSatisfactionBotRequest request) {
    ApiResponseDto apiResponseDto = new ApiResponseDto();
    apiResponseDto = Utils.verifyLicence();

        // if (apiResponseDto.isStatus() && apiResponseDto.getContent().getClass() == LicenceControl.class) {
            // LicenceControl lc = (LicenceControl) apiResponseDto.getContent();
            // if (lc.isActif()) {
                Claim claim;
                ClaimDto claimdto;
                try {
                    claim = claimService.getByCodeClient(request.getCodeClient());
                    claimdto = claimController.getClaimClient(request.getCodeClient());
                } catch (Exception e) {
                    apiResponseDto = ApiResponseDto
                            .builder()
                            .status(false)
                            .content(ErrorResponse.builder().message(e.getMessage()).title("EXCEPTION THROW").build())
                            .build();
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
                }

                if (claim.getStatus() != ClaimStatus.TREAT) {
                    apiResponseDto = ApiResponseDto
                            .builder()
                            .status(false)
                            .content(ErrorResponse.builder().message("Status de la réclamation invalide.")
                                    .title("Opération impossible").build())
                            .build();
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
                }
                // claim.getSolutionDtos().get(((claim.getSolutionDtos()).size()) - 1).getContent()
                Solution solution;
                SolutionDto solutiondto;
                try {
                    solutiondto = claimdto.getSolutionDtos().get(((claimdto.getSolutionDtos()).size()) - 1);
                    solution = solutionServiceImpl.getById(solutiondto.getId());
                } catch (Exception e) {
                    apiResponseDto = ApiResponseDto
                            .builder()
                            .status(false)
                            .content(ErrorResponse.builder().message(e.getMessage()).title("EXCEPTION THROW").build())
                            .build();
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
                }

                User measurer = null;
                // try {
                //     measurer = authService.getByCodeClient(request.getCodeClient());
                // } catch (Exception e) {
                //     apiResponseDto = ApiResponseDto
                //             .builder()
                //             .status(false)
                //             .content(ErrorResponse.builder().message(e.getMessage()).title("EXCEPTION THROW").build())
                //             .build();
                //     return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
                // }

                // if (!measurer.canMeasureClaim() && !measurer.getAdditionalrole().equals(Role.PILOTE)) {
                //     apiResponseDto = ApiResponseDto
                //             .builder()
                //             .status(false)
                //             .content(
                //                     ErrorResponse.builder().message("Vous n'êtes pas habilité à mesurer une réclamation")
                //                             .title("Habilitation manquante").build())
                //             .build();
                //     return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(apiResponseDto);
                // }
              
                claim = claimService.measureClaim(claim, solution, measurer, request.getSatisfactionStatus(),
                        request.getCommentaire());

                apiResponseDto = ApiResponseDto
                        .builder()
                        .status(true)
                        .content(claimdto)
                        .build();
                return ResponseEntity.status(HttpStatus.OK).body(apiResponseDto);
            // } else {
            //     apiResponseDto = ApiResponseDto
            //             .builder()
            //             .status(false)
            //             .content(lc)
            //             .build();

            //     return ResponseEntity.ok(apiResponseDto);
            // }

        // } else {
        //     return ResponseEntity.ok(apiResponseDto);
        // }
    }




}
