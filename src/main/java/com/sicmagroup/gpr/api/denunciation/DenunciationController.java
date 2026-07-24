package com.sicmagroup.gpr.api.denunciation;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Objects;

import org.modelmapper.ModelMapper;
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sicmagroup.gpr.api.Media.MediaResponse;
import com.sicmagroup.gpr.api.claim.AffectTreatmentRequest;
import com.sicmagroup.gpr.api.claim.ApprouvedRequest;
import com.sicmagroup.gpr.api.claim.ClaimDeleteRequest;
import com.sicmagroup.gpr.api.claim.ClaimRequest;
import com.sicmagroup.gpr.api.claim.ClassedClaimRequest;
import com.sicmagroup.gpr.api.claim.LitigateClaimRequest;
import com.sicmagroup.gpr.api.claim.MeasureSatisfactionRequest;
import com.sicmagroup.gpr.api.claim.ProposedSolutionRequest;
import com.sicmagroup.gpr.api.claim.SaveRequest;
import com.sicmagroup.gpr.api.claim.TransmissionRequest;
import com.sicmagroup.gpr.api.claim.UnApprouvedRequest;
import com.sicmagroup.gpr.api.claimAudio.ClaimAudioResponse;
import com.sicmagroup.gpr.domain.dto.ApiResponseDto;
import com.sicmagroup.gpr.domain.dto.CategorieObjetDto;
import com.sicmagroup.gpr.domain.dto.ClaimDto;
import com.sicmagroup.gpr.domain.dto.ErrorResponse;
import com.sicmagroup.gpr.domain.dto.ExistingSolutionResponse;
import com.sicmagroup.gpr.domain.dto.ExtraContentResponse;
import com.sicmagroup.gpr.domain.dto.LicenceControl;
import com.sicmagroup.gpr.domain.dto.SatisfactionMeasureDto;
import com.sicmagroup.gpr.domain.dto.SolutionDto;
import com.sicmagroup.gpr.domain.dto.chat.ChatDto;
import com.sicmagroup.gpr.domain.dto.chat.MessageDto;
import com.sicmagroup.gpr.domain.dto.chat.UserVoteDto;
import com.sicmagroup.gpr.domain.dto.chat.VoteDto;
import com.sicmagroup.gpr.domain.dto.claimResponse.CollectionChannelResponse;
import com.sicmagroup.gpr.domain.dto.claimResponse.ExternalRecourseResponse;
import com.sicmagroup.gpr.domain.dto.claimResponse.LanguageResponse;
import com.sicmagroup.gpr.domain.dto.claimResponse.ObjetResponse;
import com.sicmagroup.gpr.domain.dto.claimResponse.ProductResponse;
import com.sicmagroup.gpr.domain.dto.claimResponse.ServicePointResponse;
import com.sicmagroup.gpr.domain.dto.claimResponse.UserResponse;
import com.sicmagroup.gpr.domain.enumeration.ClaimStatus;
import com.sicmagroup.gpr.domain.enumeration.ClaimType;
import com.sicmagroup.gpr.domain.enumeration.GravityLevel;
import com.sicmagroup.gpr.domain.enumeration.Role;
import com.sicmagroup.gpr.domain.model.CategorieObjet;
import com.sicmagroup.gpr.domain.model.Claim;
import com.sicmagroup.gpr.domain.model.CollectionChannel;
import com.sicmagroup.gpr.domain.model.ExistingSolution;
import com.sicmagroup.gpr.domain.model.ExternalRecourse;
import com.sicmagroup.gpr.domain.model.ExtraContent;
import com.sicmagroup.gpr.domain.model.Language;
import com.sicmagroup.gpr.domain.model.Media;
import com.sicmagroup.gpr.domain.model.Objet;
import com.sicmagroup.gpr.domain.model.Product;
import com.sicmagroup.gpr.domain.model.SatisfactionMeasure;
import com.sicmagroup.gpr.domain.model.ServicePoint;
import com.sicmagroup.gpr.domain.model.Solution;
import com.sicmagroup.gpr.domain.model.Suggestion;
import com.sicmagroup.gpr.domain.model.User;
import com.sicmagroup.gpr.domain.model.chat.Chat;
import com.sicmagroup.gpr.domain.model.chat.Message;
import com.sicmagroup.gpr.domain.model.chat.UserVote;
import com.sicmagroup.gpr.domain.model.chat.Vote;
import com.sicmagroup.gpr.service.auth.AuthenticationServiceImpl;
import com.sicmagroup.gpr.service.claim.ClaimServiceImpl;
import com.sicmagroup.gpr.service.claimAudio.ClaimAudioServiceImpl;
import com.sicmagroup.gpr.service.externalRecourse.ExternalRecourseServiceImpl;
import com.sicmagroup.gpr.service.media.MediaServiceImpl;
import com.sicmagroup.gpr.service.solution.SolutionServiceImpl;
import com.sicmagroup.gpr.service.servicePoint.ServicePointServiceImpl;
import com.sicmagroup.gpr.service.suggestion.SuggestionServiceImpl;
import com.sicmagroup.gpr.utils.Utils;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/denunciation")
@RequiredArgsConstructor
public class DenunciationController {
    private final ClaimServiceImpl service;
    private final SuggestionServiceImpl suggestionServiceImpl;
    private final ModelMapper modelMapper;
    private final ServicePointServiceImpl spServiceImpl;
    private final ExternalRecourseServiceImpl externalRecourseServiceImpl;
    private final ClaimAudioServiceImpl claimAudioServiceImpl;
    private final AuthenticationServiceImpl authService;
    private final SolutionServiceImpl solutionServiceImpl;
    private final MediaServiceImpl mediaService;

    @GetMapping("/list/all")
    public ResponseEntity<ApiResponseDto> getAllClaim() {
        List<Claim> allClaims = service.getAll(ClaimType.DENUNCIACION);
        List<ClaimDto> allClaimDtos = allClaims.stream().map(this::convertToDto).collect(Collectors.toList());
        ApiResponseDto apiResponseDto;
        apiResponseDto = ApiResponseDto
                .builder()
                .status(true)
                .content(allClaimDtos)
                .build();
        return ResponseEntity.ok(apiResponseDto);
    }

    @GetMapping("/getFilesBy/{claimId}")
    public ResponseEntity<ApiResponseDto> getAllFilesForAClaim(@PathVariable(name = "claimId") Long claimId) {
        ApiResponseDto apiResponseDto = ApiResponseDto.builder().build();
        Claim claim = Claim.builder().build();
        try {
            claim = service.getById(claimId);

        } catch (NotFoundException e) {
            apiResponseDto = ApiResponseDto
                    .builder()
                    .status(false)
                    .content(ErrorResponse.builder().message("Claim not found").title("NOT FOUND EXCEPTION").build())
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
        }
        List<Media> medias = mediaService.getFileByClaim(claim);
        List<MediaResponse> mediaResponses = medias.stream().map(this::convertToResponse).collect(Collectors.toList());
        System.out.println("medias.size");
        System.out.println(medias.size());

        apiResponseDto = ApiResponseDto
                .builder()
                .status(true)
                .content(mediaResponses)
                .build();
        return ResponseEntity.ok(apiResponseDto);
    }

