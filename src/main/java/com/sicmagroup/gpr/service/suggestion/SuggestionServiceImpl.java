package com.sicmagroup.gpr.service.suggestion;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.servlet.http.HttpServletRequest;
import com.fasterxml.jackson.core.StreamReadConstraints.Builder;
import com.sicmagroup.gpr.api.suggestion.SuggestionAddRequest;
import com.sicmagroup.gpr.api.suggestion.SuggestionRequest;
import com.sicmagroup.gpr.api.suggestion.TreatSuggestionRequest;
import com.sicmagroup.gpr.domain.enumeration.ClaimStatus;
import com.sicmagroup.gpr.domain.enumeration.Gender;
import com.sicmagroup.gpr.domain.enumeration.LogTarget;
import com.sicmagroup.gpr.domain.enumeration.LogType;
import com.sicmagroup.gpr.domain.enumeration.Role;
import com.sicmagroup.gpr.domain.model.Claim;
import com.sicmagroup.gpr.domain.model.ClaimAudio;
import com.sicmagroup.gpr.domain.model.CollectionChannel;
import com.sicmagroup.gpr.domain.model.InboxMessage;
import com.sicmagroup.gpr.domain.model.Language;
import com.sicmagroup.gpr.domain.model.Log;
import com.sicmagroup.gpr.domain.model.Media;
import com.sicmagroup.gpr.domain.model.Product;
import com.sicmagroup.gpr.domain.model.ServicePoint;
import com.sicmagroup.gpr.domain.model.Suggestion;
import com.sicmagroup.gpr.domain.model.User;
import com.sicmagroup.gpr.repository.InboxMessageRepository;
import com.sicmagroup.gpr.service.log.LogServiceImpl;
import com.sicmagroup.gpr.repository.InboxRepository;
import com.sicmagroup.gpr.repository.MediaRepository;
import com.sicmagroup.gpr.repository.ClaimAudioRepository;
import com.sicmagroup.gpr.repository.ExtraContentRepository;
import com.sicmagroup.gpr.repository.SuggestionRepository;
import com.sicmagroup.gpr.service.MailService;
import com.sicmagroup.gpr.service.auth.AuthenticationServiceImpl;
import com.sicmagroup.gpr.service.claimAudio.ClaimAudioServiceImpl;
import com.sicmagroup.gpr.service.collectionChannel.CollectionChannelServiceImpl;
import com.sicmagroup.gpr.service.language.LanguageServiceImpl;
import com.sicmagroup.gpr.service.media.MediaServiceImpl;
import com.sicmagroup.gpr.service.objet.ObjetServcieImpl;
import com.sicmagroup.gpr.service.product.ProductServiceImpl;
import com.sicmagroup.gpr.service.servicePoint.ServicePointServiceImpl;
import com.sicmagroup.gpr.service.setting.SettingServiceImpl;
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
    private final SettingServiceImpl settingServiceImpl;
    private final LogServiceImpl logServiceImpl;

    private final LanguageServiceImpl languageServiceImpl;
    private final AuthenticationServiceImpl authServiceImpl;
    private final MediaServiceImpl mediaServiceImpl;
    private final InboxMessageRepository messageRepository;
    private final InboxRepository inboxRepository;
    private final MediaRepository mediaRepository;
    private final ClaimAudioRepository claimAudioRepository;
    private final ExtraContentRepository extraContentRepository;
    private final MailService mailService;
    private HttpServletRequest httpServletRequest; 


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
        Log log = Log
                .builder().build();
        String libelleLog = "";

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
                    .orElseThrow(() -> new Exception("Aucune suggestion ne porte ce code"));
            suggestion = oldSuggestion;
               
            
             if (oldSuggestion.getCodeClient() == null || oldSuggestion.getCodeClient() == "") {
                String codeClient = "SUG-" + UUID.randomUUID().toString().substring(0, 4);
                suggestion.setCodeClient(codeClient);
            }
            else {
                suggestion.setCodeClient(oldSuggestion.getCodeClient()); 
                // claim.setCodeClient(claimToSave.getCodeClient());
            }
        } else {
            if (suggestionRequest.getCode() != null && !suggestionRequest.getCode().isEmpty()) {
                suggestion.setCode(suggestionRequest.getCode());
                suggestion.setCodeClient(suggestionRequest.getCodeClient());

            } else {
                String code = generateCode(collector.getServicePoint().getUuid(), collector.getCode());
                suggestion.setCode(code);
                String codeClient = "SUG-" + UUID.randomUUID().toString().substring(0, 4);
                suggestion.setCodeClient(codeClient);
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
        if (suggestionRequest.getEmail() != null) {
            suggestion.setEmail(suggestionRequest.getEmail());
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
        }

        //Whatsapp
        if(suggestionRequest.getFromWhatsapp()){
            System.out.println("From Whatsapp");
            Boolean isOk = mediaServiceImpl.attachFileToClaim(suggestion, suggestionRequest.getFilesWhatsapp());
            if(isOk && suggestionRequest.getInboxWhatsapp() != null){
                List<InboxMessage> messages = messageRepository.findByInbox(suggestionRequest.getInboxWhatsapp());
                for (InboxMessage message : messages) {
                    messageRepository.delete(message);
                }
                inboxRepository.delete(suggestionRequest.getInboxWhatsapp());
            }
        }

        List<User> pilotes = authServiceImpl.getUsersByRoles(Arrays.asList(Role.PILOTE));
       
        String message = """
        <html>
        <body style="font-family: Arial, sans-serif; background-color: #f7f7f7; padding: 20px;">
            <div style="max-width: 600px; margin: auto; background: white; border-radius: 8px; 
                        box-shadow: 0 2px 8px rgba(0,0,0,0.1); padding: 20px;">

            <h2 style="color: #004080; text-align: center;">Nouvelle suggestion enregistrée - GPR</h2>

            <p>Bonjour %s,Pilote de la plateforme <strong>GPR</strong></p>

            <p>
                Une nouvelle suggestion a été enregistrée avec succès dans votre système.
            </p>

            <div style="margin-top: 20px; background-color: #f0f8ff; border-left: 4px solid #004080; 
                        padding: 10px 15px;">
                <p style="margin: 0;"><strong>Détails de la suggestion :</strong></p>
                <p style="margin: 5px 0;">📌 <strong>Code de suggestion :</strong> %s</p>
                <p style="margin: 5px 0;">📅 <strong>Date de réception :</strong> %s</p>
            </div>

            <p style="margin-top: 20px;">
                Nous vous encourageons à examiner cette suggestion dès que possible et à prendre les mesures nécessaires pour la traiter. 
                Votre expertise est essentielle pour assurer une résolution rapide et satisfaisante pour les clients.
            </p>

            <p style="margin-top: 30px;">Cordialement,<br>L’équipe GPR</p>

            <p style="font-size: 12px; color: gray; text-align: center; margin-top: 30px;">
                Cet email a été généré automatiquement. Merci de ne pas y répondre.
            </p>

            </div>
        </body>
        </html>
        """.formatted(pilotes.get(0).getFirstandlastname(),suggestion.getCodeClient(),Utils.convertLocalDateTimeToStr(suggestion.getReceiptDateTime()));

        // Envoi de mail en parallèle
        
            try {
                mailService.sendMail(pilotes.get(0).getEmail(), "Nouvelle suggestion enregistrée - GPR", message, null);
                
                Log successLog = Log.builder()
                    .libelle("Mail notification d'enregistrement de suggestion")
                    .content("Success mail notification suggestion enregistrée")
                    .createdAt(LocalDateTime.now())
                    .type(LogType.INFO)
                    .userId(suggestion.getCollecteur().getId())
                    .userIpAddress(Utils.getClientIpAddress(httpServletRequest))
                    .target(LogTarget.SUGGESTION)
                    .build();

                logServiceImpl.saveLog(successLog);
            } catch (Exception e) {
                if (e != null) {
                    Log log2 = Log
                            .builder()
                            .libelle("Echec mail notification suggestion enregistrée")
                            .content(e.getMessage())
                            .createdAt(LocalDateTime.now())
                            .type(LogType.ERROR)
                            .userId(suggestion.getCollecteur().getId())
                            .userIpAddress(Utils.getClientIpAddress(httpServletRequest))
                            .target(LogTarget.SUGGESTION)
                            .build();

                    logServiceImpl.saveLog(log2);
                }
            }
       

        try {
            Utils.sendSms(pilotes.get(0).getTel(),
                    "Nouvelle suggestion enregistrée sur la plateforme GPR.", settingServiceImpl);
        } catch (Exception e) {
            Log log2 = Log
                    .builder()
                    .libelle("Echec sms notification")
                    .content(e.getMessage())
                    .createdAt(LocalDateTime.now())
                    .type(LogType.ERROR)
                    .userId(0L)
                    .userIpAddress(null)
                    .target(LogTarget.APP)
                    .build();

            logServiceImpl.saveLog(log2);
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
                    .orElseThrow(() -> new Exception("Aucune suggestion ne porte ce code"));
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
    public Suggestion tempSaveSuggestion(SuggestionAddRequest request,ClaimStatus status) throws Exception {
        SuggestionRequest suggestionRequest = request.getSuggestionRequest();
        Log log = Log
                .builder().build();
        Suggestion suggestion = Suggestion
                .builder().build();
        User collector;
       try {
            collector = authServiceImpl.getById(suggestionRequest.getCollectorId());
        } catch (Exception e) {
            throw new Exception("Collector of the claim not found");
        }

        String libelleLog = "";
        LogTarget targetLog = null;
       
            targetLog = LogTarget.SUGGESTION;
        
        log.setTarget(targetLog);

        if (suggestionRequest.getId() != null) {
            Suggestion oldSuggestion = repository.findById(suggestionRequest.getId())
                    .orElseThrow(() -> new Exception("Aucune suggestion ne porte ce code"));
            suggestion = oldSuggestion;
            
            libelleLog = "Modification d'une suggestion Temporairement sauvegardée";
           

        } else {
            if (suggestionRequest.getCode() != null && !suggestionRequest.getCode().isEmpty()) {
                suggestion.setCode(suggestionRequest.getCode());
               
                    libelleLog = "Suggestion Temporairement sauvegardée - offline mis en ligne";
               
           
            } else {
                String code = generateCode(collector.getServicePoint().getUuid(), collector.getCode());
                suggestion.setCode(code);
            }

        }

        log.setLibelle(libelleLog);

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
        if (suggestionRequest.getEmail() != null) {
            suggestion.setEmail(suggestionRequest.getEmail());
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
        }

        //Whatsapp
        if(suggestionRequest.getFromWhatsapp()){
            System.out.println("From Whatsapp");
            Boolean isOk = mediaServiceImpl.attachFileToClaim(suggestion, suggestionRequest.getFilesWhatsapp());
            if(isOk && suggestionRequest.getInboxWhatsapp() != null){
                List<InboxMessage> messages = messageRepository.findByInbox(suggestionRequest.getInboxWhatsapp());
                for (InboxMessage message : messages) {
                    messageRepository.delete(message);
                }
                inboxRepository.delete(suggestionRequest.getInboxWhatsapp());
            }
        }


        return suggestion ;
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

        final Suggestion finalSuggestion = suggestion;
        List<User> pilotes = authServiceImpl.getUsersByRoles(Arrays.asList(Role.PILOTE));

        String priseEnCompte = suggestion.isAccepted() ? "Prise en compte" : "Non prise en compte";

        String messageHtml = """
        <html>
        <body style="font-family: Arial, sans-serif; background-color: #f7f7f7; padding: 20px;">
            <div style="max-width: 600px; margin: auto; background: white; border-radius: 8px;
                        box-shadow: 0 2px 8px rgba(0,0,0,0.1); padding: 20px;">

            <h2 style="color: #004080; text-align: center;">Traitement de suggestion - GPR</h2>

            <p>Bonjour <strong>%s</strong>, Pilote de la plateforme <strong>GPR</strong>,</p>

            <p>
                La suggestion portant le code <strong>%s</strong> a été analysée et marquée comme traitée par %s.
            </p>

            <div style="margin-top: 20px; background-color: #f0f8ff; border-left: 4px solid #004080;
                        padding: 10px 15px;">
                <p style="margin: 0;"><strong>Détails de la suggestion :</strong></p>
                <p style="margin: 5px 0;">📌 <strong>Code :</strong> %s</p>
                <p style="margin: 5px 0;">📅 <strong>Date de réception :</strong> %s</p>
                <p style="margin: 5px 0;">📌 <strong>Décision :</strong> %s</p>
            </div>

            <p style="margin-top: 30px;">Cordialement,<br>L’équipe GPR</p>

            <p style="font-size: 12px; color: gray; text-align: center; margin-top: 30px;">
                Cet email a été généré automatiquement. Merci de ne pas y répondre.
            </p>

            </div>
        </body>
        </html>
        """.formatted(treator.getFirstandlastname(),finalSuggestion.getCodeClient(),pilotes.get(0).getFirstandlastname(),finalSuggestion.getCodeClient(),Utils.convertLocalDateTimeToStr(finalSuggestion.getReceiptDateTime()),priseEnCompte);

        // Envoi de mail en parallèle
        
            try {
                mailService.sendMail(pilotes.get(0).getEmail(), "Traitement de suggestion - GPR", messageHtml, null);
                                                
                Log successLog = Log.builder()
                    .libelle("Mail notification suggestion traité")
                    .content("Success mail notification suggestion traité")
                    .createdAt(LocalDateTime.now())
                    .type(LogType.INFO)
                    .userId(suggestion.getCollecteur().getId())
                    .userIpAddress(Utils.getClientIpAddress(httpServletRequest))
                    .target(LogTarget.SUGGESTION)
                    .build();

                logServiceImpl.saveLog(successLog);                           
            } catch (Exception e) {                        
                if (e != null) {
                    Log log2 = Log
                            .builder()
                            .libelle("Echec mail notification suggestion traité")
                            .content(e.getMessage())
                            .createdAt(LocalDateTime.now())
                            .type(LogType.ERROR)
                            .userId(suggestion.getCollecteur().getId())
                            .userIpAddress(Utils.getClientIpAddress(httpServletRequest))
                            .target(LogTarget.SUGGESTION)
                            .build();

                    logServiceImpl.saveLog(log2);
                }
            }
       

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
                    .orElseThrow(() -> new Exception("Aucune suggestion ne porte ce code"));
            suggestion = oldSuggestion;
            suggestion.setCodeClient(oldSuggestion.getCodeClient());
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
        if (suggestionRequest.getEmail() != null) {
            suggestion.setEmail(suggestionRequest.getEmail());
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

    @Override
    public Suggestion getByCode(String code) throws Exception {
        return repository.findByCode(code).orElseThrow(() -> new Exception("Suggestion introuvable"));
    }
        
    @Transactional
    @Override
    public void deleteById(Long id) throws NotFoundException {
        Suggestion suggestion = repository.findById(id).orElseThrow(() -> new NotFoundException());

        // Supprimer les éléments liés à la suggestion
        mediaRepository.deleteFromJoinTableBySuggestionId(id);
        mediaRepository.deleteBySuggestionId(id);
        claimAudioRepository.deleteBySuggestionId(id);
        extraContentRepository.deleteBySuggestionId(id);

        // Enfin, supprimer la suggestion elle-même
        repository.delete(suggestion);
    }
}
