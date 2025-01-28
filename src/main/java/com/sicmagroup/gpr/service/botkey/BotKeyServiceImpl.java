package com.sicmagroup.gpr.service.botkey;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.sicmagroup.gpr.api.claim.ClaimController;
import com.sicmagroup.gpr.api.claim.ClaimRequest;
import com.sicmagroup.gpr.api.claim.SaveRequest;
import com.sicmagroup.gpr.api.denunciation.DenunciationController;
import com.sicmagroup.gpr.api.suggestion.SuggestionAddRequest;
import com.sicmagroup.gpr.api.suggestion.SuggestionRequest;
import com.sicmagroup.gpr.domain.dto.ClaimDto;
import com.sicmagroup.gpr.domain.dto.SuggestionDto;
import com.sicmagroup.gpr.domain.dto.botkey.BotKeyConfigResponse;
import com.sicmagroup.gpr.domain.enumeration.ClaimStatus;
import com.sicmagroup.gpr.domain.enumeration.ClaimType;
import com.sicmagroup.gpr.domain.enumeration.Gender;
import com.sicmagroup.gpr.domain.enumeration.LogTarget;
import com.sicmagroup.gpr.domain.enumeration.LogType;
import com.sicmagroup.gpr.domain.model.ApiKey;
import com.sicmagroup.gpr.domain.model.CategorieObjet;
import com.sicmagroup.gpr.domain.model.Claim;
import com.sicmagroup.gpr.domain.model.ClaimAudio;
import com.sicmagroup.gpr.domain.model.CollectionChannel;
import com.sicmagroup.gpr.domain.model.ExistingSolution;
import com.sicmagroup.gpr.domain.model.ExternalRecourse;
import com.sicmagroup.gpr.domain.model.Language;
import com.sicmagroup.gpr.domain.model.Log;
import com.sicmagroup.gpr.domain.model.Media;
import com.sicmagroup.gpr.domain.model.Objet;
import com.sicmagroup.gpr.domain.model.Poste;
import com.sicmagroup.gpr.domain.model.Product;
import com.sicmagroup.gpr.domain.model.ServicePoint;
import com.sicmagroup.gpr.domain.model.Suggestion;
import com.sicmagroup.gpr.repository.ApiKeyRepository;
import com.sicmagroup.gpr.repository.CategorieObjetRepository;
import com.sicmagroup.gpr.repository.ClaimRepository;
import com.sicmagroup.gpr.repository.CollectionChannelRespository;
import com.sicmagroup.gpr.repository.ExistingSolutionRepository;
import com.sicmagroup.gpr.repository.ExternalRecourseRepository;
import com.sicmagroup.gpr.repository.LanguageRepository;
import com.sicmagroup.gpr.repository.ObjetRepository;
import com.sicmagroup.gpr.repository.PosteRepository;
import com.sicmagroup.gpr.repository.ProductRepository;
import com.sicmagroup.gpr.repository.ServicePointRepository;
import com.sicmagroup.gpr.repository.SuggestionRepository;
import com.sicmagroup.gpr.service.claimAudio.ClaimAudioServiceImpl;
import com.sicmagroup.gpr.service.faq.FaqServiceImpl;
import com.sicmagroup.gpr.service.log.LogServiceImpl;
import com.sicmagroup.gpr.service.media.MediaServiceImpl;
import com.sicmagroup.gpr.utils.Utils;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BotKeyServiceImpl implements BotKeyService {
    private final ServicePointRepository servicePointRepository;

    private final PosteRepository posteRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final ProductRepository productRepository;
    private final ObjetRepository objetRepository;
    private final LanguageRepository languageRepository;
    private final CollectionChannelRespository collectionChannelRespository;
    private final ExternalRecourseRepository externalRecourseRepository;
    private final ClaimRepository claimRepository;
    private final SuggestionRepository suggestionRepository;
    private final ExistingSolutionRepository existingSolutionRepository;
    private final CategorieObjetRepository categorieObjetRepository;
    private final ClaimAudioServiceImpl claimAudioServiceImpl;
    private final MediaServiceImpl mediaServiceImpl;
    private final LogServiceImpl logServiceImpl;
    private final PasswordEncoder passwordEncoder;

    private final ClaimController claimController;

    private final ModelMapper modelMapper;

    @Override
    public Boolean checkApiKeyBoolean(HttpServletRequest request) {

        String key = request.getHeader("API_KEY");
        String secret = request.getHeader("API_SECRET");
        if (key == null || secret == null) {
            return false;
        }
         
        Optional<ApiKey> optApiKey = apiKeyRepository.findByCle(key);
        if(optApiKey.isPresent()){
           ApiKey apiKey = optApiKey.get();
           return passwordEncoder.matches(secret,apiKey.getSecret());
        }else{

            return false;
        }
       
    }

    @Override
    public BotKeyConfigResponse getConfig() {
        try {

            List<ServicePoint> servicePoints = servicePointRepository.findByIsDeleted(false);

            List<Poste> postes = posteRepository.findByIsDeleted(false);
            // Product
            List<Product> products = productRepository.findByIsDeleted(false);
            // objet
            List<Objet> objets = objetRepository.findByIsDeleted(false);
            // language
            List<Language> languages = languageRepository.findByIsDeleted(false);

            // collectionChannel
            List<CollectionChannel> collectionChannels = collectionChannelRespository.findByIsDeleted(false);
            // External Recourse
            List<ExternalRecourse> externalRecourses = externalRecourseRepository.findByIsDeleted(false);

            // existing solutions
            List<ExistingSolution> existingSolutions = existingSolutionRepository.findAll();
            // categorie objet
            List<CategorieObjet> categorieObjets = categorieObjetRepository.findAll();

            return BotKeyConfigResponse.builder()
                    .servicePoints(servicePoints)
                    .categorieObjets(categorieObjets)
                    .languages(languages)
                    .modalites(collectionChannels)
                    .objets(objets)
                    .products(products)
                    .existingSolutions(existingSolutions)
                    .externalRecourses(externalRecourses)
                    .postes(postes)
                    .build();
        } catch (Exception e) {
            return BotKeyConfigResponse.builder().build();
        }
    }

    @Override
    public List<SuggestionDto> getSuggestions(String userCode) {
        try {

            if (!userCode.startsWith("bot") || userCode.length() <10) {
                return null;
            }
            
            List<Suggestion> suggestions = suggestionRepository.findByCodeStartsWith(userCode);
            return suggestions.stream().map(this::convertToDto).collect(Collectors.toList());

        } catch (Exception e) {
            return null;
        }
        
    }


    @Override
    public SuggestionDto getSuggestion(String code) {
        try {

            Suggestion suggestion = suggestionRepository.findByCodeClient(code)
                    .orElseThrow(() -> new Exception("La suggestion est introuvable"));

            return convertToDto(suggestion);

        } catch (Exception e) {
            return null;
        }
        
    }
  


    @Override
    public SuggestionDto saveSuggestion (SuggestionAddRequest request,String botName) throws Exception {
        SuggestionRequest suggestionRequest = request.getSuggestionRequest();
        Suggestion suggestion = Suggestion
                .builder()
                .build();
    
        if (suggestionRequest.getId() != null) {
            Suggestion oldSuggestion = suggestionRepository.findById(suggestionRequest.getId())
                    .orElseThrow(() -> new Exception("Aucune réclamation ne porte ce code"));
            if (!suggestion.getCode().startsWith("bot")) {
                throw new Exception("Vous n'avez pas l'autorisation");
            }
            suggestion = oldSuggestion;
        } else {
            // if (suggestionRequest.getCode() == null || suggestionRequest.getCode().isEmpty()) {
            //     throw new Exception("Donnez un identifiant unique à ce utilisateur pour pouvoir relier tous ces plaintes et suggestions à lui");
            // }else if(suggestionRequest.getCode().trim().length()<10){
            //     throw new Exception("Le taille de l'identifiant doit etre au moins de 10 carateres");
            // }
            //  else {
                String code ="bot-"+"-"+botName+"-"+UUID.randomUUID().toString().substring(0,8);
                suggestion.setCode(code);
                String codeClient = "SUG-" + UUID.randomUUID().toString().substring(0, 4);
                suggestion.setCodeClient(codeClient);
            // }
        }

        CollectionChannel collectionChannel;
        if (suggestionRequest.getCollectionChannelId() != null) {
            collectionChannel = collectionChannelRespository.findById(suggestionRequest.getCollectionChannelId()).orElseThrow(()-> new Exception("Collection channelle choosed not found"));
            suggestion.setCanal(collectionChannel);
        }

        ServicePoint servicePoint;
        if (suggestionRequest.getServicePointId() != null) {
                servicePoint = servicePointRepository.findById(suggestionRequest.getServicePointId()).orElseThrow(()-> new Exception("Service point choosed not found"));
                suggestion.setServiceIndexe(servicePoint);
        }

        Product product;
        if (suggestionRequest.getProductId() != null) {
            
                product = productRepository.findById(suggestionRequest.getProductId()).orElseThrow(()-> new Exception("Prodcut choosed not found"));;
                suggestion.setProduit(product);
           
        }

        Language language;
        if (suggestionRequest.getLanguageId() != null) {
          
                language = languageRepository.findById(suggestionRequest.getLanguageId()).orElseThrow(()-> new Exception("Language choosed not found"));;
                suggestion.setLangue(language);
            
        }
        if (suggestionRequest.getClientFirstAndLastName() != null) {
            suggestion.setClientFirstAndLastName(suggestionRequest.getClientFirstAndLastName());
        }

        if (suggestionRequest.getGender() != null && !suggestionRequest.getGender().equals("")) {
            suggestion.setGender(Gender.valueOf(suggestionRequest.getGender()));
        } else {
            suggestion.setGender(Gender.NON_DEFINI);
        }

        if (suggestionRequest.getAddress() != null) {
            suggestion.setAddress(suggestionRequest.getAddress());
        }

        if (suggestionRequest.getPhone() != null) {
            suggestion.setTel(suggestionRequest.getPhone());
        }

        if (suggestionRequest.getFolderCode() != null) {
            suggestion.setFolderCode(suggestionRequest.getFolderCode());
        }

        if (suggestionRequest.getContent() != null) {
            suggestion.setContent(suggestionRequest.getContent());
        }

        // suggestion.setCollecteur(collector);

        suggestion.setStatus(ClaimStatus.TEMP_SAVED);
        suggestion.setCreatedAt(LocalDateTime.now());
        // if (suggestionRequest.getReceiptDateTime() != null && !suggestionRequest.getReceiptDateTime().isEmpty()) {
        //     suggestion.setReceiptDateTime(Utils.convertStrToLocalDateTime(suggestionRequest.getReceiptDateTime()));
            suggestion.setReceiptDateTime(LocalDateTime.now());
        // }
        suggestion = suggestionRepository.save(suggestion);

        if (request.getFiles() != null && request.getFiles().length != 0) {
            // System.out.println("test");
            // System.out.println(claim.getCode());
            List<Media> medias = mediaServiceImpl.store(request.getFiles(), suggestion);
            suggestion.setUpdatedAt(LocalDateTime.now());
            suggestion.setFiles(medias);
            suggestion = suggestionRepository.save(suggestion);
        }
        if (request.getAudios() != null && request.getAudios().length != 0) {
            List<ClaimAudio> audios = claimAudioServiceImpl.store(request.getAudios(),suggestion);
            suggestion.setUpdatedAt(LocalDateTime.now());
            suggestion = suggestionRepository.save(suggestion);
            
            // for (ClaimAudio audio : audios) {
            // audio.setClaim(null);
            // }
            // claim.setAudios(audios);
        }

        return convertToDto(suggestion);
    }

    @Override
    public ClaimDto saveClaim(SaveRequest claimPart, String botName,ClaimType type) throws Exception {
        ClaimRequest claimToSave = claimPart.getClaimRequest();
        Log log = Log
                .builder().build();
        Claim claim = Claim
                .builder().build();
       
   
        log.setTarget(LogTarget.CLAIM);
        log.setLibelle("Enregistrement d'une reclamation depuis un bot");

        if (claimToSave.getId() != null) {
            Claim oldClaim = claimRepository.findById(claimToSave.getId())
                    .orElseThrow(() -> new Exception("Aucune réclamation ne porte ce code"));
            if (!oldClaim.getCode().startsWith("bot")) {
                throw new Exception("Vous n'avez pas l'autorisation");
            }
            claim = oldClaim;
        } else {
            // if (claimToSave.getCode() == null || claimToSave.getCode().isEmpty()) {
            //     throw new Exception("Donnez un identifiant unique à ce utilisateur pour pouvoir relier tous ces plaintes et suggestions à lui");
            // }else if(claimToSave.getCode().trim().length()<10){
            //     throw new Exception("Le taille de l'identifiant doit etre au moins de 10 carateres");
            // }
            //  else {
                String code ="bot-"+"-"+botName+"-"+UUID.randomUUID().toString().substring(0,10);
                claim.setCode(code);
                String codeClient = "REC-" + UUID.randomUUID().toString().substring(0, 4);
                claim.setCodeClient(codeClient);

            // }
        }

        // if (claimToSave.getCode() != null && !claimToSave.getCode().isEmpty()) {
        // Claim oldClaim = repository.findByCode(claimToSave.getCode())
        // .orElseThrow(() -> new Exception("Aucune réclamation ne porte ce code"));
        // claim = oldClaim;
        // } else {
        // String code = generateCode(collector.getServicePoint().getUuid(),
        // collector.getCode(), type);
        // claim.setCode(code);
        // }

        CollectionChannel collectionChannel;
        if (claimToSave.getCollectionChannelId() != null) {
            collectionChannel = collectionChannelRespository.findById(claimToSave.getCollectionChannelId()).orElseThrow(()-> new Exception("Collection channelle choosed not found"));
            claim.setCollectionChannel(collectionChannel);
        }

        ServicePoint servicePoint;
        if (claimToSave.getServicePointId() != null) {
                servicePoint = servicePointRepository.findById(claimToSave.getServicePointId()).orElseThrow(()-> new Exception("Service point choosed not found"));
                claim.setServicePoint(servicePoint);
        }

        Product product;
        if (claimToSave.getProductId() != null) {
            
            product = productRepository.findById(claimToSave.getProductId()).orElseThrow(()-> new Exception("Prodcut choosed not found"));;
            claim.setProduct(product);
           
        }

        Language language;
        if (claimToSave.getLanguageId() != null) {
          
                language = languageRepository.findById(claimToSave.getLanguageId()).orElseThrow(()-> new Exception("Language choosed not found"));;
                claim.setLanguage(language);
            
        }
        Objet objet;
        if (claimToSave.getLanguageId() != null) {
          
                objet = objetRepository.findById(claimToSave.getObjetId()).orElseThrow(()-> new Exception("Objet choosed not found"));;
                claim.setObjet(objet);
            
        }

      

        if (claimToSave.getClientFirstAndLastName() != null) {
            claim.setClientFirstAndLastName(claimToSave.getClientFirstAndLastName());
        }

        if (claimToSave.getGender() != null && !claimToSave.getGender().equals("")) {
            claim.setGender(Gender.valueOf(claimToSave.getGender()));
        }

        if (claimToSave.getAddress() != null) {
            claim.setAddress(claimToSave.getAddress());
        }
        claim.setType(type);
        if (claimToSave.getPhone() != null) {
            claim.setTel(claimToSave.getPhone());
        }


        if (claimToSave.getFolderCode() != null) {
            claim.setFolderCode(claimToSave.getFolderCode());
        }

        if (claimToSave.getContent() != null) {
            claim.setContent(claimToSave.getContent());
        }

       
        claim.setStatus(ClaimStatus.TEMP_SAVED);
        claim.setCreatedAt(LocalDateTime.now());
        // if (claimToSave.getReceiptDateTime() != null && !claimToSave.getReceiptDateTime().isEmpty()) {
            claim.setReceiptDateTime(LocalDateTime.now());
        
        // }
        if (claimToSave.getOnlineUploadDateTime() != null) {
            claim.setOnlineUploadDateTime(claimToSave.getOnlineUploadDateTime());
        }
        claim = claimRepository.save(claim);

        log.setContent("code: " + claim.getCode());
        log.setCreatedAt(LocalDateTime.now());
        log.setType(LogType.INFO);
        // log.setUserId(claim.getCollector().getId());
        log.setUserIpAddress(claimPart.getRemoteAddress());
        

        logServiceImpl.saveLog(log);

        if (claimPart.getAudios() != null && claimPart.getAudios().length != 0) {
            List<ClaimAudio> audios = claimAudioServiceImpl.store(claimPart.getAudios(), claim);
            claim.setUpdatedAt(LocalDateTime.now());
            // for (ClaimAudio audio : audios) {
            // audio.setClaim(null);
            // }
            // claim.setAudios(audios);

        }

        if (claimPart.getFiles() != null && claimPart.getFiles().length != 0) {
            // System.out.println("test");
            // System.out.println(claim.getCode());
            List<Media> medias = mediaServiceImpl.store(claimPart.getFiles(), claim);
            claim.setUpdatedAt(LocalDateTime.now());
            // claim.setMedias(medias);
        }
        claim = claimRepository.save(claim);
        return claimController.convertToDto(claim);
    }
    @Override
    public List<ClaimDto> getClaims(String userCode, ClaimType type) {
        
        try {

            if (!userCode.startsWith("bot") || userCode.length() <10) {
                return null;
            }
            
            List<Claim> claims = claimRepository.findByTypeAndCodeStartsWith(type,userCode);

            return claims.stream().map(claimController::convertToDto).collect(Collectors.toList());

        } catch (Exception e) {
            return null;
        }
        
    }

    @Override
    public Claim getClaim(String code) {
        try {

            Claim claim = claimRepository.findByCodeClient(code)
                    .orElseThrow(() -> new Exception("La réclamation est introuvable"));

            return claim;
            // return convertToDto(claim);

        } catch (Exception e) {
            return null;
        }
    }

    // @Override
    // public Claim getClaim(String code) throws Exception {
    //     return claimRepository.findByCodeClient(code).orElseThrow(() -> new Exception("Réclamation introuvable"));
    // }

    @Override
    public Claim updateClaim(Long code) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'updateClaim'");
    }

    @Override
    public Claim deleteClaim(Long code) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'deleteClaim'");
    }

    @Override
    public Claim addFile(Long code) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'addFile'");
    }

    @Override
    public Claim mesureSatistafaction(Long code) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'mesureSatistafaction'");
    }

    private SuggestionDto convertToDto(Suggestion suggestion) {
        SuggestionDto suggestionDto = modelMapper.map(suggestion, SuggestionDto.class);
        if (suggestion.getCreatedAt() != null) {
            suggestionDto.setCreatedAt(suggestion.getCreatedAt().toString());
            // System.out.println(suggestionDto.getCreatedAt());
        }
        if (suggestion.getUpdatedAt() != null) {
            suggestionDto.setUpdatedAt(suggestion.getUpdatedAt().toString());
            // System.out.println(suggestionDto.getUpdatedAt());
        }

        if (suggestion.getReceiptDateTime() != null) {
            suggestionDto.setReceiptDateTime(suggestion.getReceiptDateTime().toString());
        }

        if (suggestion.getTreatAt() != null) {
            suggestionDto.setTreatAt(suggestion.getTreatAt().toString());
        }

        return suggestionDto;
    }

    private ClaimDto convertToDto(Claim claim) {
        ClaimDto claimDto = modelMapper.map(claim, ClaimDto.class);
        if (claim.getCreatedAt() != null) {
            claimDto.setCreatedAt(claim.getCreatedAt().toString());
            // System.out.println(suggestionDto.getCreatedAt());
        }
        if (claim.getUpdatedAt() != null) {
            claimDto.setUpdatedAt(claim.getUpdatedAt().toString());
            // System.out.println(suggestionDto.getUpdatedAt());
        }

        if (claim.getReceiptDateTime() != null) {
            claimDto.setReceiptDateTime(claim.getReceiptDateTime().toString());
        }

        // if (claim.getTreatAt() != null) {
        //     claimDto.setTreatAt(claim.getTreatAt().toString());
        // }

        return claimDto;
    }


}