    @GetMapping("/list")
    public ResponseEntity<ApiResponseDto> getAllClaimWithoutTempSave() {
        ApiResponseDto apiResponseDto;
        UserDetails collectorDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication()
                .getPrincipal();
        User connectedUser = User.builder().build();
        List<Claim> allClaims = new ArrayList<>();
        try {
            connectedUser = authService.getByEmail(collectorDetails.getUsername());
        } catch (Exception e) {
            apiResponseDto = ApiResponseDto
                    .builder()
                    .status(false)
                    .content(ErrorResponse.builder().message("Utilisateur introuvable").title("NOT FOUND EXCEPTION")
                            .build())
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
        }

        allClaims = service.getAllNotTempSave(ClaimType.DENUNCIACION);

        List<Claim> tmpClaims = new ArrayList<>();
        if (connectedUser.isRa()) {
            // Récupérer le point de service de l'utilisateur
            ServicePoint servicePoint = connectedUser.getServicePoint();
            
            // Récupérer tous les points de service dont le direction_id est égal à l'ID du point de service de l'utilisateur
            List<ServicePoint> relatedServicePoints = spServiceImpl.getByDirectionId(servicePoint.getId());
            
            if (!relatedServicePoints.isEmpty()) {
                // Ajouter le point de service de l'utilisateur à la liste des points de service liés
                relatedServicePoints.add(servicePoint);
        
                // Filtrer les Dénonciations pour tous ces points de service
                allClaims = allClaims.stream()
                    .filter(claim -> relatedServicePoints.contains(claim.getServicePoint()))
                    .collect(Collectors.toList());
            } else {
                // Si aucun point de service lié n'est trouvé, filtrer uniquement par le point de service de l'utilisateur
                allClaims = allClaims.stream()
                    .filter(claim -> claim.getServicePoint().equals(servicePoint))
                    .collect(Collectors.toList());
            }
        }else if (!connectedUser.getAdditionalrole().equals(Role.PILOTE)
                && !connectedUser.getAdditionalrole().equals(Role.DE)) {
            for (Claim claim : allClaims) {
                if (claim.getCollector() == connectedUser || claim.getTreatmentAffectedTo() == connectedUser) {
                    // allClaims.remove(claim);
                    tmpClaims.add(claim);
                }
            }
            allClaims = tmpClaims;
        }

        List<ClaimDto> allClaimDtos = allClaims.stream()
            .sorted(Comparator.comparing(Claim::getCreatedAt).reversed())
            .map(this::convertToDto)
            .collect(Collectors.toList());

        apiResponseDto = ApiResponseDto
                .builder()
                .status(true)
                .content(allClaimDtos)
                .build();
        return ResponseEntity.ok(apiResponseDto);
    }

    @GetMapping("/{code}/details")
    public ResponseEntity<ApiResponseDto> getClaim(@PathVariable String code) {
        ApiResponseDto apiResponseDto;
        Claim claim;
        try {
            claim = service.getByCode(code);
            apiResponseDto = ApiResponseDto
                    .builder()
                    .status(true)
                    .content(convertToDto(claim))
                    .build();
            return ResponseEntity.ok(apiResponseDto);
        } catch (Exception e) {
            apiResponseDto = ApiResponseDto
                    .builder()
                    .status(false)
                    .content(ErrorResponse.builder().message("Claim not found").title("NOT FOUND EXCEPTION").build())
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
        }

    }
 
   

    @GetMapping(value = "/listTreat")
    public ResponseEntity<ApiResponseDto> getTreatList() {
        List<Claim> allClaims = new ArrayList<>();
        ApiResponseDto apiResponseDto;
        UserDetails collectorDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication()
                .getPrincipal();
        User connectedUser = User.builder().build();
        try {
            connectedUser = authService.getByEmail(collectorDetails.getUsername());
        } catch (Exception e) {
            apiResponseDto = ApiResponseDto
                    .builder()
                    .status(false)
                    .content(ErrorResponse.builder().message("Utilisateur introuvable").title("NOT FOUND EXCEPTION")
                            .build())
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
        }

        if (connectedUser.isRa()) {

            // Filtrer les Dénonciations appartenant au même point de service que l'utilisateur
            allClaims = service.getAllByTypeAndStatusIn(ClaimType.DENUNCIACION, Arrays.asList(ClaimStatus.SAVED,
            ClaimStatus.AFFECTED, ClaimStatus.TO_APPROUVED, ClaimStatus.DESAPPROUVED));

            List<Claim> moreClaim = service.getAllByTypeAndStatusIn(ClaimType.DENUNCIACION,
                    Arrays.asList(ClaimStatus.UNSATISFIED, ClaimStatus.PARTIAL_SATISFIED,
                            ClaimStatus.CLASSED));
            allClaims.addAll(moreClaim);
            // Récupérer le point de service de l'utilisateur
            ServicePoint servicePoint = connectedUser.getServicePoint();
            Long raId = connectedUser.getId();

            // Récupérer tous les points de service dont le direction_id est égal à l'ID du point de service de l'utilisateur
            List<ServicePoint> relatedServicePoints = spServiceImpl.getByDirectionId(servicePoint.getId());

            if (!relatedServicePoints.isEmpty()) {
                // Ajouter le point de service de l'utilisateur à la liste des points de service liés
                relatedServicePoints.add(servicePoint);

                // Filtrer les Dénonciations pour tous ces points de service
                allClaims = allClaims.stream()
                    .filter(claim -> relatedServicePoints.contains(claim.getServicePoint())
                    || (claim.getTreatmentAffectedTo() != null && claim.getTreatmentAffectedTo().getId().equals(raId))
                    || (claim.getTransmittedTo() != null && claim.getTransmittedTo().getId().equals(raId)))
                    .collect(Collectors.toList());
            } else {
                // Si aucun point de service lié n'est trouvé, filtrer uniquement par le point de service de l'utilisateur
                allClaims = allClaims.stream()
                    .filter(claim -> claim.getServicePoint().equals(servicePoint)
                    || (claim.getTreatmentAffectedTo() != null && claim.getTreatmentAffectedTo().getId().equals(raId))
                    || (claim.getTransmittedTo() != null && claim.getTransmittedTo().getId().equals(raId)))
                    .collect(Collectors.toList());
            }
        }else if (connectedUser.canAffectTreatment() || (connectedUser.getAdditionalrole().equals(Role.PILOTE)
                || connectedUser.getAdditionalrole().equals(Role.DE))) {
            // System.out.println(connectedUser.getPoste().getHabilitations());
            allClaims = service.getAllByTypeAndStatusIn(ClaimType.DENUNCIACION, Arrays.asList(ClaimStatus.SAVED,
                    ClaimStatus.AFFECTED, ClaimStatus.TO_APPROUVED, ClaimStatus.DESAPPROUVED));

            if (connectedUser.getAdditionalrole().equals(Role.DE)
                    || connectedUser.getAdditionalrole().equals(Role.PILOTE)) {
                List<Claim> moreClaim = service.getAllByTypeAndStatusIn(ClaimType.DENUNCIACION,
                        Arrays.asList(ClaimStatus.UNSATISFIED, ClaimStatus.PARTIAL_SATISFIED,
                                ClaimStatus.CLASSED));
                allClaims.addAll(moreClaim);
            }

            if (connectedUser.getAdditionalrole().equals(Role.DE)) {
                allClaims = service.getClaimsWhenUserIsInGuestChatSuper(connectedUser, allClaims);
            }
        }else {
            // voir les Dénonciations qu'on à affecter à l'utilisateur et
            allClaims = service.getAllByTypeAndCollectorAndStatusOrTreatmentAffectedToAndStatusIn(
                    ClaimType.DENUNCIACION, ClaimStatus.SAVED, connectedUser,
                    Arrays.asList(ClaimStatus.AFFECTED, ClaimStatus.TO_APPROUVED, ClaimStatus.DESAPPROUVED));

            allClaims = service.getClaimsWhenUserIsInGuestChat(connectedUser, allClaims);

        }
        List<ClaimDto> allClaimDtos = allClaims.stream()
            .sorted(Comparator.comparing(Claim::getCreatedAt).reversed())
            .map(this::convertToDto)
            .collect(Collectors.toList());

        apiResponseDto = ApiResponseDto
                .builder()
                .status(true)
                .content(allClaimDtos)
                .build();
        return ResponseEntity.ok(apiResponseDto);
    }

