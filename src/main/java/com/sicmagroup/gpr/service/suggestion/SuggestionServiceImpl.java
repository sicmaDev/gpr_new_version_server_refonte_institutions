package com.sicmagroup.gpr.service.suggestion;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.StreamReadConstraints.Builder;
import com.sicmagroup.gpr.api.suggestion.SuggestionAddRequest;
import com.sicmagroup.gpr.api.suggestion.SuggestionRequest;
import com.sicmagroup.gpr.api.suggestion.TreatSuggestionRequest;
import com.sicmagroup.gpr.domain.enumeration.ClaimStatus;
import com.sicmagroup.gpr.domain.enumeration.Gender;
import com.sicmagroup.gpr.domain.model.Claim;
import com.sicmagroup.gpr.domain.model.ClaimAudio;
import com.sicmagroup.gpr.domain.model.CollectionChannel;
import com.sicmagroup.gpr.domain.model.Language;
import com.sicmagroup.gpr.domain.model.Media;
import com.sicmagroup.gpr.domain.model.Product;
import com.sicmagroup.gpr.domain.model.ServicePoint;
import com.sicmagroup.gpr.domain.model.Suggestion;
import com.sicmagroup.gpr.domain.model.User;
import com.sicmagroup.gpr.repository.SuggestionRepository;
import com.sicmagroup.gpr.service.auth.AuthenticationServiceImpl;
import com.sicmagroup.gpr.service.claimAudio.ClaimAudioServiceImpl;
import com.sicmagroup.gpr.service.collectionChannel.CollectionChannelServiceImpl;
import com.sicmagroup.gpr.service.language.LanguageServiceImpl;
import com.sicmagroup.gpr.service.media.MediaServiceImpl;
import com.sicmagroup.gpr.service.objet.ObjetServcieImpl;
import com.sicmagroup.gpr.service.product.ProductServiceImpl;
import com.sicmagroup.gpr.service.servicePoint.ServicePointServiceImpl;
import com.sicmagroup.gpr.utils.Utils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SuggestionServiceImpl implements SuggestionService {
    private final SuggestionRepository repository;
    private final CollectionChannelServiceImpl collectionChannelService;
    private final ServicePointServiceImpl servicePointServiceImpl;
    private final ProductServiceImpl productServiceImpl;
    private final ObjetServcieImpl objetServcieImpl;
    private final ClaimAudioServiceImpl claimAudioServiceImpl;

    private final LanguageServiceImpl languageServiceImpl;
    private final AuthenticationServiceImpl authServiceImpl;
    private final MediaServiceImpl mediaServiceImpl;

    @Override
    public List<Suggestion> getAll() {
        return repository.findAll();
    }


    @Override
    public List<Suggestion> getAllByStatus(ClaimStatus status) {
        return repository.findByStatus(status);
    }

    @Override
    public Suggestion getById(Long id) throws Exception {
        return repository.findById(id).orElseThrow(() -> new Exception("Suggestion introuvable"));
    }

    @Override
    public Suggestion saveSuggestion(SuggestionAddRequest request, ClaimStatus status) throws Exception {
        SuggestionRequest suggestionRequest = request.getSuggestionRequest();
        User collector;
        Suggestion suggestion = Suggestion
                .builder()
                .build();
        try {
            collector = authServiceImpl.getById(suggestionRequest.getCollectorId());
        } catch (Exception e) {
            throw new Exception("Collector of the claim not found");
        }

        if (suggestionRequest.getId() != null) {
            Suggestion oldSuggestion = repository.findById(suggestionRequest.getId())
                    .orElseThrow(() -> new Exception("Aucune réclamation ne porte ce code"));
            suggestion = oldSuggestion;
        } else {
            if (suggestionRequest.getCode() != null && !suggestionRequest.getCode().isEmpty()) {
                suggestion.setCode(suggestionRequest.getCode());
            } else {
                String code = generateCode(collector.getServicePoint().getUuid(), collector.getCode());
                suggestion.setCode(code);
                String codeClient = "SUG-" + UUID.randomUUID().toString().substring(0, 4);
                suggestion.setCodeClient(codeClient);
            }
        }

        // if (suggestionRequest.getCode() != null && !suggestionRequest.getCode().isEmpty()) {
        //     Suggestion oldSuggestion = repository.findByCode(suggestionRequest.getCode())
        //             .orElseThrow(() -> new Exception("Aucune réclamation ne porte ce code"));
        //     suggestion = oldSuggestion;
        // } else {
        //     String code = generateCode(collector.getServicePoint().getUuid(), collector.getCode());
        //     suggestion.setCode(code);
        // }
        CollectionChannel collectionChannel;
        if (suggestionRequest.getCollectionChannelId() != null) {
            try {
                collectionChannel = collectionChannelService.getById(suggestionRequest.getCollectionChannelId());
                suggestion.setCanal(collectionChannel);
            } catch (Exception e) {
                throw new Exception("Collection channelle choosed not found");
            }
        }

        ServicePoint servicePoint;
        if (suggestionRequest.getServicePointId() != null) {
            try {
                servicePoint = servicePointServiceImpl.getById(suggestionRequest.getServicePointId());
                suggestion.setServiceIndexe(servicePoint);
            } catch (Exception e) {
                throw new Exception("Service Point choosed not found");
            }
        }

        Product product;
        if (suggestionRequest.getProductId() != null) {
            try {
                product = productServiceImpl.getById(suggestionRequest.getProductId());
                suggestion.setProduit(product);
            } catch (Exception e) {
                throw new Exception("Product choosed not found");
            }
        }

        Language language;
        if (suggestionRequest.getLanguageId() != null) {
            try {
                language = languageServiceImpl.getById(suggestionRequest.getLanguageId());
                suggestion.setLangue(language);
            } catch (Exception e) {
                throw new Exception("Objet choosed not found");
            }
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

        if (suggestionRequest.getCrew() != null) {
            suggestion.setCrew(suggestionRequest.getCrew());
        }

        if (suggestionRequest.getFolderCode() != null) {
            suggestion.setFolderCode(suggestionRequest.getFolderCode());
        }

        if (suggestionRequest.getContent() != null) {
            suggestion.setContent(suggestionRequest.getContent());
        }

        suggestion.setCollecteur(collector);

        suggestion.setStatus(status);
        suggestion.setCreatedAt(LocalDateTime.now());
        if (suggestionRequest.getReceiptDateTime() != null && !suggestionRequest.getReceiptDateTime().isEmpty()) {
            suggestion.setReceiptDateTime(Utils.convertStrToLocalDateTime(suggestionRequest.getReceiptDateTime()));
        }
        suggestion = repository.save(suggestion);

        if (request.getFiles() != null && request.getFiles().length != 0) {
            // System.out.println("test");
            // System.out.println(claim.getCode());
            List<Media> medias = mediaServiceImpl.store(request.getFiles(), suggestion);
            suggestion.setUpdatedAt(LocalDateTime.now());
            suggestion.setFiles(medias);
            suggestion = repository.save(suggestion);
        }
        if (request.getAudios() != null && request.getAudios().length != 0) {
            List<ClaimAudio> audios = claimAudioServiceImpl.store(request.getAudios(),suggestion);
            suggestion.setUpdatedAt(LocalDateTime.now());
            suggestion = repository.save(suggestion);
            
            // for (ClaimAudio audio : audios) {
            // audio.setClaim(null);
            // }
            // claim.setAudios(audios);
        }

        return suggestion;
    }
   
    
    @Override
    public Suggestion botSaveSuggestion(SuggestionAddRequest request,String botName) throws Exception {
        SuggestionRequest suggestionRequest = request.getSuggestionRequest();
        Suggestion suggestion = Suggestion
                .builder()
                .build();
      



        if (suggestionRequest.getId() != null) {
            Suggestion oldSuggestion = repository.findById(suggestionRequest.getId())
                    .orElseThrow(() -> new Exception("Aucune réclamation ne porte ce code"));
            if (!suggestion.getCode().startsWith("bot")) {
                throw new Exception("Vous n'avez pas l'autorisation");
            }
            suggestion = oldSuggestion;
        } else {
            if (suggestionRequest.getCode() == null || !suggestionRequest.getCode().isEmpty()) {
                throw new Exception("Donnez un identifiant unique à ce utilisateur pour pouvoir relier tous ces plaintes et suggestions à lui");
            }else if(suggestionRequest.getCode().length()<10){
                throw new Exception("Le taille de l'identifiant doit etre au moins de 10 carateres");
            }
             else {
                String code ="bot-"+suggestion.getCode()+"-"+botName+"-"+UUID.randomUUID().toString().substring(0,10);
                suggestion.setCode(code);
            }
        }

        CollectionChannel collectionChannel;
        if (suggestionRequest.getCollectionChannelId() != null) {
            try {
                collectionChannel = collectionChannelService.getById(suggestionRequest.getCollectionChannelId());
                suggestion.setCanal(collectionChannel);
            } catch (Exception e) {
                throw new Exception("Collection channelle choosed not found");
            }
        }

        ServicePoint servicePoint;
        if (suggestionRequest.getServicePointId() != null) {
            try {
                servicePoint = servicePointServiceImpl.getById(suggestionRequest.getServicePointId());
                suggestion.setServiceIndexe(servicePoint);
            } catch (Exception e) {
                // throw new Exception("Service Point choosed not found");
            }
        }

        Product product;
        if (suggestionRequest.getProductId() != null) {
            try {
                product = productServiceImpl.getById(suggestionRequest.getProductId());
                suggestion.setProduit(product);
            } catch (Exception e) {
                // throw new Exception("Product choosed not found");
            }
        }

        Language language;
        if (suggestionRequest.getLanguageId() != null) {
            try {
                language = languageServiceImpl.getById(suggestionRequest.getLanguageId());
                suggestion.setLangue(language);
            } catch (Exception e) {
                // throw new Exception("Objet choosed not found");
            }
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

        if (suggestionRequest.getCrew() != null) {
            suggestion.setCrew(suggestionRequest.getCrew());
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
        if (suggestionRequest.getReceiptDateTime() != null && !suggestionRequest.getReceiptDateTime().isEmpty()) {
            suggestion.setReceiptDateTime(Utils.convertStrToLocalDateTime(suggestionRequest.getReceiptDateTime()));
        }
        suggestion = repository.save(suggestion);

        if (request.getFiles() != null && request.getFiles().length != 0) {
            // System.out.println("test");
            // System.out.println(claim.getCode());
            List<Media> medias = mediaServiceImpl.store(request.getFiles(), suggestion);
            suggestion.setUpdatedAt(LocalDateTime.now());
            suggestion.setFiles(medias);
            suggestion = repository.save(suggestion);
        }
        if (request.getAudios() != null && request.getAudios().length != 0) {
            List<ClaimAudio> audios = claimAudioServiceImpl.store(request.getAudios(),suggestion);
            suggestion.setUpdatedAt(LocalDateTime.now());
            suggestion = repository.save(suggestion);
            
            // for (ClaimAudio audio : audios) {
            // audio.setClaim(null);
            // }
            // claim.setAudios(audios);
        }

        return suggestion;
    }

    @Override
    public Suggestion tempSaveSuggestion(SuggestionAddRequest request) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'tempSaveSuggestion'");
    }

    @Override
    public Suggestion treatSuggestion(Suggestion suggestion, User treator, TreatSuggestionRequest request)
            throws Exception {

        suggestion.setAccepted(request.isAccepted());
        suggestion.setCommentaire(request.getCommentaire());
        suggestion.setUpdatedAt(LocalDateTime.now());
        suggestion.setStatus(ClaimStatus.TREAT);
        suggestion.setTraiteur(treator);
        suggestion.setTreatAt(LocalDateTime.now());

        suggestion = repository.save(suggestion);

        return suggestion;
    }

    @Override
    public List<Suggestion> getAllByStatusNot(ClaimStatus status) {
        return repository.findByStatusNot(status);
    }

    /**
     * Generat an unique code to each claim
     * 
     * @param servicePointIndexeCode
     * @param collectorCode
     * @return
     */
    private String generateCode(String servicePointIndexeCode, String collectorCode) {
        String code = "sug" + UUID.randomUUID().toString().substring(0, 5) + "-" + servicePointIndexeCode + "-"
                + collectorCode;

        while (repository.findByCode(code).isPresent()) {
            code = "sug" + UUID.randomUUID().toString().substring(0, 5) + "-" + servicePointIndexeCode + "-"
                    + collectorCode;
        }

        return code;
    }

    @Override
    public List<Suggestion> getAllByStatusIn(List<ClaimStatus> status) {
        return repository.findByStatusIn(status);
    }

    @Override
    public List<Suggestion> getAllByCollectorAndStatus(User collector, ClaimStatus status) {
        // return repository.findByCollecteurAndStatus(collector, status);
        return repository.findByCollecteurAndStatus(collector, status);
    }

    @Override
    public void saveSuggestionOffline(SuggestionAddRequest request, ClaimStatus status) throws Exception {
        SuggestionRequest suggestionRequest = request.getSuggestionRequest();
        User collector;
     
        Suggestion suggestion = Suggestion
                .builder()
                .build();
        try {
            collector = authServiceImpl.getById(suggestionRequest.getCollectorId());
        } catch (Exception e) {
            throw new Exception("Collector of the claim not found");
        }

        if (suggestionRequest.getId() != null) {
            suggestion.setId(suggestionRequest.getId());
            Suggestion oldSuggestion = repository.findById(suggestionRequest.getId())
                    .orElseThrow(() -> new Exception("Aucune réclamation ne porte ce code"));
            suggestion = oldSuggestion;
        } else {
            if (suggestionRequest.getCode() != null && !suggestionRequest.getCode().isEmpty()) {
                suggestion.setCode(suggestionRequest.getCode());
            } else {
                String code = generateCode(collector.getServicePoint().getUuid(), collector.getCode());
                suggestion.setCode(code);
                String codeClient = "SUG-" + UUID.randomUUID().toString().substring(0, 4);
                suggestion.setCodeClient(codeClient);
            }
        }

        // if (suggestionRequest.getCode() != null && !suggestionRequest.getCode().isEmpty()) {
        //     Suggestion oldSuggestion = repository.findByCode(suggestionRequest.getCode())
        //             .orElseThrow(() -> new Exception("Aucune réclamation ne porte ce code"));
        //     suggestion = oldSuggestion;
        // } else {
        //     String code = generateCode(collector.getServicePoint().getUuid(), collector.getCode());
        //     suggestion.setCode(code);
        // }
        CollectionChannel collectionChannel;
        if (suggestionRequest.getCollectionChannelId() != null) {
            try {
                collectionChannel = collectionChannelService.getById(suggestionRequest.getCollectionChannelId());
                suggestion.setCanal(collectionChannel);
            } catch (Exception e) {
                throw new Exception("Collection channelle choosed not found");
            }
        }

        ServicePoint servicePoint;
        if (suggestionRequest.getServicePointId() != null) {
            try {
                servicePoint = servicePointServiceImpl.getById(suggestionRequest.getServicePointId());
                suggestion.setServiceIndexe(servicePoint);
            } catch (Exception e) {
                throw new Exception("Service Point choosed not found");
            }
        }

        Product product;
        if (suggestionRequest.getProductId() != null) {
            try {
                product = productServiceImpl.getById(suggestionRequest.getProductId());
                suggestion.setProduit(product);
            } catch (Exception e) {
                throw new Exception("Product choosed not found");
            }
        }

        Language language;
        if (suggestionRequest.getLanguageId() != null) {
            try {
                language = languageServiceImpl.getById(suggestionRequest.getLanguageId());
                suggestion.setLangue(language);
            } catch (Exception e) {
                throw new Exception("Objet choosed not found");
            }
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

        if (suggestionRequest.getCrew() != null) {
            suggestion.setCrew(suggestionRequest.getCrew());
        }

        if (suggestionRequest.getFolderCode() != null) {
            suggestion.setFolderCode(suggestionRequest.getFolderCode());
        }

        if (suggestionRequest.getContent() != null) {
            suggestion.setContent(suggestionRequest.getContent());
        }

        suggestion.setCollecteur(collector);

        suggestion.setStatus(status);
        if(suggestionRequest.getCreatedAt() != null && !suggestionRequest.getCreatedAt().isEmpty()){
            suggestion.setCreatedAt(Utils.convertStrWithTToLocalDateTime(suggestionRequest.getCreatedAt()));
        } else {
            suggestion.setCreatedAt(LocalDateTime.now());
        }
        
        if (suggestionRequest.getReceiptDateTime() != null && !suggestionRequest.getReceiptDateTime().isEmpty()) {
            suggestion.setReceiptDateTime(Utils.convertStrToLocalDateTime(suggestionRequest.getReceiptDateTime()));
        }
        suggestion.setOnlineUploadDateTime(suggestionRequest.getOnlineUploadDateTime());
        suggestion = repository.save(suggestion);

        if (request.getFiles() != null && request.getFiles().length != 0) {
            // System.out.println("test");
            // System.out.println(claim.getCode());
            List<Media> medias = mediaServiceImpl.store(request.getFiles(), suggestion);
            suggestion.setUpdatedAt(LocalDateTime.now());
            suggestion.setFiles(medias);
            suggestion = repository.save(suggestion);
        }
    }

}