    @GetMapping("/getAudiosBy/{claimId}")
    public ResponseEntity<List<ClaimAudioResponse>> getAllClaimAudioForAClaim(
            @PathVariable(name = "claimId") Long claimId) {
        ApiResponseDto apiResponseDto = ApiResponseDto.builder().build();
        Claim claim = Claim.builder().build();
        try {
            claim = service.getById(claimId);

        } catch (NotFoundException e) {
            apiResponseDto = ApiResponseDto
                    .builder()
                    .status(false)
                    .content(ErrorResponse.builder().message("Claim not found").title("NOT FOUND EXCEPTION").build())
                    .build();
            return ResponseEntity.notFound().build();
        }
        List<ClaimAudioResponse> medias = claimAudioServiceImpl.getAudioByClaim(claim);
        // List<MediaResponse> mediaResponses =
        // medias.stream().map(this::convertToResponse).collect(Collectors.toList());
        // System.out.println("medias.size");
        // System.out.println(medias.size());

        apiResponseDto = ApiResponseDto
                .builder()
                .status(true)
                .content(medias)
                .build();
        return ResponseEntity.ok(medias);
    }

    @GetMapping(value = "/list/{status}")
public ResponseEntity<ApiResponseDto> getAllClaimBasedOnStatus(@PathVariable ClaimStatus status) {
    List<Claim> allClaims = new ArrayList<>();
    ApiResponseDto apiResponseDto;

    // Récupérer l'utilisateur connecté
    UserDetails connectedUserDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    User connectedUser;
    
    try {
        connectedUser = authService.getByEmail(connectedUserDetails.getUsername());
    } catch (Exception e) {
        apiResponseDto = ApiResponseDto
                .builder()
                .status(false)
                .content(ErrorResponse.builder().message("Utilisateur introuvable").title("NOT FOUND EXCEPTION").build())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
    }

    // Si l'utilisateur est un PILOTE
    if (connectedUser.getAdditionalrole().equals(Role.PILOTE)) {
        List<Claim> allFilteredClaims = service.getAllByStatusIn(Arrays.asList(status));

        // Ajouter toutes les dénonciations "bot"
        for (Claim claim : allFilteredClaims) {
            if (claim.getCode().startsWith("bot") &&
                allClaims.stream().noneMatch(c -> c.getId().equals(claim.getId()))) {
                allClaims.add(claim);
            }
        }

        // Si le statut est TEMP_SAVED, ajouter les dénonciations sauvegardées par le pilote
        if (status == ClaimStatus.TEMP_SAVED) {
            List<Claim> savedByPilot = service.getAllByTypeStatusCollector(ClaimType.DENUNCIACION, status, connectedUser);
            // allClaims.addAll(savedByPilot);
            for (Claim claim : savedByPilot) {
                if (allClaims.stream().noneMatch(c -> c.getId().equals(claim.getId()))) {
                    allClaims.add(claim);
                }
            }
        }
    } else {
        // Si l'utilisateur n'est pas un PILOTE, appliquer la logique normale
        if (status == ClaimStatus.TEMP_SAVED) {
            allClaims = service.getAllByTypeStatusCollector(ClaimType.DENUNCIACION, status, connectedUser);
        } else if (status == ClaimStatus.TREAT) {
            allClaims = service.getAllWithLatestApprouvedSolutionByTypeAndStatusIn(ClaimType.DENUNCIACION, Arrays.asList(ClaimStatus.TREAT));
        } else {
            allClaims = service.getClaimByStatus(ClaimType.DENUNCIACION, status);
        }
    }

    // Conversion en DTO
    List<ClaimDto> allClaimDtos = allClaims.stream()
        .map(this::convertToDto)
        .sorted((c1, c2) -> c2.getCreatedAt().compareTo(c1.getCreatedAt()))
        .collect(Collectors.toList());

    // Création de la réponse API
    apiResponseDto = ApiResponseDto
            .builder()
            .status(true)
            .content(allClaimDtos)
            .build();

    return ResponseEntity.ok(apiResponseDto);
}

// @GetMapping("/{code}/details/client")
    public ClaimDto getClaimClient(@PathVariable String code) {
    ApiResponseDto apiResponseDto;
    Claim claim;
    try {
        claim = service.getByCodeClient(code);
        apiResponseDto = ApiResponseDto
                .builder()
                .status(true)
                .content(convertToDto(claim))
                .build();
        // return ResponseEntity.ok(apiResponseDto);
        return convertToDto(claim);
    } catch (Exception e) {
        apiResponseDto = ApiResponseDto
                .builder()
                .status(false)
                .content(ErrorResponse.builder().message("Claim not found").title("NOT FOUND EXCEPTION").build())
                .build();
        return null;
        // return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
    }

}




    @PostMapping(value = "/add", consumes = { MediaType.APPLICATION_OCTET_STREAM_VALUE,
            MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<ApiResponseDto> saveClaim(@RequestPart("denun") String denunRequest,
            @RequestPart(name = "files", required = false) MultipartFile[] files,
            @RequestPart(name = "audios", required = false) MultipartFile[] audios, HttpServletRequest request2)
            throws JsonMappingException, JsonProcessingException {
        ApiResponseDto apiResponseDto = Utils.verifyLicence();
        
        if (apiResponseDto.isStatus() && apiResponseDto.getContent().getClass() == LicenceControl.class) {
            LicenceControl lc = (LicenceControl) apiResponseDto.getContent();
            if (lc.isActif()) {
                ObjectMapper mapper = new ObjectMapper();
                DenunRequest denunRequest2 = mapper.readValue(denunRequest, DenunRequest.class);

                try {
                    SaveDenunRequest saveRequest = SaveDenunRequest.builder().claimRequest(denunRequest2).files(files)
                            .audios(audios)
                            .remoteAddress(request2.getRemoteAddr()).build();

                    Claim claim = service.saveClaim(saveRequest, ClaimType.DENUNCIACION);
                    apiResponseDto = ApiResponseDto
                            .builder()
                            .status(true)
                            .content(convertToDto(claim))
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
            } else {
                apiResponseDto = ApiResponseDto
                        .builder()
                        .status(false)
                        .content(lc)
                        .build();

                return ResponseEntity.ok(apiResponseDto);
            }

        } else {

            return ResponseEntity.ok(apiResponseDto);
        }
    }

    @PostMapping(value = "/save_temp", consumes = { MediaType.APPLICATION_OCTET_STREAM_VALUE,
            MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<ApiResponseDto> saveTempClaim(@RequestPart("denun") String denunRequest,
            @RequestPart(name = "files", required = false) MultipartFile[] files,
            @RequestPart(name = "audios", required = false) MultipartFile[] audios, HttpServletRequest request2)
            throws JsonMappingException, JsonProcessingException {
        ApiResponseDto apiResponseDto = Utils.verifyLicence();
        
        if (apiResponseDto.isStatus() && apiResponseDto.getContent().getClass() == LicenceControl.class) {
            LicenceControl lc = (LicenceControl) apiResponseDto.getContent();
            if (lc.isActif()) {
                ObjectMapper mapper = new ObjectMapper();
                ClaimRequest denunRequest2 = mapper.readValue(denunRequest, ClaimRequest.class);
                try {
                    SaveRequest saveRequest = SaveRequest.builder().claimRequest(denunRequest2).files(files)
                            .audios(audios)
                            .remoteAddress(request2.getRemoteAddr()).build();

                    Claim claim = service.saveTempClaim(saveRequest, ClaimType.DENUNCIACION);
                    apiResponseDto = ApiResponseDto
                            .builder()
                            .status(true)
                            .content(convertToDto(claim))
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
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponseDto);
                    }
                }
            } else {
                apiResponseDto = ApiResponseDto
                        .builder()
                        .status(false)
                        .content(lc)
                        .build();

                return ResponseEntity.ok(apiResponseDto);
            }

        } else {

            return ResponseEntity.ok(apiResponseDto);
        }

    }

    @PutMapping("/affectTreatment")
    public ResponseEntity<ApiResponseDto> affectTreatmentTo(@RequestBody AffectTreatmentRequest request,
            HttpServletRequest request2) {
        ApiResponseDto apiResponseDto = Utils.verifyLicence();
        ;
        if (apiResponseDto.isStatus() && apiResponseDto.getContent().getClass() == LicenceControl.class) {
            LicenceControl lc = (LicenceControl) apiResponseDto.getContent();
            if (lc.isActif()) {

                Claim claim = new Claim();

                try {
                    claim = service.getById(request.getClaimId());
                } catch (Exception e) {
                    apiResponseDto = ApiResponseDto
                            .builder()
                            .status(false)
                            .content(ErrorResponse.builder().message(e.getMessage()).title("EXCEPTION THROW").build())
                            .build();
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
                }
                User affectedBy = new User();
                try {
                    affectedBy = authService.getById(request.getAffectorId());
                    if (!affectedBy.canAffectTreatment() && !affectedBy.getAdditionalrole().equals(Role.PILOTE) && !affectedBy.isRa()) {
                        throw new Exception("L'utilisateur n'est pas habilité à effectuer cette action");
                    }
                } catch (Exception e) {
                    apiResponseDto = ApiResponseDto
                            .builder()
                            .status(false)
                            .content(ErrorResponse.builder().message(e.getMessage()).title("EXCEPTION THROW").build())
                            .build();
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
                }
                User affectedTo = new User();
                try {
                    affectedTo = authService.getById(request.getAffectToId());
                } catch (Exception e) {
                    apiResponseDto = ApiResponseDto
                            .builder()
                            .status(false)
                            .content(ErrorResponse.builder().message(e.getMessage()).title("EXCEPTION THROW").build())
                            .build();
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
                }
                if (claim.getObjet().getRisqueLevel() == GravityLevel.GRAVE && !affectedTo.canTreatHighRiskClaim()) {
                    apiResponseDto = ApiResponseDto
                            .builder()
                            .status(false)
                            .content(ErrorResponse.builder()
                                    .message(
                                            "L'utilisateur choisi n'est pas habilité à traiter des dénonciations à risque "
                                                    + GravityLevel.GRAVE.name())
                                    .title("Opération non autorisée").build())
                            .build();
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponseDto);
                } else if (claim.getObjet().getRisqueLevel() == GravityLevel.MOYEN
                        && !affectedTo.canTreatMiddleRiskClaim()) {
                    apiResponseDto = ApiResponseDto
                            .builder()
                            .status(false)
                            .content(ErrorResponse.builder()
                                    .message(
                                            "L'utilisateur choisi n'est pas habilité à traiter des dénonciations à risque "
                                                    + GravityLevel.MOYEN.name())
                                    .title("Opération non autorisée").build())
                            .build();
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponseDto);
                } else if (claim.getObjet().getRisqueLevel() == GravityLevel.MINEUR
                        && !affectedTo.canTreatMinorRiskClaim()) {
                    apiResponseDto = ApiResponseDto
                            .builder()
                            .status(false)
                            .content(ErrorResponse.builder()
                                    .message(
                                            "L'utilisateur choisi n'est pas habilité à traiter des dénonciations à risque "
                                                    + GravityLevel.MINEUR.name())
                                    .title("Opération non autorisée").build())
                            .build();
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponseDto);
                }

                try {
                    claim = service.affectTreatmentToUser(claim, affectedTo, affectedBy, request.getAffectedAnonymous(),
                            request2.getRemoteAddr(), request);
                    apiResponseDto = ApiResponseDto
                            .builder()
                            .status(true)
                            .content(convertToDto(claim))
                            .build();
                    return ResponseEntity.ok(apiResponseDto);
                } catch (Exception e) {
                    apiResponseDto = ApiResponseDto
                            .builder()
                            .status(false)
                            .content(ErrorResponse.builder().message(e.getMessage()).title("EXCEPTION THROW").build())
                            .build();
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
                }
            } else {
                apiResponseDto = ApiResponseDto
                        .builder()
                        .status(false)
                        .content(lc)
                        .build();

                return ResponseEntity.ok(apiResponseDto);
            }

        } else {

            return ResponseEntity.ok(apiResponseDto);
        }
    }

    @PutMapping("/treatDenun")
    public ResponseEntity<ApiResponseDto> treatClaim(@RequestBody ProposedSolutionRequest request) {
        ApiResponseDto apiResponseDto;
        Claim claim = new Claim();
        User treator = new User();
        apiResponseDto = Utils.verifyLicence();
        ;
        if (apiResponseDto.isStatus() && apiResponseDto.getContent().getClass() == LicenceControl.class) {
            LicenceControl lc = (LicenceControl) apiResponseDto.getContent();
            if (lc.isActif()) {
                try {
                    claim = service.getById(request.getClaimId());
                } catch (Exception e) {
                    apiResponseDto = ApiResponseDto
                            .builder()
                            .status(false)
                            .content(ErrorResponse.builder().message(e.getMessage()).title("Dénonciation introuvable")
                                    .build())
                            .build();
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
                }

                try {
                    treator = authService.getById(request.getTreatorId());
                } catch (Exception e) {
                    apiResponseDto = ApiResponseDto
                            .builder()
                            .status(false)
                            .content(ErrorResponse.builder().message(e.getMessage()).title("Dénonciation introuvable")
                                    .build())
                            .build();
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
                }
                if (claim.getObjet().getRisqueLevel() == GravityLevel.GRAVE &&
                        (!treator.getAdditionalrole().equals(Role.DE))
                        && !treator.canTreatHighRiskClaim()) {
                    apiResponseDto = ApiResponseDto
                            .builder()
                            .status(false)
                            .content(
                                    ErrorResponse.builder()
                                            .message("Vous n'êtes pas habileté à traiter cette Dénonciation GRAVE")
                                            .title("Habilitation manquante").build())
                            .build();
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(apiResponseDto);
                } else if (claim.getObjet().getRisqueLevel() == GravityLevel.MOYEN &&
                        (!treator.getAdditionalrole().equals(Role.DE))
                        && !treator.canTreatMiddleRiskClaim()) {
                    apiResponseDto = ApiResponseDto
                            .builder()
                            .status(false)
                            .content(ErrorResponse.builder()
                                    .message("Vous n'êtes pas habileté à traiter cette Dénonciation à risque MOYEN")
                                    .title("Habilitation manquante").build())
                            .build();
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(apiResponseDto);
                } else if (claim.getObjet().getRisqueLevel() == GravityLevel.MINEUR
                        && !treator.canTreatMinorRiskClaim()) {
                    apiResponseDto = ApiResponseDto
                            .builder()
                            .status(false)
                            .content(ErrorResponse.builder()
                                    .message("Vous n'êtes pas hailité à traiter cette Dénonciation à risque MINEUR")
                                    .title("Habilitation manquante").build())
                            .build();
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(apiResponseDto);
                }
                try {
                    claim = service.treatClaim(claim, treator, request);
                    apiResponseDto = ApiResponseDto
                            .builder()
                            .status(true)
                            .content(convertToDto(claim))
                            .build();
                    return ResponseEntity.ok(apiResponseDto);
                } catch (Exception e) {
                    apiResponseDto = ApiResponseDto
                            .builder()
                            .status(false)
                            .content(ErrorResponse.builder().message(e.getMessage()).title("EXCEPTION THROW").build())
                            .build();
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
                }
            } else {
                apiResponseDto = ApiResponseDto
                        .builder()
                        .status(false)
                        .content(lc)
                        .build();

                return ResponseEntity.ok(apiResponseDto);
            }

        } else {

            return ResponseEntity.ok(apiResponseDto);
        }
    }

    @PutMapping(value = "/unapprouvedSolution")
    public ResponseEntity<ApiResponseDto> unApprouvedSolution(@RequestBody UnApprouvedRequest request) {
        ApiResponseDto apiResponseDto = new ApiResponseDto();
        apiResponseDto = Utils.verifyLicence();
        ;
        if (apiResponseDto.isStatus() && apiResponseDto.getContent().getClass() == LicenceControl.class) {
            LicenceControl lc = (LicenceControl) apiResponseDto.getContent();
            if (lc.isActif()) {
                Claim claim = new Claim();
                try {
                    claim = service.getById(request.getClaimId());
                } catch (Exception e) {
                    apiResponseDto = ApiResponseDto
                            .builder()
                            .status(false)
                            .content(ErrorResponse.builder().message(e.getMessage()).title("EXCEPTION THROW").build())
                            .build();
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
                }

                if (claim.getStatus() != ClaimStatus.TO_APPROUVED) {
                    apiResponseDto = ApiResponseDto
                            .builder()
                            .status(false)
                            .content(ErrorResponse.builder().message("Status de la Dénonciation invalide.")
                                    .title("Opération impossible").build())
                            .build();
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
                }
                Solution solution = new Solution();

                try {
                    solution = solutionServiceImpl.getById(request.getSolutionId());
                } catch (Exception e) {
                    apiResponseDto = ApiResponseDto
                            .builder()
                            .status(false)
                            .content(ErrorResponse.builder().message(e.getMessage()).title("EXCEPTION THROW").build())
                            .build();
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
                }

                User unapprouver = new User();

                try {
                    unapprouver = authService.getById(request.getUnApprouverId());

                } catch (Exception e) {
                    apiResponseDto = ApiResponseDto
                            .builder()
                            .status(false)
                            .content(ErrorResponse.builder().message(e.getMessage()).title("EXCEPTION THROW").build())
                            .build();
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
                }

                claim = service.unApprouvedSolution(claim, solution, unapprouver, request.getMotifDesaprobation());

                apiResponseDto = ApiResponseDto
                        .builder()
                        .status(true)
                        .content(convertToDto(claim))
                        .build();

                return ResponseEntity.status(HttpStatus.OK).body(apiResponseDto);
            } else {
                apiResponseDto = ApiResponseDto
                        .builder()
                        .status(false)
                        .content(lc)
                        .build();

                return ResponseEntity.ok(apiResponseDto);
            }

        } else {

            return ResponseEntity.ok(apiResponseDto);
        }
    }

    @PutMapping(value = "/approuvedSolution")
    public ResponseEntity<ApiResponseDto> approuvedSolution(@RequestBody ApprouvedRequest request) {
        ApiResponseDto apiResponseDto = new ApiResponseDto();
        apiResponseDto = Utils.verifyLicence();
        ;
        if (apiResponseDto.isStatus() && apiResponseDto.getContent().getClass() == LicenceControl.class) {
            LicenceControl lc = (LicenceControl) apiResponseDto.getContent();
            if (lc.isActif()) {
                Claim claim = new Claim();
                try {
                    claim = service.getById(request.getClaimId());
                } catch (Exception e) {
                    apiResponseDto = ApiResponseDto
                            .builder()
                            .status(false)
                            .content(ErrorResponse.builder().message(e.getMessage()).title("EXCEPTION THROW").build())
                            .build();
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
                }

                if (claim.getStatus() != ClaimStatus.TO_APPROUVED) {
                    apiResponseDto = ApiResponseDto
                            .builder()
                            .status(false)
                            .content(ErrorResponse.builder().message("Status de la Dénonciation invalide.")
                                    .title("Opération impossible").build())
                            .build();
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
                }
                Solution solution = new Solution();

                try {
                    solution = solutionServiceImpl.getById(request.getSolutionId());
                } catch (Exception e) {
                    apiResponseDto = ApiResponseDto
                            .builder()
                            .status(false)
                            .content(ErrorResponse.builder().message(e.getMessage()).title("EXCEPTION THROW").build())
                            .build();
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
                }

                User approuver = new User();

                try {
                    approuver = authService.getById(request.getApprouverId());

                } catch (Exception e) {
                    apiResponseDto = ApiResponseDto
                            .builder()
                            .status(false)
                            .content(ErrorResponse.builder().message(e.getMessage()).title("EXCEPTION THROW").build())
                            .build();
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
                }

                claim = service.approuvedSolution(claim, solution, approuver);

                apiResponseDto = ApiResponseDto
                        .builder()
                        .status(true)
                        .content(convertToDto(claim))
                        .build();

                return ResponseEntity.status(HttpStatus.OK).body(apiResponseDto);
            } else {
                apiResponseDto = ApiResponseDto
                        .builder()
                        .status(false)
                        .content(lc)
                        .build();

                return ResponseEntity.ok(apiResponseDto);
            }

        } else {

            return ResponseEntity.ok(apiResponseDto);
        }
    }

    @PutMapping("/transmit_to")
    public ResponseEntity<ApiResponseDto> transmitClaim(@RequestBody TransmissionRequest request) {
        ApiResponseDto apiResponseDto = new ApiResponseDto();
        apiResponseDto = Utils.verifyLicence();
        ;
        if (apiResponseDto.isStatus() && apiResponseDto.getContent().getClass() == LicenceControl.class) {
            LicenceControl lc = (LicenceControl) apiResponseDto.getContent();
            if (lc.isActif()) {
                UserDetails collectorDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication()
                        .getPrincipal();
                User connectedUser = User.builder().build();
                try {
                    connectedUser = authService.getByEmail(collectorDetails.getUsername());
                } catch (Exception e) {
                    apiResponseDto = ApiResponseDto
                            .builder()
                            .status(false)
                            .content(ErrorResponse.builder().message("Utilisateur introuvable")
                                    .title("NOT FOUND EXCEPTION")
                                    .build())
                            .build();
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
                }

                Claim claim = new Claim();
                try {
                    claim = service.getById(request.getClaimId());
                } catch (Exception e) {
                    apiResponseDto = ApiResponseDto
                            .builder()
                            .status(false)
                            .content(ErrorResponse.builder().message(e.getMessage()).title("EXCEPTION THROW").build())
                            .build();
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
                }
                // if (claim.getCollector() != connectedUser) {
                //     apiResponseDto = ApiResponseDto
                //             .builder()
                //             .status(false)
                //             .content(ErrorResponse.builder()
                //                     .message("Vous n'êtes pas le collecteur de cette Dénonciation.")
                //                     .title("Opération invalide")
                //                     .build())
                //             .build();
                //     return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponseDto);
                // }
                try {
                    claim = service.transmitClaim(claim);
                    apiResponseDto = ApiResponseDto
                            .builder()
                            .status(true)
                            .content(convertToDto(claim))
                            .build();
                    return ResponseEntity.ok(apiResponseDto);
                } catch (Exception e) {
                    apiResponseDto = ApiResponseDto
                            .builder()
                            .status(false)
                            .content(ErrorResponse.builder().message(e.getMessage()).title("EXCEPTION THROW").build())
                            .build();
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponseDto);
                }
            } else {
                apiResponseDto = ApiResponseDto
                        .builder()
                        .status(false)
                        .content(lc)
                        .build();

                return ResponseEntity.ok(apiResponseDto);
            }

        } else {

            return ResponseEntity.ok(apiResponseDto);
        }
    }
    
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ApiResponseDto> deleteClaim(@PathVariable Long id, HttpServletRequest request) {
        ApiResponseDto apiResponseDto;
        try {
            UserDetails collectorDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication()
                .getPrincipal();
            User connectedUser = User.builder().build();

            try {
                connectedUser = authService.getByEmail(collectorDetails.getUsername());
            } catch (Exception e) {
                apiResponseDto = ApiResponseDto
                        .builder()
                        .status(false)
                        .content(ErrorResponse.builder().message("Utilisateur introuvable")
                                .title("NOT FOUND EXCEPTION")
                                .build())
                        .build();
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
            }

          
            try {
                service.deleteById_2(id);
                return ResponseEntity.ok(
                    ApiResponseDto.builder().status(true).content("Dénonciation supprimée avec succès.").build()
                    );
            } catch (NotFoundException e) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponseDto.builder().status(false).content("Dénonciation non trouvée.").build()
                );
            }
           
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponseDto.builder()
                    .status(false)
                    .content(ErrorResponse.builder().title("Erreur").message(e.getMessage()).build())
                    .build()
            );
        }
    }

    @PostMapping("/convert")
    public ResponseEntity<ApiResponseDto> convertClaimToSuggestionOrDenunciation(@RequestBody Map<String, String> body) {
        String code = (body.get("code")).trim();
        Long claimId = Long.valueOf(body.get("claimId"));
        ApiResponseDto apiResponseDto;
        try {
            UserDetails collectorDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication()
                .getPrincipal();
            User connectedUser = User.builder().build();

            try {
                connectedUser = authService.getByEmail(collectorDetails.getUsername());
            } catch (Exception e) {
                apiResponseDto = ApiResponseDto
                        .builder()
                        .status(false)
                        .content(ErrorResponse.builder().message("Utilisateur introuvable")
                                .title("NOT FOUND EXCEPTION")
                                .build())
                        .build();
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
            }

            System.out.println("claimId :" + claimId);
            if (connectedUser.getAdditionalrole().equals(Role.PILOTE)) {
                if (code.startsWith("sug")) {
                    Suggestion suggestion = suggestionServiceImpl.getByCode(code);
                    service.convertClaimToSuggestion(claimId, suggestion.getId());

                    apiResponseDto = ApiResponseDto.builder()
                            .status(true)
                            .content("La Dénonciation a été convertie en suggestion avec succès.")
                            .build();
                    return ResponseEntity.ok(apiResponseDto);
                } else {
                    apiResponseDto = ApiResponseDto.builder()
                            .status(false)
                            .content(ErrorResponse.builder()
                                    .message("Code inconnu.")
                                    .title("INVALID CODE")
                                    .build())
                            .build();
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponseDto);
                }
            } else {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    ApiResponseDto.builder()
                        .status(false)
                        .content("Vous n’êtes pas autorisé à effectuer cette action.")
                        .build()
                );
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponseDto.builder()
                    .status(false)
                    .content(ErrorResponse.builder().title("Erreur").message(e.getMessage()).build())
                    .build()
            );
        }
    }

    private MediaResponse convertToResponse(Media media) {
        MediaResponse mediaResponse = modelMapper.map(media, MediaResponse.class);
        mediaResponse.setSize(media.getSize());
        return mediaResponse;
    }

    private UserResponse convertToResponse(User user) {
        UserResponse userResponse = modelMapper.map(user, UserResponse.class);
        return userResponse;
    };

    public ClaimDto convertToDto(Claim claim) {
        ClaimDto claimDto = modelMapper.map(claim, ClaimDto.class);
        if (claim.getProduct() != null) {
            claimDto.setProduct(convertToResponse(claim.getProduct()));
        }
        if (claim.getCollectionChannel() != null) {
            claimDto.setCollectionChannel(convertToResponse(claim.getCollectionChannel()));
        }

        if (claim.getObjet() != null) {
            claimDto.setObjet(convertToResponse(claim.getObjet()));
        }

        if (claim.getServicePoint() != null) {
            claimDto.setServicePoint(convertToResponse(claim.getServicePoint()));
        }

        if (claim.getLanguage() != null) {
            claimDto.setLanguage(convertToResponse(claim.getLanguage()));
        }

        if (claim.getCollector() != null) {
            claimDto.setCollector(convertToResponse(claim.getCollector()));
        }

        if (claim.getExtraContents() != null) {
            claimDto.setExtras(claim.getExtraContents().stream().map(this::convertToResponse).filter(Objects::nonNull).collect(Collectors.toList()));
        }

        // if (claim.getMedias() != null) {
        // claimDto.setMedias(claim.getMedias());
        // }

        if (claim.getSolutions() != null) {
            claimDto.setSolutionDtos(
                    claim.getSolutions().stream().map(this::convertToDto).collect(Collectors.toList()));

            Collections.reverse(claimDto.getSolutionDtos());

        }

        if (claim.getExternalRecourses() != null) {
            claimDto.setExternalRecourses(
                    claim.getExternalRecourses().stream().map(this::convertToResponse).collect(Collectors.toList()));
        }

        if (claim.getReceiptDateTime() != null) {
            claimDto.setReceiptDateTime(claimDto.convertDate(claim.getReceiptDateTime()));
        }

        // if (claim.getCreatedAt() != null) {
        // claimDto.setCreatedAt(claimDto.convertDate(claim.getCreatedAt()));
        // }

        if (claim.getUpdatedAt() != null) {
            claimDto.setUpdatedAt(claimDto.convertDate(claim.getUpdatedAt()));
        }

        if (claim.getOnlineUploadDateTime() != null) {
            claimDto.setOnlineUploadDateTime(claimDto.convertDate(claim.getOnlineUploadDateTime()));
        }

        if (claim.getSession() != null) {
            claimDto.setSession(convertToDto(claim.getSession()));
        }

        if (claim.getConvertedAt() != null) {
            claimDto.setConvertedAt(claimDto.convertDate(claim.getConvertedAt()));
        }

        if (claim.getConvertedBy() != null) {
            claimDto.setConvertedBy(convertToResponse(claim.getConvertedBy()));
        }

        //Date déclenchement de retard de traitement
        if (claim.getObjet() != null && claim.getReceiptDateTime() != null) {
            LocalDateTime calculateDate = claim.getReceiptDateTime().plusDays(claim.getObjet().getProcessingTime());
            // calculateDate = calculateDate.minusDays(7);
            if (LocalDateTime.now().isAfter(calculateDate)) {

                Long hoursRetard = LocalDateTime.now().until(calculateDate, ChronoUnit.HOURS);
                Long days = hoursRetard / 24;
                Long hours = hoursRetard % 24;
                if (days == 0) {
                    claimDto.setRetardDay(hours);
                } else {
                    claimDto.setRetardDay(days);
                }
                claimDto.setDeclenchedDate(days + " jr(s) " + hours + " heure(s)");
                
            } else {
                if(claim.getObjet().getProcessingTime() <= 7){
                        Long day = LocalDateTime.now().until((claim.getReceiptDateTime().plusDays(claim.getObjet().getProcessingTime())), ChronoUnit.DAYS);
                        if (day == 0) {
                            day = LocalDateTime.now().until((claim.getReceiptDateTime().plusDays(claim.getObjet().getProcessingTime())), ChronoUnit.HOURS);
                            claimDto.setDeclenchedDate(day + " heure(s) ");
                            claimDto.setRetardDay(Long.parseLong(""+day));
                        } else {
                            claimDto.setDeclenchedDate(day + " jr(s) ");
                            claimDto.setRetardDay(Long.parseLong(""+day));
                        }
                        
                    }else{
                        claimDto.setDeclenchedDate("-");
                        claimDto.setRetardDay(Long.parseLong(""+(claim.getObjet().getProcessingTime() - 7)));
                    }
                
            }
        }

        
        // if (claim.getAffectedAt() != null) {
        // claimDto.setAffectedAt(claimDto.convertDate(claim.getAffectedAt()));
        // }
        return claimDto;
    }

    private ChatDto convertToDto(Chat chat) {
        ChatDto chatDto = modelMapper.map(chat, ChatDto.class);
        if (chat.getMessages() != null && !chat.getMessages().isEmpty()) {
            chatDto.setMessages(chat.getMessages().stream().map(this::convertToDto).collect(Collectors.toList()));
        }

        if (chat.getMembers() != null && !chat.getMembers().isEmpty()) {
            chatDto.setMembers(chat.getMembers().stream().map(this::convertToResponse).collect(Collectors.toList()));
        }

        if (chat.getGuests() != null && !chat.getGuests().isEmpty()) {
            chatDto.setGuests(chat.getGuests().stream().map(this::convertToResponse).collect(Collectors.toList()));
        }

        if (chat.getVote() != null && !chat.getVote().isEmpty()) {
            chatDto.setVote(chat.getVote().stream().map(this::convertToDto).collect(Collectors.toList()));
        }
        return chatDto;
    }

    private MessageDto convertToDto(Message message) {
        MessageDto messageDto = modelMapper.map(message, MessageDto.class);
        if (message.getChat() != null) {
            messageDto.setChatId(message.getChat().getId());
        }
        if (message.getCreatedAt() != null) {
            messageDto.setCreatedAt(Utils.convertLocalDateTimeToStr(message.getCreatedAt()));
        }
        if (message.getLinkedVote() != null) {
            messageDto.setVoteDto(convertToDto(message.getLinkedVote()));
        }

        if (message.isVote()) {
            messageDto.setVote(message.isVote());
        }
        return messageDto;
    }

    private VoteDto convertToDto(Vote vote) {
        VoteDto voteDto = modelMapper.map(vote, VoteDto.class);

        if (vote.getChat() != null) {
            voteDto.setChatId(vote.getChat().getId());
        }
        if (vote.getUserVotes() != null && !vote.getUserVotes().isEmpty()) {
            voteDto.setUserVote(vote.getUserVotes().stream().map(this::convertToDto).collect(Collectors.toList()));
        }
        if (vote.getMessage() != null) {
            voteDto.setMessageId(vote.getMessage().getId());
        }
        return voteDto;
    }

    private UserVoteDto convertToDto(UserVote userVote) {
        UserVoteDto userVoteDto = modelMapper.map(userVote, UserVoteDto.class);
        userVoteDto.setAuthor(this.convertToResponse(userVote.getUser()));
        return userVoteDto;
    }

    private SolutionDto convertToDto(Solution solution) {
        SolutionDto solutionDto = modelMapper.map(solution, SolutionDto.class);
        if (solution.getSatisfactionMeasure() != null) {
            solutionDto.setSatisfactionMeasureDto(convertToDto(solution.getSatisfactionMeasure()));
        }

        if (solution.getAuthor() != null) {
            solutionDto.setAuthor(convertToResponse(solution.getAuthor()));
        }

        if (solution.getApprouver() != null) {
            solutionDto.setApprouver(convertToResponse(solution.getApprouver()));
        }

        if (solution.getUnApprouver() != null) {
            solutionDto.setUnApprouver(convertToResponse(solution.getUnApprouver()));
        }

        return solutionDto;
    }

    private SatisfactionMeasureDto convertToDto(SatisfactionMeasure satisfactionMeasure) {
        SatisfactionMeasureDto satisfactionMeasureDto = modelMapper.map(satisfactionMeasure,
                SatisfactionMeasureDto.class);
        satisfactionMeasureDto.setMeasurer(convertToResponse(satisfactionMeasure.getMeasurer()));
        return satisfactionMeasureDto;
    }

    private ServicePointResponse convertToResponse(ServicePoint servicepoint1) {
        ServicePointResponse servicePointResponse = modelMapper.map(servicepoint1, ServicePointResponse.class);
        return servicePointResponse;
    }

    private ProductResponse convertToResponse(Product product) {
        ProductResponse productResponse = modelMapper.map(product, ProductResponse.class);
        return productResponse;
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

    private LanguageResponse convertToResponse(Language language) {
        LanguageResponse languageResponse = modelMapper.map(language, LanguageResponse.class);
        return languageResponse;
    }

    private CollectionChannelResponse convertToResponse(CollectionChannel collectionChannel) {
        CollectionChannelResponse collectionChannelResponse = modelMapper.map(collectionChannel,
                CollectionChannelResponse.class);
        return collectionChannelResponse;
    }

    private ExternalRecourseResponse convertToResponse(ExternalRecourse externalRecourse) {
        ExternalRecourseResponse externalRecourseResponse = modelMapper.map(externalRecourse,
                ExternalRecourseResponse.class);
        return externalRecourseResponse;
    }
    private ExtraContentResponse convertToResponse(ExtraContent extraContent) {
        if(!extraContent.isFile()){

            ExtraContentResponse extraContentResponse = modelMapper.map(extraContent,
            ExtraContentResponse.class);
            // satisfactionMeasureDto.setMeasurer(convertToResponse(satisfactionMeasure.getMeasurer()));
            return extraContentResponse;
        }else{
            return null;
        }
    }

    @PostMapping("/delete/soft")
    public ResponseEntity<ApiResponseDto> deleteClaim(@RequestBody ClaimDeleteRequest request, HttpServletRequest request2) {
        ApiResponseDto apiResponseDto;
       
        try {
            UserDetails collectorDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication()
                .getPrincipal();
            User connectedUser = User.builder().build();
            try {
                connectedUser = authService.getByEmail(collectorDetails.getUsername());
            } catch (Exception e) {
                apiResponseDto = ApiResponseDto
                        .builder()
                        .status(false)
                        .content(ErrorResponse.builder().message("Utilisateur introuvable")
                                .title("NOT FOUND EXCEPTION")
                                .build())
                        .build();
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
            }
            Claim claim = new Claim();
            try {
                claim = service.getByCodeClient(request.getClaimCode());
            } catch (Exception e) {
                apiResponseDto = ApiResponseDto
                        .builder()
                        .status(false)
                        .content(ErrorResponse.builder().message("Dénonciation non trouvée")
                                .title("NOT FOUND EXCEPTION")
                                .build())
                        .build();
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
            }

            // 🔎 Vérification du statut
            if (claim.getStatus() != ClaimStatus.SAVED) {
                apiResponseDto = ApiResponseDto.builder()
                    .status(false)
                    .content(ErrorResponse.builder()
                            .message("Action non autorisée : la dénonciation n'est pas au statut Enregistrée.")
                            .title("INVALID STATUS")
                            .build())
                    .build();
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponseDto);
            }

            if (connectedUser.getPoste().getHabilitations().contains("H12")) {
                try {
                   
                    // Vérifie que la dénonciation n’est pas déjà supprimée
                    if (Boolean.TRUE.equals(claim.isDeleted())) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                            ApiResponseDto.builder()
                                .status(false)
                                .content("Cette dénonciation a déjà été supprimée !!!")
                                .build()
                        );
                    }else{
                        service.deleteById(claim,request.getReason(),connectedUser);
                        return ResponseEntity.ok(
                            ApiResponseDto.builder().status(true).content("Dénonciation supprimée avec succès.").build()
                        );
                    }
                  
                } catch (NotFoundException e) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        ApiResponseDto.builder().status(false).content("Erreur lors de la suppression.").build()
                    );
                }
            } else {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    ApiResponseDto.builder()
                        .status(false)
                        .content("Vous n’êtes pas autorisé à effectuer cette action.")
                        .build()
                );
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponseDto.builder()
                    .status(false)
                    .content(ErrorResponse.builder().title("Erreur").message(e.getMessage()).build())
                    .build()
            );
        }
    }


    @PostMapping("/restore/{id}")
    public ResponseEntity<ApiResponseDto> restoreClaim(@PathVariable Long id, HttpServletRequest request2) {
        ApiResponseDto apiResponseDto;
        try {
            UserDetails collectorDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication()
                .getPrincipal();
            User connectedUser = User.builder().build();

            try {
                connectedUser = authService.getByEmail(collectorDetails.getUsername());
            } catch (Exception e) {
                apiResponseDto = ApiResponseDto
                        .builder()
                        .status(false)
                        .content(ErrorResponse.builder().message("Utilisateur introuvable")
                                .title("NOT FOUND EXCEPTION")
                                .build())
                        .build();
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
            }
            Claim claim = new Claim();
            try {
                claim = service.getById(id);
            } catch (Exception e) {
                apiResponseDto = ApiResponseDto
                        .builder()
                        .status(false)
                        .content(ErrorResponse.builder().message("Dénonciation non trouvée")
                                .title("NOT FOUND EXCEPTION")
                                .build())
                        .build();
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
            }

            if (connectedUser.getPoste().getHabilitations().contains("H12")) {
                try {
                   
                    // Vérifie que la Dénonciation n’est pas déjà supprimée
                    if (Boolean.FALSE.equals(claim.isDeleted())) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                            ApiResponseDto.builder()
                                .status(false)
                                .content("Cette dénonciation n'a pas été supprimée !!!")
                                .build()
                        );
                    }else{
                        service.restoreById(id,connectedUser);
                        return ResponseEntity.ok(
                            ApiResponseDto.builder().status(true).content("Dénonciation supprimée avec succès.").build()
                        );
                    }
                  
                } catch (NotFoundException e) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        ApiResponseDto.builder().status(false).content("Erreur lors de la suppression.").build()
                    );
                }
            } else {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    ApiResponseDto.builder()
                        .status(false)
                        .content("Vous n’êtes pas autorisé à effectuer cette action.")
                        .build()
                );
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponseDto.builder()
                    .status(false)
                    .content(ErrorResponse.builder().title("Erreur").message(e.getMessage()).build())
                    .build()
            );
        }
    }


}
