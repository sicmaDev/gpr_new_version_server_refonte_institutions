package com.sicmagroup.gpr.service.claim;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.concurrent.CompletableFuture;

import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.sicmagroup.gpr.api.claim.AffectTreatmentRequest;
import com.sicmagroup.gpr.api.claim.ClaimRequest;
import com.sicmagroup.gpr.api.claim.ProposedSolutionRequest;
import com.sicmagroup.gpr.api.claim.SaveRequest;
import com.sicmagroup.gpr.api.denunciation.DenunRequest;
import com.sicmagroup.gpr.api.denunciation.SaveDenunRequest;
import com.sicmagroup.gpr.domain.dto.AlertDto;
import com.sicmagroup.gpr.domain.enumeration.ClaimStatus;
import com.sicmagroup.gpr.domain.enumeration.ClaimType;
import com.sicmagroup.gpr.domain.enumeration.Gender;
import com.sicmagroup.gpr.domain.enumeration.GravityLevel;
import com.sicmagroup.gpr.domain.enumeration.LogTarget;
import com.sicmagroup.gpr.domain.enumeration.LogType;
import com.sicmagroup.gpr.domain.enumeration.Role;
import com.sicmagroup.gpr.domain.enumeration.SatisfactionStatus;
import com.sicmagroup.gpr.domain.enumeration.SolutionStatus;
import com.sicmagroup.gpr.domain.model.Claim;
import com.sicmagroup.gpr.domain.model.ClaimAudio;
import com.sicmagroup.gpr.domain.model.CollectionChannel;
import com.sicmagroup.gpr.domain.model.ExistingSolution;
import com.sicmagroup.gpr.domain.model.ExternalRecourse;
import com.sicmagroup.gpr.domain.model.ExtraContent;
import com.sicmagroup.gpr.domain.model.Inbox;
import com.sicmagroup.gpr.domain.model.InboxMessage;
import com.sicmagroup.gpr.domain.model.Language;
import com.sicmagroup.gpr.domain.model.Log;
import com.sicmagroup.gpr.domain.model.Media;
import com.sicmagroup.gpr.domain.model.Objet;
import com.sicmagroup.gpr.domain.model.Product;
import com.sicmagroup.gpr.domain.model.SatisfactionMeasure;
import com.sicmagroup.gpr.domain.model.ServicePoint;
import com.sicmagroup.gpr.domain.model.Solution;
import com.sicmagroup.gpr.domain.model.Suggestion;
import com.sicmagroup.gpr.domain.model.User;
import com.sicmagroup.gpr.domain.model.chat.Chat;
import com.sicmagroup.gpr.repository.ClaimAudioRepository;
import com.sicmagroup.gpr.repository.ExtraContentRepository;
import com.sicmagroup.gpr.repository.ClaimRepository;
import com.sicmagroup.gpr.repository.ExistingSolutionRepository;
import com.sicmagroup.gpr.repository.ExternalRecourseRepository;
import com.sicmagroup.gpr.repository.InboxMessageRepository;
import com.sicmagroup.gpr.repository.InboxRepository;
import com.sicmagroup.gpr.repository.MediaRepository;
import com.sicmagroup.gpr.repository.ServicePointRepository;
import com.sicmagroup.gpr.repository.chat.ChatRepository;
import com.sicmagroup.gpr.service.auth.AuthenticationServiceImpl;
import com.sicmagroup.gpr.service.claimAudio.ClaimAudioServiceImpl;
import com.sicmagroup.gpr.service.collectionChannel.CollectionChannelServiceImpl;
import com.sicmagroup.gpr.service.existingSolution.ExistingSolutionServiceImpl;
import com.sicmagroup.gpr.service.externalRecourse.ExternalRecourseServiceImpl;
import com.sicmagroup.gpr.service.extra.ExtraContentServiceImpl;
import com.sicmagroup.gpr.service.historiqueAffectation.HistoriqueAffectationServiceImpl;
import com.sicmagroup.gpr.service.language.LanguageServiceImpl;
import com.sicmagroup.gpr.service.log.LogServiceImpl;
import com.sicmagroup.gpr.service.media.MediaServiceImpl;
import com.sicmagroup.gpr.service.objet.ObjetServcieImpl;
import com.sicmagroup.gpr.service.product.ProductServiceImpl;
import com.sicmagroup.gpr.service.satisfactionMeasure.SatifactionMeasureServiceImpl;
import com.sicmagroup.gpr.service.servicePoint.ServicePointServiceImpl;
import com.sicmagroup.gpr.service.setting.SettingServiceImpl;
import com.sicmagroup.gpr.service.solution.SolutionServiceImpl;
import com.sicmagroup.gpr.utils.CurrentUserUtils;
import com.sicmagroup.gpr.utils.Utils;
import com.sicmagroup.gpr.utils.CurrentUserUtils;
import com.sicmagroup.gpr.repository.ServicePointRepository;
import com.sicmagroup.gpr.repository.SuggestionRepository;

import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ClaimServiceImpl implements ClaimService {
    private final ClaimRepository repository;
    private final CollectionChannelServiceImpl collectionChannelService;
    private final ServicePointServiceImpl servicePointServiceImpl;
    private final ProductServiceImpl productServiceImpl;
    private final ObjetServcieImpl objetServcieImpl;
    private final LanguageServiceImpl languageServiceImpl;
    private final AuthenticationServiceImpl authServiceImpl;
    private final MediaServiceImpl mediaServiceImpl;
    private final SatifactionMeasureServiceImpl satifactionMeasureServiceImpl;
    private final SolutionServiceImpl solutionServiceImpl;
    private final ExternalRecourseServiceImpl externalRecourseServiceImpl;
    private final ClaimAudioServiceImpl claimAudioServiceImpl;
    private final LogServiceImpl logServiceImpl;
    private final ExistingSolutionRepository existingSolutionRepository;
    private final ChatRepository chatRepository;
    private final SettingServiceImpl settingServiceImpl;
    private final ServicePointRepository spRepository;
    private final InboxRepository inboxRepository;
    private final InboxMessageRepository messageRepository;
    private final MediaRepository mediaRepository;
    private final ClaimAudioRepository claimAudioRepository;
    private final ExtraContentRepository extraContentRepository;
    private final ExtraContentServiceImpl extraContentServiceImpl;
    private final ExternalRecourseRepository externalRecourseRepository;
    private final HistoriqueAffectationServiceImpl historiqueAffectationServiceImpl;
    private final CurrentUserUtils userAuth;
    private final SuggestionRepository suggestionRepository;

    @Override
    public List<Claim> getAll(ClaimType type) {
        return repository.findByType(type);
    }

    @Override
    public Claim getById(Long id) throws NotFoundException {
        return repository.findById(id).orElseThrow(() -> new NotFoundException());
    }

    // public List<Claim> getUserClaim(String userCode) {
    //     return repository.findByCodeStartsWith(userCode);
    // }

    @Override
    public Claim saveClaim(SaveRequest claimPart, ClaimType type) throws Exception {
        ClaimRequest claimToSave = claimPart.getClaimRequest();
        User collector;
        try {
            collector = authServiceImpl.getById(claimToSave.getCollectorId());
        } catch (Exception e) {
            throw new Exception("Collector " + claimToSave.getCollectorId() + " of the claim not found");
        }
        // if(claimToSave.getCode() != null && claimToSave.getId() != null){
        // Claim oldClaim = repository.findByCode(claimToSave.getCode()).orElseThrow(()
        // -> new ClaimException("Claim with this code not exist"));
        // } else {

        // }

        

        Claim claim = Claim
                .builder()
                .clientFirstAndLastName(claimToSave.getClientFirstAndLastName())
                .gender(Gender.valueOf(claimToSave.getGender()))
                .type(type)
                .address(claimToSave.getAddress())
                .tel(claimToSave.getPhone())
                .crew(claimToSave.getCrew())
                .folderCode(claimToSave.getFolderCode())
                .content(claimToSave.getContent())
                .collector(collector).status(ClaimStatus.SAVED)
                .createdAt(LocalDateTime.now())
                .receiptDateTime(Utils.convertStrToLocalDateTime(claimToSave.getReceiptDateTime()))
                .build();

        if (claimToSave.getStatus() != null) {
            claim.setStatus(claimToSave.getStatus());
        } else {
            claim.setStatus(ClaimStatus.SAVED);
            claim.setCodeClient(claimToSave.getCodeClient());
        }

        if (claimToSave.getId() != null) {
            claim.setId(claimToSave.getId());
            claim.setCode(claimToSave.getCode());

           
           
            // Only TEMP_SAVED can be saved
            Claim oldClaim = repository.findById(claimToSave.getId())
                    .orElseThrow(() -> new ClaimException("Claim with this code doesn't exist"));

            // claim.setCodeClient(oldClaim.getCodeClient());
            // if (oldClaim.getStatus() != ClaimStatus.TEMP_SAVED) {
            // throw new ClaimException(
            // "Invalid operation! this claim is not temporarly saved, you can't change it
            // again");
            // }
            if (oldClaim.getCodeClient() == null || oldClaim.getCodeClient() == "") {
                String codeClient = "REC-" + UUID.randomUUID().toString().substring(0, 4);
                claim.setCodeClient(codeClient);
            }
            else {
                claim.setCodeClient(oldClaim.getCodeClient());
                // claim.setCodeClient(claimToSave.getCodeClient());
            }

        } else {
            if (claimToSave.getCode() == null || claimToSave.getCode() == "") {
                String code = generateCode(collector.getServicePoint().getUuid(), collector.getCode(), type);
                claim.setCode(code);
                String codeClient = "REC-" + UUID.randomUUID().toString().substring(0, 4);
                claim.setCodeClient(codeClient);
            } else {
                claim.setCode(claimToSave.getCode());
                claim.setCodeClient(claimToSave.getCodeClient());
            }
        }
        // if(claimToSave.getCode() == null || claimToSave.getCode()== "") {
        // String code = generateCode(collector.getServicePoint().getUuid(),
        // collector.getCode(), type);
        // claim.setCode(code);
        // }
        CollectionChannel collectionChannel;
        if (claimToSave.getCollectionChannelId() != null) {
            try {
                collectionChannel = collectionChannelService.getById(claimToSave.getCollectionChannelId());
                claim.setCollectionChannel(collectionChannel);
            } catch (Exception e) {
                throw new Exception("Canal de collecte introuvable");
            }
        }
        ServicePoint servicePoint;
        if (claimToSave.getServicePointId() != null) {
            try {
                servicePoint = servicePointServiceImpl.getById(claimToSave.getServicePointId());
                claim.setServicePoint(servicePoint);
            } catch (Exception e) {
                throw new Exception("Point Service introuvable");
            }
        }

        Product product;
        if (claimToSave.getProductId() != null) {
            try {
                product = productServiceImpl.getById(claimToSave.getProductId());
                claim.setProduct(product);
            } catch (Exception e) {
                throw new Exception("Product introuvable");
            }
        }

        Objet objet;
        if (claimToSave.getObjetId() != null) {
            try {
                objet = objetServcieImpl.getById(claimToSave.getObjetId());
                claim.setObjet(objet);
            } catch (Exception e) {
                throw new Exception("Objet introuvable");
            }
        }

        Language language;
        if (claimToSave.getLanguageId() != null) {
            try {
                language = languageServiceImpl.getById(claimToSave.getLanguageId());
                claim.setLanguage(language);
            } catch (Exception e) {
                throw new Exception("Langage introuvable");
            }
        }


        claim = repository.save(claim);
        Log log = Log
                .builder()
                .content("code: " + claim.getCode())
                .createdAt(LocalDateTime.now())
                .type(LogType.INFO)
                .userId(claim.getCollector().getId())
                .userIpAddress(claimPart.getRemoteAddress())
                .build();
        if (type.equals(ClaimType.CLAIM)) {
            log.setLibelle("Nouvelle réclamation");
            log.setTarget(LogTarget.CLAIM);
        } else {
            log.setLibelle("Nouvelle dénonciation");
            log.setTarget(LogTarget.DENUNCIACION);
        }

        logServiceImpl.saveLog(log);

        if (claimPart.getFiles() != null && claimPart.getFiles().length != 0) {
            List<Media> medias = mediaServiceImpl.store(claimPart.getFiles(), claim);
            claim.setUpdatedAt(LocalDateTime.now());
            // claim.setMedias(medias);
        }
        if (claimPart.getAudios() != null && claimPart.getAudios().length != 0) {
            List<ClaimAudio> audios = claimAudioServiceImpl.store(claimPart.getAudios(), claim);
            claim.setUpdatedAt(LocalDateTime.now());
            // for (ClaimAudio audio : audios) {
            // audio.setClaim(null);
            // }
            // claim.setAudios(audios);
        }

        //Whatsapp
        if(claimToSave.getFromWhatsapp()){
            System.out.println("From Whatsapp");
            Boolean isOk = mediaServiceImpl.attachFileToClaim(claim, claimToSave.getFilesWhatsapp());
            if(isOk && claimToSave.getInboxWhatsapp() != null){
                List<InboxMessage> messages = messageRepository.findByInbox(claimToSave.getInboxWhatsapp());
                for (InboxMessage message : messages) {
                    messageRepository.delete(message);
                }
                inboxRepository.delete(claimToSave.getInboxWhatsapp());
            }
        }

        claim = repository.save(claim);
       
        List<User> usersToContact = authServiceImpl.getEmailReceiversForNotif(claim.getServicePoint());

        Double apercuContent = claim.getContent().length() * 0.5;
        String message = "" +
                "Cher(e) utilisateur, "+
                "une nouvelle réclamation a été enregistrée avec succès dans votre système. Vous recevez ce mail en tant qu'utilisateur habilité à recevoir une notification lors d'enregistrement de nouvelles réclamations."
                + "\n\n" +
                "Détails de la réclamation :" + "\n\n" +
                "* Code de réclamation : " + claim.getCodeClient() + "\n" +
                "* Date d'enregistrement : " + Utils.convertLocalDateTimeToStr(claim.getReceiptDateTime()) + "\n" +
                "* Aperçu du contenu : " + claim.getContent().substring(0, apercuContent.intValue()) + "...\n\n" +
                "Nous vous encourageons à examiner cette réclamation dès que possible et à prendre les mesures nécessaires pour la traiter. Votre expertise et vos compétences sont essentielles pour assurer une résolution rapide et satisfaisante pour les clients.";

        // Envoi de mail en parallèle
        CompletableFuture.runAsync(() -> {
            try {
                Utils.sendmail(usersToContact, " Notification d'enregistrement de réclamation", message, null,
                        " ", settingServiceImpl);
                
                Log successLog = Log.builder()
                    .libelle("Mail notification d'enregistrement de réclamation")
                    .content("Success mail notification réclamation affectée")
                    .createdAt(LocalDateTime.now())
                    .type(LogType.INFO)
                    .userId(0L)
                    .userIpAddress(claimPart.getRemoteAddress())
                    .target(LogTarget.APP)
                    .build();

                logServiceImpl.saveLog(successLog);
            } catch (Exception e) {
                if (e != null) {
                    Log log2 = Log
                            .builder()
                            .libelle("Echec mail notification réclamation créée")
                            .content(e.getMessage())
                            .createdAt(LocalDateTime.now())
                            .type(LogType.ERROR)
                            .userId(0L)
                            .userIpAddress(claimPart.getRemoteAddress())
                            .target(LogTarget.APP)
                            .build();

                    logServiceImpl.saveLog(log2);
                }
            }
        });

        // try {
        //     Utils.sendSms(usersToContact,
        //             "Nouvelle réclamation enregistrée de niveau de gravité "
        //                     + claim.getObjet().getRisqueLevel().name(), settingServiceImpl);
        // } catch (Exception e) {
        //     Log log2 = Log
        //             .builder()
        //             .libelle("Echec sms notification")
        //             .content(e.getMessage())
        //             .createdAt(LocalDateTime.now())
        //             .type(LogType.ERROR)
        //             .userId(0L)
        //             .userIpAddress(claimPart.getRemoteAddress())
        //             .target(LogTarget.APP)
        //             .build();

        //     logServiceImpl.saveLog(log2);
        // }

        return claim;

    }

    @Override
    public Claim saveExtra(ExtraContent extraContent, MultipartFile[] files, MultipartFile[] audios, Long id)
            throws Exception {
        Claim claim = getById(id);
        extraContent.setStatus(claim.getStatus());
        extraContent.setType(claim.getType());
        extraContent.setClaim(claim);
        extraContent.setSuggestion(null);
        extraContent.setUser(userAuth.getUser());
        extraContent.setCreatedAt(LocalDateTime.now());
        extraContent.setUpdatedAt(LocalDateTime.now());

        ExtraContent extraContentSave = extraContentServiceImpl.saveExtraContent(extraContent);
        // Ajout du contenu au Claim
        claim.getExtraContents().add(extraContentSave);

        if (extraContentSave.isFile()) { 
            if (files != null && files.length != 0) {
                List<Media> medias = mediaServiceImpl.store(files, claim, extraContentSave);
                // claim.setUpdatedAt(LocalDateTime.now());
                
                // return repository.save(claim);
            }
            if (audios != null && audios.length != 0) {
                List<ClaimAudio> audio = claimAudioServiceImpl.store(audios, claim, extraContentSave);
                // claim.setUpdatedAt(LocalDateTime.now());
                
                // return repository.save(claim);
            }
        }

        claim.setUpdatedAt(LocalDateTime.now());
        
        return repository.save(claim);
    }

    @Override
    public Claim saveTempClaim(SaveRequest claimPart, ClaimType type) throws Exception {
        ClaimRequest claimToSave = claimPart.getClaimRequest();
        Log log = Log
                .builder().build();
        Claim claim = Claim
                .builder().build();
        User collector;
        try {
            collector = authServiceImpl.getById(claimToSave.getCollectorId());
            claim.setCollector(collector);
        } catch (Exception e) {
            throw new Exception("Collector " + claimToSave.getCollectorId() + " of the temp claim not found");
        }
        String libelleLog = "";
        LogTarget targetLog = null;
        if (type.equals(ClaimType.CLAIM)) {
            targetLog = LogTarget.CLAIM;
        } else {
            targetLog = LogTarget.DENUNCIACION;
        }
        log.setTarget(targetLog);

        if (claimToSave.getId() != null) {
            Claim oldClaim = repository.findById(claimToSave.getId())
                    .orElseThrow(() -> new Exception("Aucune réclamation ne porte ce code"));
            claim = oldClaim;
            if (type.equals(ClaimType.CLAIM)) {
                libelleLog = "Modification d'une réclamation Temporairement sauvegardée";
            } else {
                libelleLog = "Modification d'une dénonciation Temporairement sauvegardée";
            }
        } else {
            if (claimToSave.getCode() != null && !claimToSave.getCode().isEmpty()) {
                claim.setCode(claimToSave.getCode());
                if (type.equals(ClaimType.CLAIM)) {
                    libelleLog = "Réclamation Temporairement sauvegardée - offline mis en ligne";
                } else {
                    libelleLog = "Dénonciation Temporairement sauvegardée- offline mis en ligne";
                }
            } else {
                String code = generateCode(collector.getServicePoint().getUuid(), collector.getCode(), type);
                claim.setCode(code);
                if (type.equals(ClaimType.CLAIM)) {
                    libelleLog = "Nouvelle réclamation Temporairement sauvegardée";
                } else {
                    libelleLog = "Nouvelle dénonciation Temporairement sauvegardée";
                }
            }
        }

        log.setLibelle(libelleLog);

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
            try {
                collectionChannel = collectionChannelService.getById(claimToSave.getCollectionChannelId());
                claim.setCollectionChannel(collectionChannel);
            } catch (Exception e) {
                throw new Exception("Collection channelle choosed not found");
            }
        }

        ServicePoint servicePoint;
        if (claimToSave.getServicePointId() != null) {
            try {
                servicePoint = servicePointServiceImpl.getById(claimToSave.getServicePointId());
                claim.setServicePoint(servicePoint);
            } catch (Exception e) {
                throw new Exception("Point Service introuvable");
            }
        }

        Product product;
        if (claimToSave.getProductId() != null) {
            try {
                product = productServiceImpl.getById(claimToSave.getProductId());
                claim.setProduct(product);
            } catch (Exception e) {
                throw new Exception("Product introuvable");
            }
        }

        Objet objet;
        if (claimToSave.getObjetId() != null) {
            try {
                objet = objetServcieImpl.getById(claimToSave.getObjetId());
                claim.setObjet(objet);
            } catch (Exception e) {
                throw new Exception("Objet introuvable");
            }
        }

        Language language;
        if (claimToSave.getLanguageId() != null) {
            try {
                language = languageServiceImpl.getById(claimToSave.getLanguageId());
                claim.setLanguage(language);
            } catch (Exception e) {
                throw new Exception("Langage introuvable");
            }
        }


        // ServicePoint servicePoint;
        // if (claimToSave.getServicePointUuid() != null) {
        //     try {
        //         servicePoint = servicePointServiceImpl.findPointDeServiceByUuid(claimToSave.getServicePointUuid());
        //         claim.setServicePoint(servicePoint);
        //     } catch (Exception e) {
        //         throw new Exception("Service Point choosed not found");
        //     }
        // }

        // Product product;
        // if (claimToSave.getProductUuid() != null) {
        //     try {
        //         product = productServiceImpl.findProductByUuid(claimToSave.getProductUuid());
        //         claim.setProduct(product);
        //     } catch (Exception e) {
        //         throw new Exception("Product choosed not found");
        //     }
        // }

        // Objet objet;
        // // System.out.println("objet id");
        // // System.out.println(claimToSave.getObjetId());
        // if (claimToSave.getObjetUuid() != null) {
        //     try {
        //         objet = objetServcieImpl.findByUuid(claimToSave.getObjetUuid());
        //         claim.setObjet(objet);
        //     } catch (Exception e) {
        //         throw new Exception("Objet choosed not found");
        //     }
        // }

        // Language language;
        // if (claimToSave.getLanguageUuid() != null) {
        //     try {
        //         language = languageServiceImpl.findByUuid(claimToSave.getLanguageUuid());
        //         claim.setLanguage(language);
        //     } catch (Exception e) {
        //         throw new Exception("Objet choosed not found");
        //     }
        // }

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

        if (claimToSave.getCrew() != null) {
            claim.setCrew(claimToSave.getCrew());
        }

        if (claimToSave.getFolderCode() != null) {
            claim.setFolderCode(claimToSave.getFolderCode());
        }

        if (claimToSave.getContent() != null) {
            claim.setContent(claimToSave.getContent());
        }

        claim.setCollector(collector);
        claim.setStatus(ClaimStatus.TEMP_SAVED);
        claim.setCreatedAt(LocalDateTime.now());
        if (claimToSave.getReceiptDateTime() != null && !claimToSave.getReceiptDateTime().isEmpty()) {
            claim.setReceiptDateTime(Utils.convertStrToLocalDateTime(claimToSave.getReceiptDateTime()));
        }
        if (claimToSave.getOnlineUploadDateTime() != null) {
            claim.setOnlineUploadDateTime(claimToSave.getOnlineUploadDateTime());
        }
        claim = repository.save(claim);

        log.setContent("code: " + claim.getCode());
        log.setCreatedAt(LocalDateTime.now());
        log.setType(LogType.INFO);
        log.setUserId(claim.getCollector().getId());
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
        claim = repository.save(claim);
        return claim;
    }

    /**
     * Generat an unique code to each claim
     * 
     * @param servicePointIndexeCode
     * @param collectorCode
     * @return
     */
    private String generateCode(String servicePointIndexeCode, String collectorCode, ClaimType type) {
        String start = "rec";
        if (type == ClaimType.DENUNCIACION) {
            start = "den";
        }
        String code = start + UUID.randomUUID().toString().substring(0, 5) + "-" + servicePointIndexeCode + "-"
                + collectorCode;
        

        while (repository.findByCode(code).isPresent()) {
            code = start + UUID.randomUUID().toString().substring(0, 5) + "-" + servicePointIndexeCode + "-"
                    + collectorCode;
        }

        return code;
    }

    @Override
    public Claim affectTreatmentToUser(Claim claim, User affectedTo, User affectedBy, Boolean anonymous,
            String remoteAddress, AffectTreatmentRequest affectTreatmentRequest)
            throws Exception {
        // Claim claim = repository.findById(claimId).orElseThrow(() -> new
        // ClaimException("Claim choosed not found"));
        // User affectedTo = authServiceImpl.getById(userId);
        // User affectedBy = authServiceImpl.getById(affectorId);

        if (!Arrays.asList(ClaimStatus.SAVED,ClaimStatus.PARTIAL_SATISFIED,ClaimStatus.UNSATISFIED,ClaimStatus.CLASSED,ClaimStatus.AFFECTED).contains(claim.getStatus())) {
            throw new ClaimException("Invalid request! You can't affect treatment to not saved claim");
        }

        claim.setTreatmentAffectedBy(affectedBy);
        claim.setTreatmentAffectedTo(affectedTo);
        claim.setAffectedAnonymous(anonymous);
        claim.setStatus(ClaimStatus.AFFECTED);
        claim.setUpdatedAt(LocalDateTime.now());
        claim.setAffectedAt(LocalDateTime.now());

        claim = repository.save(claim);

        Log log = Log
                .builder()
                .libelle("Affectation de traitement")
                .content("Code : " + claim.getCode())
                .createdAt(LocalDateTime.now())
                .type(LogType.INFO)
                .userId(affectedBy.getId())
                .userIpAddress(remoteAddress)
                .target(claim.getType().equals(ClaimType.CLAIM) ? LogTarget.CLAIM : LogTarget.DENUNCIACION)
                .build();
        logServiceImpl.saveLog(log);

        Double apercuContent = claim.getContent().length() * 0.3;
        String message = "" +
                "Cher(e) " + affectedTo.getFirstandlastname() + ",\n\n" +
                "Le traitement d'une nouvelle réclamation vous a été affecté(e). Cette réclamation nécessite votre attention et votre expertise pour garantir une résolution rapide et satisfaisante."
                + "\n\n" +
                "Détails de la réclamation :" + "\n\n" +
                "* Code de réclamation : " + claim.getCodeClient() + "\n" +
                "* Date d'enregistrement : " + Utils.convertLocalDateTimeToStr(claim.getReceiptDateTime()) + "\n" +
                "* Délai de traitement : " + affectTreatmentRequest.getDelai() + " jours \n" +
                "* Aperçu du contenu : " + claim.getContent().substring(0, apercuContent.intValue()) + "...\n\n" +
                "Veuillez prendre les mesures nécessaires pour examiner et traiter cette réclamation dans les plus brefs délais";

        if (!affectTreatmentRequest.getMessage().isEmpty()) {
            message = affectTreatmentRequest.getMessage();
        }

        historiqueAffectationServiceImpl.storeHistorique(affectTreatmentRequest);
        
        final String finalMessage = message;
        // Envoi de mail en parallèle
        CompletableFuture.runAsync(() -> {
            try {
                Utils.sendmail(affectedTo.getEmail(), "Affectation de réclamation", finalMessage, null,
                        " ", settingServiceImpl);
                
                Log successLog = Log.builder()
                    .libelle("Mail notification d'affectation de réclamation")
                    .content("Success mail notification réclamation affectée")
                    .createdAt(LocalDateTime.now())
                    .type(LogType.INFO)
                    .userId(0L)
                    .userIpAddress(remoteAddress)
                    .target(LogTarget.APP)
                    .build();

                logServiceImpl.saveLog(successLog);
            } catch (Exception e) {
                if (e != null) {
                    Log log2 = Log
                            .builder()
                            .libelle("Echec mail notification réclamation affectée")
                            .content(e.getMessage())
                            .createdAt(LocalDateTime.now())
                            .type(LogType.ERROR)
                            .userId(0L)
                            .userIpAddress(remoteAddress)
                            .target(LogTarget.APP)
                            .build();
    
                    logServiceImpl.saveLog(log2);
                }
    
            }
        });

        try {
            Utils.sendSms(Arrays.asList(affectedTo), "Une nouvelle réclamation de niveau de gravité "
                    + claim.getObjet().getRisqueLevel().name() + " vous a été affectée", settingServiceImpl);
        } catch (Exception e) {
            Log log2 = Log
                    .builder()
                    .libelle("Echec sms notification")
                    .content(e.getMessage())
                    .createdAt(LocalDateTime.now())
                    .type(LogType.ERROR)
                    .userId(0L)
                    .userIpAddress(remoteAddress)
                    .target(LogTarget.APP)
                    .build();

            logServiceImpl.saveLog(log2);
        }
        return claim;
    }

    @Override
    public Claim treatClaim(Claim claim, User treator, ProposedSolutionRequest request) throws Exception {
        Solution solution2;
        System.out.println("Request received: " + request);
        if (!Arrays.asList(ClaimStatus.SAVED, ClaimStatus.AFFECTED, ClaimStatus.TO_APPROUVED, ClaimStatus.DESAPPROUVED,
                ClaimStatus.UNSATISFIED, ClaimStatus.PARTIAL_SATISFIED, ClaimStatus.CLASSED)
                .contains(claim.getStatus())) {
            throw new ClaimException("Invalid request ! You can't treat an claim " + claim.getStatus().name() + "");
        }
        System.out.println("Here 4 ");
        String solution = "";
        String commantaire = "";
        if (request.getSolution() != null && request.getSolution() != "") {
            solution = request.getSolution();
        }
        if (request.getCommentaire() != null && request.getCommentaire() != "") {
            commantaire = request.getCommentaire();
        }
        System.out.println("Here 5 ");
        solution2 = Solution
                .builder()
                .author(treator)
                .claim(claim)
                .content(solution)
                .commentaire(commantaire)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        System.out.println("Here 5 ");
        System.out.println(request);
        if ((request.getSolution() == null || request.getSolution() == "") 
                && request.getExistingId() != null) {
                     System.out.println("Here 6 in it ");
            // TODO cas d'une solution existante
            ExistingSolution existingSolution = existingSolutionRepository.findById(request.getExistingId())
                    .orElseThrow(() -> new Exception("Solution choisie introuvable"));
                   
            solution2.setExistingSolution(existingSolution);
            
            solution2.setContent(existingSolution.getContent());
            solution2.setCommentaire("(cf le contenu de la solution existante choisie)");
            if (existingSolution.getCompteur() == null || existingSolution.getCompteur() == 0) {
                existingSolution.setCompteur(1L);
            } else {
                existingSolution.setCompteur(existingSolution.getCompteur() + 1);
            }
            existingSolution = existingSolutionRepository.save(existingSolution);

        }
        System.out.println("Here 6 ");
        // Is Affected claim ?
        claim.setStatus(ClaimStatus.TREAT);
        solution2.setStatus(SolutionStatus.APPROVED);
        String type = "réclamation";
        if (claim.getType().equals(ClaimType.DENUNCIACION)) {
            type = "dénonciation";
        }

        final Claim finalClaim = claim;
        final String finalType = type;
        if (claim.hasAffectedTreatment() && treator.getCode() == claim.getTreatmentAffectedTo().getCode()) {
            // Is treator is user who receiverd affectation
            // claim.setStatus(ClaimStatus.TO_APPROUVED);
            // solution2.setStatus(SolutionStatus.UNAPPROVED);

            Double apercuContent = claim.getContent().length() * 0.3;

            String message = "" +
                    "Cher(e) " + claim.getTreatmentAffectedBy().getFirstandlastname() + ",\n\n" +
                    "L'utilisateur " + treator.getFirstandlastname() + " a examiné la " + type + " portant le code : "
                    + claim.getCodeClient()
                    + " qui lui a été affectée et a proposé une solution pour résoudre cette " + type + "." + "\n\n" +
                    "Détails de la " + type + " :" + "\n\n" +
                    "* Code de " + type + " : " + claim.getCodeClient() + "\n" +
                    "* Date d'enregistrement : " + Utils.convertLocalDateTimeToStr(claim.getReceiptDateTime()) + "\n" +
                    "* Aperçu du contenu : " + claim.getContent().substring(0, apercuContent.intValue()) + "...\n\n" +
                    "La solution proposée par " + treator.getFirstandlastname() + " est la suivante : " + "\n" +
                    request.getSolution() + "\n\n" +
                    "Nous vous invitons à examiner attentivement cette solution.";
            
            // Envoi de mail en parallèle
            CompletableFuture.runAsync(() -> {
                try {
                    Utils.sendmail(finalClaim.getTreatmentAffectedBy().getEmail(), "Proposition de solution à une " + finalType + "",
                            message, null, " ", settingServiceImpl);
                
                    Log successLog = Log.builder()
                        .libelle("Mail notification  proposition de solution")
                        .content("Success mail notification proposition de solution")
                        .createdAt(LocalDateTime.now())
                        .type(LogType.INFO)
                        .userId(0L)
                        .userIpAddress("")
                        .target(LogTarget.APP)
                        .build();

                    logServiceImpl.saveLog(successLog);                            
                } catch (Exception e) {                
                    if (e != null) {
                        Log log2 = Log
                                .builder()
                                .libelle("Echec mail proposition de solution")
                                .content(e.getMessage())
                                .createdAt(LocalDateTime.now())
                                .type(LogType.ERROR)
                                .userId(0L)
                                .userIpAddress("")
                                .target(LogTarget.APP)
                                .build();

                        logServiceImpl.saveLog(log2);
                    }
                }
            });
        } else {
            claim.setStatus(ClaimStatus.TREAT);
            solution2.setStatus(SolutionStatus.APPROVED);
            solution2.setUpdatedAt(LocalDateTime.now());
            Double apercuContent = claim.getContent().length() * 0.3;
            List<User> pilote = authServiceImpl.getUsersByRoles(Arrays.asList(Role.PILOTE));
            if (pilote != null && !pilote.isEmpty()) {                            
                String message = "" +
                        "Cher(e) " + pilote.get(0).getFirstandlastname() + ", Pilote de la plateforme GPR, \n\n" +
                        "l'utilisateur " + treator.getFirstandlastname()
                        + " a examiné la " + type + " portant le code : "
                        + claim.getCodeClient()
                        + " et l'a traitée." + "\n\n" +
                        "Détails de la " + type + " :" + "\n\n" +
                        "* Code de " + type + " : " + claim.getCodeClient() + "\n" +
                        "* Date d'enregistrement : " + Utils.convertLocalDateTimeToStr(claim.getReceiptDateTime())
                        + "\n" +
                        "* Aperçu du contenu : " + claim.getContent().substring(0, apercuContent.intValue()) + "...\n\n"
                        +
                        "La solution proposée par " + treator.getFirstandlastname() + " est la suivante : " + "\n" +
                        request.getSolution() + "\n\n" +
                        "Nous vous invitons à communiquer la solution au plaignant pour mesurer sa satisfaction. ";

                // Envoi de mail en parallèle
                CompletableFuture.runAsync(() -> {
                    try {
        
                        Utils.sendmail(pilote.get(0).getEmail(), "" + finalType + " traitée",
                                message, null, " ", settingServiceImpl);
                                                
                        Log successLog = Log.builder()
                            .libelle("Mail notification notification " + finalType + " traitée")
                            .content("Success mail notification notification " + finalType + " traitée")
                            .createdAt(LocalDateTime.now())
                            .type(LogType.INFO)
                            .userId(0L)
                            .userIpAddress("")
                            .target(LogTarget.APP)
                            .build();

                        logServiceImpl.saveLog(successLog);
                    } catch (Exception e) {                        
                        if (e != null) {
                            Log log2 = Log
                                    .builder()
                                    .libelle("Echec mail notification " + finalType + " traitée")
                                    .content(e.getMessage())
                                    .createdAt(LocalDateTime.now())
                                    .type(LogType.ERROR)
                                    .userId(0L)
                                    .userIpAddress("")
                                    .target(LogTarget.APP)
                                    .build();

                            logServiceImpl.saveLog(log2);
                        }
                    }
                }); 
            }

        }
        // System.out.println("Here 7 ");
        solution2 = solutionServiceImpl.saveSolution(solution2);
        claim.setTreatBy(treator);
        // List<Solution> oldSolutions = claim.getSolutions();
        // oldSolutions.add(solution2);
        claim.getSolutions().add(solution2);

        claim.setUpdatedAt(LocalDateTime.now());
        System.out.println("Here 8 ");
        claim = repository.save(claim);
        System.out.println("Here 9 ");
        return claim;
    }

    @Override
    public Claim measureClaim(Claim claim, Solution solution, User measurer, SatisfactionStatus status, String commentaire) {

        SatisfactionMeasure satisfactionMeasure = SatisfactionMeasure
                .builder()
                .measurer(measurer)
                .measureDateTime(LocalDateTime.now())
                .commentaire(commentaire)
                .solution(solution)
                .status(status)
                .build();

        satisfactionMeasure = satifactionMeasureServiceImpl.saveSatisfactionMeasure(satisfactionMeasure);
        solution.setSatisfactionMeasure(satisfactionMeasure);

        satisfactionMeasure = satifactionMeasureServiceImpl.saveSatisfactionMeasure(satisfactionMeasure);
        solution = solutionServiceImpl.saveSolution(solution);

        satisfactionMeasure.setSolution(solution);

        if (status == SatisfactionStatus.SATISFIED) {
            claim.setStatus(ClaimStatus.SATISFIED);
        } else if (status == SatisfactionStatus.UNSATISFIED) {
            claim.setStatus(ClaimStatus.UNSATISFIED);
            // send mail to

            if (claim.getSession() == null) { // To CGR if it is direct treat
                String message = "Cher(e) utilisateur, le client ayant fait la réclamation : " + claim.getCodeClient()
                        + " n'est pas satisfait de la solution proposée par "
                        + claim.getTreatBy().getFirstandlastname() + ". \n\n " +
                        "Veuillez vous connectez à la plateforme GPR afin de prendre des mesures adéquates par rapport à cette réclamation.";
                List<User> cgrs = authServiceImpl.getUsersByRoles(Arrays.asList(Role.PILOTE));

                            
                // Envoi de mail en parallèle
                CompletableFuture.runAsync(() -> {
                    try {
                        Utils.sendmail(cgrs, "RECLAMATION NON SATISFAITE", message, null, " ", settingServiceImpl);
                                                
                        Log successLog = Log.builder()
                            .libelle("Mail notification réclamation non satisfaite")
                            .content("Success mail notification réclamation non satisfaite")
                            .createdAt(LocalDateTime.now())
                            .type(LogType.INFO)
                            .userId(0L)
                            .userIpAddress("")
                            .target(LogTarget.APP)
                            .build();

                        logServiceImpl.saveLog(successLog);
                    } catch (Exception e) {                    
                        if (e != null) {
                            Log log2 = Log
                                    .builder()
                                    .libelle("Echec mail notification réclamation non satisfaite")
                                    .content(e.getMessage())
                                    .createdAt(LocalDateTime.now())
                                    .type(LogType.ERROR)
                                    .userId(0L)
                                    .userIpAddress("")
                                    .target(LogTarget.APP)
                                    .build();
    
                            logServiceImpl.saveLog(log2);
                        }
                    }
                });
            } else {// TODE and CA if it's come from CGR
                String message = "Cher(e) utilisateur, le client ayant fait la réclamation : "
                        + claim.getCodeClient()
                        + " est non-satisfait de la solution qui lui a été proposée. \n\n" +
                        "Veuillez vous connectez à la plateforme GPR afin de prendre les mesures adéquates.";
                List<User> cgrs = authServiceImpl.getUsersByRoles(Arrays.asList(Role.PILOTE, Role.DE));

                            
                // Envoi de mail en parallèle
                CompletableFuture.runAsync(() -> {
                    try { 
                        Utils.sendmail(cgrs, "RECLAMATION NON SATISFAITE", message, null,
                                " ", settingServiceImpl);
                                                
                        Log successLog = Log.builder()
                            .libelle("Mail notification réclamation non satisfaite")
                            .content("Success mail notification réclamation non satisfaite")
                            .createdAt(LocalDateTime.now())
                            .type(LogType.INFO)
                            .userId(0L)
                            .userIpAddress("")
                            .target(LogTarget.APP)
                            .build();

                        logServiceImpl.saveLog(successLog);                                
                    } catch (Exception e) {                        
                        if (e != null) {
                            Log log2 = Log
                                    .builder()
                                    .libelle("Echec mail notification réclamation non satisfaite")
                                    .content(e.getMessage())
                                    .createdAt(LocalDateTime.now())
                                    .type(LogType.ERROR)
                                    .userId(0L)
                                    .userIpAddress("")
                                    .target(LogTarget.APP)
                                    .build();

                            logServiceImpl.saveLog(log2);
                        }
                    }
                });
            }

        } else {
            claim.setStatus(ClaimStatus.PARTIAL_SATISFIED);

            if (claim.getSession() == null) { // To CGR if it is direct treat
                String message = "Cher(e) utilisateur, le client ayant fait la réclamation : " + claim.getCodeClient()
                        + " est partiellement satisfait de la solution proposée par "
                        + claim.getTreatBy().getFirstandlastname() + ". \n\n " +
                        "Veuillez vous connectez à la plateforme GPR afin de prendre les mesures adéquates.";
                List<User> cgrs = authServiceImpl.getUsersByRoles(Arrays.asList(Role.PILOTE));

                                            
                // Envoi de mail en parallèle
                CompletableFuture.runAsync(() -> {
                    try {
                        Utils.sendmail(cgrs, "RECLAMATION PARTIELLEMENT-SATISFAITE", message, null,
                                " ", settingServiceImpl);
                                                
                        Log successLog = Log.builder()
                            .libelle("Mail notification réclamation partiellement satisfaite")
                            .content("Success mail notification réclamation partiellement satisfaite")
                            .createdAt(LocalDateTime.now())
                            .type(LogType.INFO)
                            .userId(0L)
                            .userIpAddress("")
                            .target(LogTarget.APP)
                            .build();

                        logServiceImpl.saveLog(successLog);                                
                    } catch (Exception e) {                    
                        if (e != null) {
                            Log log2 = Log
                                    .builder()
                                    .libelle("Echec mail notification réclamation partiellement satisfaite")
                                    .content(e.getMessage())
                                    .createdAt(LocalDateTime.now())
                                    .type(LogType.ERROR)
                                    .userId(0L)
                                    .userIpAddress("")
                                    .target(LogTarget.APP)
                                    .build();
    
                            logServiceImpl.saveLog(log2);
                        }
                    }
                }); 
            } else {// TODE and CA if it's come from CGR
                String message = "Cher(e) utilisateur, le client ayant fait la réclamation : "
                        + claim.getCodeClient()
                        + " est partiellement satisfait de la solution proposée par " 
                        + claim.getTreatBy().getFirstandlastname() + ". \n\n " +
                        
                        "Veuillez vous connectez à la plateforme GPR afin de prendre les mesures adéquates.";
                List<User> cgrs = authServiceImpl.getUsersByRoles(Arrays.asList(Role.PILOTE, Role.DE));
                                            
                // Envoi de mail en parallèle
                CompletableFuture.runAsync(() -> {
                    try {
                        Utils.sendmail(cgrs, "RECLAMATION PARTIELLEMENT-SATISFAITE", message, null,
                                " ", settingServiceImpl);
                                                
                        Log successLog = Log.builder()
                            .libelle("Mail notification réclamation partiellement satisfaite")
                            .content("Success mail notification réclamation partiellement satisfaite")
                            .createdAt(LocalDateTime.now())
                            .type(LogType.INFO)
                            .userId(0L)
                            .userIpAddress("")
                            .target(LogTarget.APP)
                            .build();

                        logServiceImpl.saveLog(successLog);                                
                    } catch (Exception e) {                    
                        if (e != null) {
                            Log log2 = Log
                                    .builder()
                                    .libelle("Echec mail notification réclamation partiellement satisfaite")
                                    .content(e.getMessage())
                                    .createdAt(LocalDateTime.now())
                                    .type(LogType.ERROR)
                                    .userId(0L)
                                    .userIpAddress("")
                                    .target(LogTarget.APP)
                                    .build();
    
                            logServiceImpl.saveLog(log2);
                        }
                    }
                });
            }
        }

        claim.setUpdatedAt(LocalDateTime.now());

        claim = repository.save(claim);

        return claim;

    }

    @Override
    public Claim unApprouvedSolution(Claim claim, Solution solution, User unApprouver, String commentaire) {

        solution.setStatus(SolutionStatus.UNAPPROVED);
        solution.setMotifDesaprobation(commentaire);
        solution.setUpdatedAt(LocalDateTime.now());
        solution.setUnApprouver(unApprouver);
        solution.setUnApprouvedAt(LocalDateTime.now());

        solution = solutionServiceImpl.saveSolution(solution);

        claim.setStatus(ClaimStatus.DESAPPROUVED);
        claim.setUpdatedAt(LocalDateTime.now());

        if(claim.getTreatmentAffectedTo() == null){
            claim.setTreatmentAffectedTo(solution.getAuthor());
            claim.setTreatmentAffectedBy(unApprouver);
        }
        claim = repository.save(claim);
        // TODO send mail to CGR User
        Double apercuContent = claim.getContent().length() * 0.5;
        List<User> cgrMembers = authServiceImpl.getUsersByRoles(Arrays.asList(Role.PILOTE));
        System.out.println("here 6");
        String type = "réclamation";
        if (claim.getType().equals(ClaimType.DENUNCIACION)) {
            type = "dénonciation";
        }
        String message = "" +
                "Cher(e) utilisateur ,\n\n" +
                "le DE " + unApprouver.getFirstandlastname()
                + " a examiné et désapprouvé la solution que vous avez proposé pour la " + type + " portant le code : "
                + claim.getCodeClient() + "\n\n" +
                "Détails de la " + type + " :" + "\n\n" +
                "* Date d'enregistrement : " + Utils.convertLocalDateTimeToStr(claim.getReceiptDateTime()) + "\n" +
                "* Aperçu du contenu : " + claim.getContent().substring(0, apercuContent.intValue()) + "...\n\n" +
                "* Motif de désapprobation : " + commentaire + "\n\n" +
                "Nous vous invitons à examiner attentivement le commentaire laissé puis à proposer une nouvelle solution.";
                                    
        // Envoi de mail en parallèle
        CompletableFuture.runAsync(() -> {
            try {
                Utils.sendmail(cgrMembers, "Solution désapprouvée",
                        message, null, " ", settingServiceImpl);
                                                
                    Log successLog = Log.builder()
                        .libelle("Mail notification solution désapprouvée")
                        .content("Success mail notification solution désapprouvée")
                        .createdAt(LocalDateTime.now())
                        .type(LogType.INFO)
                        .userId(0L)
                        .userIpAddress("")
                        .target(LogTarget.APP)
                        .build();

                    logServiceImpl.saveLog(successLog);                        
            } catch (Exception e) {
                if (e != null) {
                    Log log2 = Log
                            .builder()
                            .libelle("Echec mail notification solution désapprouvée")
                            .content(e.getMessage())
                            .createdAt(LocalDateTime.now())
                            .type(LogType.ERROR)
                            .userId(0L)
                            .userIpAddress("")
                            .target(LogTarget.APP)
                            .build();
    
                    logServiceImpl.saveLog(log2);
                }    
            }
        });

        return claim;
    }

    @Override
    public Claim approuvedSolution(Claim claim, Solution solution, User approuver) {

        solution.setStatus(SolutionStatus.APPROVED);
        solution.setUpdatedAt(LocalDateTime.now());
        solution.setApprouvedAt(LocalDateTime.now());
        solution.setApprouver(approuver);
        solution = solutionServiceImpl.saveSolution(solution);

        claim.setStatus(ClaimStatus.TREAT);
        claim.setUpdatedAt(LocalDateTime.now());
        claim = repository.save(claim);
        final Claim finalClaim = claim;
        String type = "réclamation";
        if (claim.getType().equals(ClaimType.DENUNCIACION)) {
            type = "dénonciation";
        }
        String message = "" +
                "Cher(e) " + finalClaim.getTreatmentAffectedTo().getFirstandlastname() + ",\n\n" +
                "L'utilisateur " + approuver.getFirstandlastname()
                + " a examiné et approuvé la solution que vous avez proposée pour la " + type + " portant le code : "
                + finalClaim.getCodeClient() + "\n\n";
                            
        // Envoi de mail en parallèle
        CompletableFuture.runAsync(() -> {
            try {
                Utils.sendmail(finalClaim.getTreatmentAffectedTo().getEmail(), "Solution approuvée",
                        message, null, " ", settingServiceImpl);
                                                
                Log successLog = Log.builder()
                    .libelle("Mail notification solution approuvée")
                    .content("Success mail notification solution approuvée")
                    .createdAt(LocalDateTime.now())
                    .type(LogType.INFO)
                    .userId(0L)
                    .userIpAddress("")
                    .target(LogTarget.APP)
                    .build();

                logServiceImpl.saveLog(successLog);                        
            } catch (Exception e) {             
                if (e != null) {
                    Log log2 = Log
                            .builder()
                            .libelle("Echec mail notification solution approuvée")
                            .content(e.getMessage())
                            .createdAt(LocalDateTime.now())
                            .type(LogType.ERROR)
                            .userId(0L)
                            .userIpAddress(null)
                            .target(LogTarget.APP)
                            .build();
    
                    logServiceImpl.saveLog(log2);
                }
            }      
        });

        return claim;
    }

    @Override
    public Claim classedClaim(Claim claim, User classer) {
        claim.setStatus(ClaimStatus.CLASSED);
        claim.setUpdatedAt(LocalDateTime.now());
        claim.setClassedBy(classer);
        claim = repository.save(claim);
        return claim;
    }

    @Override
    public Claim unClassedClaim(Claim claim, User classer) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'unClassedClaim'");
    }

    @Override
    public Claim litigateClaim(Claim claim, User litigator, List<ExternalRecourse> externalRecour) {

        claim.setStatus(ClaimStatus.LITIGATION);
        claim.setExternalRecourses(externalRecour);
        claim.setUpdatedAt(LocalDateTime.now());

        claim = repository.save(claim);
        LocalDateTime majDate = LocalDateTime.now();
        for (ExternalRecourse externalRecourse : externalRecour) {
            externalRecourse.setUpdatedAt(majDate);
            List<Claim> oldClaimsChoosed = externalRecourse.getClaims();
            oldClaimsChoosed.add(claim);
            externalRecourse.setClaims(oldClaimsChoosed);
            externalRecourseServiceImpl.saveExternalRecourse(externalRecourse);
        }

        return claim;
    }

    @Override
    public List<Claim> getClaimByStatus(ClaimType type, ClaimStatus status) {
        return repository.findByTypeAndStatus(type, status);
    }

    @Override
    public List<Claim> getAllNotTempSave(ClaimType type) {
        return repository.findByTypeAndStatusNot(type, ClaimStatus.TEMP_SAVED);
    }

    @Override
    public List<Claim> getAllByTypeStatusCollector(ClaimType type, ClaimStatus status, User collector) {
        return repository.findByTypeAndStatusAndCollector(type, status, collector);
    }

    @Override
    public List<Claim> getAllByTypeAndStatusIn(ClaimType type, List<ClaimStatus> statusList) {
        return repository.findByTypeAndStatusIn(type, statusList);
    }

    @Override
    public List<Claim> getAllByTypeAndCollectorAndStatusOrTreatmentAffectedToAndStatusIn(ClaimType type,
            ClaimStatus status, User affectedTo,
            List<ClaimStatus> statusList) {

        List<Claim> resultat = repository.findByTypeAndCollectorAndStatusOrTypeAndTreatmentAffectedToAndStatusIn(type,
                affectedTo, status, type, affectedTo, statusList);
        List<Claim> tmp = resultat;

        tmp = resultat.stream().filter(claim -> {
            if (!affectedTo.canTreatHighRiskClaim() && claim.getObjet().getRisqueLevel() == GravityLevel.GRAVE) {
                return false;
            } else if (claim.getObjet().getRisqueLevel() == GravityLevel.MOYEN
                    && !affectedTo.canTreatMiddleRiskClaim()) {
                return false;
            } else if (claim.getObjet().getRisqueLevel() == GravityLevel.MINEUR
                    && !affectedTo.canTreatMinorRiskClaim()) {
                return false;
            }else {
                return true;
            }
        }).collect(Collectors.toList());

        return tmp;
    }

    @Override
    public List<Claim> getAllWithLatestApprouvedSolutionByTypeAndStatusIn(ClaimType type,
            List<ClaimStatus> statusList) {
        List<Claim> req = repository.findByTypeAndStatusIn(type, statusList);
        for (Claim claim : req) {
            Collections.reverse(claim.getSolutions());
            claim.getSolutions().removeIf(solution -> solution.getStatus() == SolutionStatus.UNAPPROVED);
            if (claim.getSolutions().size() > 0) {
                claim.setSolutions(Arrays.asList(claim.getSolutions().get(0)));
            }

        }

        return req;
    }

    @Override
    public List<Claim> getAllWithLatestSolutionByTypeAndStatusOrTypeAndTreatmentAffectedToAndStatusIn(ClaimType type,
            ClaimStatus status, User affectedTo, List<ClaimStatus> statusList) {
        return repository.findClaimsWithLatestSolutionByTypeAndStatusOrTypeAndTreatmentAffectedToAndStatusIn(type,
                status, affectedTo, statusList);
    }

    @Override
    public List<Claim> getAllWithApprovedSolutionByTypeAndStatus(ClaimType type, List<ClaimStatus> statusList) {
        List<Claim> req = repository.findByTypeAndStatusIn(type, statusList);
        for (Claim claim : req) {
            Collections.reverse(claim.getSolutions());
            claim.getSolutions().removeIf(solution -> solution.getStatus() == SolutionStatus.UNAPPROVED);

        }

        return req;
    }

    @Override
    public Claim saveClaim(SaveDenunRequest claimPart, ClaimType type) throws Exception {
        DenunRequest claimToSave = claimPart.getClaimRequest();
        User collector;
        try {
            collector = authServiceImpl.getById(claimToSave.getCollectorId());
        } catch (Exception e) {
            throw new Exception("Collector " + claimToSave.getCollectorId() + " of the claim not found");
        }
        // if(claimToSave.getCode() != null && claimToSave.getId() != null){
        // Claim oldClaim = repository.findByCode(claimToSave.getCode()).orElseThrow(()
        // -> new ClaimException("Claim with this code not exist"));
        // } else {

        // }

       

        Claim claim = Claim
                .builder()
                .type(type)
                .content(claimToSave.getContent())
                .collector(collector).status(ClaimStatus.SAVED)
                .createdAt(LocalDateTime.now())
                .receiptDateTime(Utils.convertStrToLocalDateTime(claimToSave.getReceiptDateTime()))
                .build();
        if (claimToSave.getCode() != null && claimToSave.getId() != null) {
            claim.setId(claimToSave.getId());
            claim.setCode(claimToSave.getCode());
            // Only TEMP_SAVED can be saved
            Claim oldClaim = repository.findByCode(claimToSave.getCode())
                    .orElseThrow(() -> new ClaimException("Claim with this code doesn't exist"));
                    claim.setCodeClient(oldClaim.getCodeClient());
            
                  
            if (oldClaim.getStatus() != ClaimStatus.TEMP_SAVED) {
                throw new ClaimException(
                        "Invalid operation! this claim is not temporarly saved, you can't change it again");
            }
            if (oldClaim.getCodeClient() == null || oldClaim.getCodeClient() == "") {
                String codeClient = "DEN-" + UUID.randomUUID().toString().substring(0, 4);
                claim.setCodeClient(codeClient);
            }
            else {
                claim.setCodeClient(oldClaim.getCodeClient());
                // claim.setCodeClient(claimToSave.getCodeClient());
            }

        } else {
            String code = generateCode(collector.getServicePoint().getUuid(), collector.getCode(), type);
            claim.setCode(code);
            String codeClient = "DEN-" + UUID.randomUUID().toString().substring(0, 4);
            claim.setCodeClient(codeClient);
        }
        CollectionChannel collectionChannel;
        if (claimToSave.getCollectionChannelId() != null) {
            try {
                collectionChannel = collectionChannelService.getById(claimToSave.getCollectionChannelId());
                claim.setCollectionChannel(collectionChannel);
            } catch (Exception e) {
                throw new Exception("Canal de collecte introuvable");
            }
        }
        ServicePoint servicePoint;
        if (claimToSave.getServicePointId() != null) {
            try {
                servicePoint = servicePointServiceImpl.getById(claimToSave.getServicePointId());
                claim.setServicePoint(servicePoint);
            } catch (Exception e) {
                throw new Exception("Point Service introuvable");
            }
        }

        Product product;
        if (claimToSave.getProductId() != null) {
            try {
                product = productServiceImpl.getById(claimToSave.getProductId());
                claim.setProduct(product);
            } catch (Exception e) {
                throw new Exception("Product introuvable");
            }
        }

        Objet objet;
        if (claimToSave.getObjetId() != null) {
            try {
                objet = objetServcieImpl.getById(claimToSave.getObjetId());
                claim.setObjet(objet);
            } catch (Exception e) {
                throw new Exception("Objet introuvable");
            }
        }

        Language language;
        if (claimToSave.getLanguageId() != null) {
            try {
                language = languageServiceImpl.getById(claimToSave.getLanguageId());
                claim.setLanguage(language);
            } catch (Exception e) {
                throw new Exception("Langage introuvable");
            }
        }

        claim = repository.save(claim);
        final Claim finalClaim = claim;

        if (claimPart.getFiles() != null && claimPart.getFiles().length != 0) {
            List<Media> medias = mediaServiceImpl.store(claimPart.getFiles(), claim);
            claim.setUpdatedAt(LocalDateTime.now());
            claim.setMedias(medias);
        }
        if (claimPart.getAudios() != null && claimPart.getAudios().length != 0) {
            List<ClaimAudio> audios = claimAudioServiceImpl.store(claimPart.getAudios(), claim);
            claim.setUpdatedAt(LocalDateTime.now());
            // for (ClaimAudio audio : audios) {
            // audio.setClaim(null);
            // }
            // claim.setAudios(audios);
        }

        //Whatsapp
        if(claimToSave.getFromWhatsapp()){
            System.out.println("From Whatsapp");
            Boolean isOk = mediaServiceImpl.attachFileToClaim(claim, claimToSave.getFilesWhatsapp());
            if(isOk && claimToSave.getInboxWhatsapp() != null){
                List<InboxMessage> messages = messageRepository.findByInbox(claimToSave.getInboxWhatsapp());
                for (InboxMessage message : messages) {
                    messageRepository.delete(message);
                }
                inboxRepository.delete(claimToSave.getInboxWhatsapp());
            }
        }

        Double apercuContent = claim.getContent().length() * 0.3;
        String message = "" +
                "Cher(e) utilisteur" +
                "Une nouvelle réclamation a été enregistrée avec succès dans notre système. Vous recevez cette notification en tant qu'utilisateur habilité à recevoir des notification lorsqu'une nouvelle réclamation est enregistrée."
                + "\n\n" +
                "Détails de la réclamation :" + "\n\n" +
                "* Code de réclamation : " + claim.getCodeClient() + "\n" +
                "* Date d'enregistrement : " + Utils.convertLocalDateTimeToStr(claim.getReceiptDateTime()) + "\n" +
                "* Aperçu du contenu : " + claim.getContent().substring(0, apercuContent.intValue()) + "...\n\n" +
                "Nous vous encourageons à examiner cette réclamation dès que possible et à prendre les mesures nécessaires pour son traitement. Votre expertise et vos compétences sont essentielles pour assurer une résolution rapide et satisfaisante pour les clients.";
                                    
        // Envoi de mail en parallèle
        CompletableFuture.runAsync(() -> {
            try {
                Utils.sendmail(authServiceImpl.getEmailReceiversForNotif(finalClaim.getServicePoint()),
                        " Notification d'enregistrement de réclamation", message, null,
                        " ", settingServiceImpl);
                                                
                Log successLog = Log.builder()
                    .libelle("Mail notification nouvelle réclamation")
                    .content("Success mail notification nouvelle réclamation")
                    .createdAt(LocalDateTime.now())
                    .type(LogType.INFO)
                    .userId(0L)
                    .userIpAddress("")
                    .target(LogTarget.APP)
                    .build();

                logServiceImpl.saveLog(successLog);                           
            } catch (Exception e) {
                if (e != null) {
                    Log log2 = Log
                            .builder()
                            .libelle("Echec mail notification nouvelle réclamation")
                            .content(e.getMessage())
                            .createdAt(LocalDateTime.now())
                            .type(LogType.ERROR)
                            .userId(0L)
                            .userIpAddress(claimPart.getRemoteAddress())
                            .target(LogTarget.APP)
                            .build();
    
                    logServiceImpl.saveLog(log2);
                }
            }
        });

        return claim;
    }

    @Override
    public List<Claim> getAllByTypeAndStatusNotIn(ClaimType type, List<ClaimStatus> status) {
        return repository.findByTypeAndStatusNotIn(type, status);
    }

    @Override
    public List<AlertDto> getAllAlertDtosByType(ClaimType type) {
        List<ClaimStatus> lStatus = Arrays.asList(ClaimStatus.CLASSED, ClaimStatus.SATISFIED, ClaimStatus.UNSATISFIED,
                ClaimStatus.PARTIAL_SATISFIED, ClaimStatus.TEMP_SAVED, ClaimStatus.LITIGATION);
        if (type == ClaimType.DENUNCIACION) {
            lStatus = Arrays.asList(ClaimStatus.CLASSED, ClaimStatus.SATISFIED, ClaimStatus.UNSATISFIED,
                    ClaimStatus.PARTIAL_SATISFIED, ClaimStatus.TEMP_SAVED, ClaimStatus.TREAT, ClaimStatus.LITIGATION);
        }
        List<Claim> allClaims = this.getAllByTypeAndStatusNotIn(type, lStatus);
        boolean isOneSolutionMeasured = false;
        List<AlertDto> claimAlertDtos = new ArrayList<>();
        AlertDto alertDto = AlertDto.builder().build();
        for (Claim claim : allClaims) {
            // vérifier s'il y a au moins une solution ne pas prendre en compte

            // if (!claim.getSolutions().isEmpty() && type == ClaimType.CLAIM) {
            //     for (Solution solution : claim.getSolutions()) {
            //         if (solution.getSatisfactionMeasure() != null) {
            //             // allClaims.remove(claim);
            //             isOneSolutionMeasured = true;
            //             break;
            //         }
            //     }
            // }
            // if (!isOneSolutionMeasured) {
                LocalDateTime calculateDate = claim.getReceiptDateTime().plusDays(claim.getObjet().getProcessingTime());
                if (LocalDateTime.now().isAfter(calculateDate)) {
                    Long hoursRetard = calculateDate.until(LocalDateTime.now(), ChronoUnit.HOURS);
                    Long days = hoursRetard / 24;
                    Long hours = hoursRetard % 24;
                    alertDto = AlertDto
                            .builder()
                            .claimClient(claim.getClientFirstAndLastName())
                            .claimCode(claim.getCode())
                            .claimId(claim.getId())
                            .retardDay(days + " jr(s) " + hours + " heure(s)")
                            .declenchedDate(calculateDate)
                            .receiptDateTime(claim.getReceiptDateTime())
                            .status(claim.getStatus())
                            .build();
                    claimAlertDtos.add(alertDto);
                }
            // }
        }

        return claimAlertDtos;
    }

    @Override
    public Claim getByCode(String code) throws Exception {
        return repository.findByCode(code).orElseThrow(() -> new Exception("Réclamation introuvable"));
    }

    // @Override
    public Claim getByCodeClient(String code) throws Exception {
        return repository.findByCodeClient(code).orElseThrow(() -> new Exception("Réclamation introuvable"));
    }

    @Override
    public void saveClaimOffline(SaveRequest claimPart, ClaimType type) throws Exception {
        ClaimRequest claimToSave = claimPart.getClaimRequest();
        User collector;
        try {
            collector = authServiceImpl.getById(claimToSave.getCollectorId());
        } catch (Exception e) {
            throw new Exception("Collector " + claimToSave.getCollectorId() + " of the claim offline not found");
        }
        // if(claimToSave.getCode() != null && claimToSave.getId() != null){
        // Claim oldClaim = repository.findByCode(claimToSave.getCode()).orElseThrow(()
        // -> new ClaimException("Claim with this code not exist"));
        // } else {

        // }

        //String codeClient = "REC-" + UUID.randomUUID().toString().substring(0, 4);
        Claim claim = Claim
                .builder()
                .clientFirstAndLastName(claimToSave.getClientFirstAndLastName())
                .gender(Gender.valueOf(claimToSave.getGender()))
                .type(type)
                .address(claimToSave.getAddress())
                .tel(claimToSave.getPhone())
                .crew(claimToSave.getCrew())
                .folderCode(claimToSave.getFolderCode())
                .content(claimToSave.getContent())
                .collector(collector)
                .status(ClaimStatus.SAVED)
                .createdAt(LocalDateTime.now())
                .onlineUploadDateTime(claimToSave.getOnlineUploadDateTime())
                .receiptDateTime(Utils.convertStrToLocalDateTime(claimToSave.getReceiptDateTime()))
                .build();

        if (claimToSave.getStatus() != null) {
            claim.setStatus(claimToSave.getStatus());
        } else {
            claim.setStatus(ClaimStatus.SAVED);
        }

        if (claimToSave.getCreatedAt() != null && !claimToSave.getCreatedAt().isEmpty()) {
            claim.setCreatedAt(Utils.convertStrWithTToLocalDateTime(claimToSave.getCreatedAt()));
        } else {
            claim.setCreatedAt(LocalDateTime.now());
        }

        if (claimToSave.getId() != null) {
            claim.setId(claimToSave.getId());
            claim.setCode(claimToSave.getCode());
            
            // Only TEMP_SAVED can be saved
            Claim oldClaim = repository.findById(claimToSave.getId())
            .orElseThrow(() -> new ClaimException("Claim with this code doesn't exist"));

            //claim.setCodeClient(oldClaim.getCodeClient());

            // if (oldClaim.getStatus() != ClaimStatus.TEMP_SAVED) {
            // throw new ClaimException(
            // "Invalid operation! this claim is not temporarly saved, you can't change it
            // again");
            // }

        } else {
            if (claimToSave.getCode() == null || claimToSave.getCode() == "") {
                String code = generateCode(collector.getServicePoint().getUuid(), collector.getCode(), type);
                claim.setCode(code);
            } else {
                claim.setCode(claimToSave.getCode());
            }
        }
        // if(claimToSave.getCode() == null || claimToSave.getCode()== "") {
        // String code = generateCode(collector.getServicePoint().getUuid(),
        // collector.getCode(), type);
        // claim.setCode(code);
        // }
        CollectionChannel collectionChannel;
        if (claimToSave.getCollectionChannelId() != null) {
            try {
                collectionChannel = collectionChannelService.getById(claimToSave.getCollectionChannelId());
                claim.setCollectionChannel(collectionChannel);
            } catch (Exception e) {
                throw new Exception("Canal de collecte introuvable");
            }
        }
        ServicePoint servicePoint;
        if (claimToSave.getServicePointId() != null) {
            try {
                servicePoint = servicePointServiceImpl.getById(claimToSave.getServicePointId());
                claim.setServicePoint(servicePoint);
            } catch (Exception e) {
                throw new Exception("Point Service introuvable");
            }
        }

        Product product;
        if (claimToSave.getProductId() != null) {
            try {
                product = productServiceImpl.getById(claimToSave.getProductId());
                claim.setProduct(product);
            } catch (Exception e) {
                throw new Exception("Product introuvable");
            }
        }

        Objet objet;
        if (claimToSave.getObjetId() != null) {
            try {
                objet = objetServcieImpl.getById(claimToSave.getObjetId());
                claim.setObjet(objet);
            } catch (Exception e) {
                throw new Exception("Objet introuvable");
            }
        }

        Language language;
        if (claimToSave.getLanguageId() != null) {
            try {
                language = languageServiceImpl.getById(claimToSave.getLanguageId());
                claim.setLanguage(language);
            } catch (Exception e) {
                throw new Exception("Langage introuvable");
            }
        }


        claim = repository.save(claim);

        try {

            Utils.sendSms(claim.getTel(),
                    "Cher(e) client, Votre réclamation a été prise en compte. Nous vous recontacterons dès que possible avec une solution.", settingServiceImpl);

        } catch (Exception ex) {
            // TODO Auto-generated catch block
            ex.printStackTrace();
        }

        if (claimPart.getAudios() != null && claimPart.getAudios().length != 0) {
            List<ClaimAudio> audios = claimAudioServiceImpl.store(claimPart.getAudios(), claim);
            claim.setUpdatedAt(LocalDateTime.now());
            // for (ClaimAudio audio : audios) {
            // audio.setClaim(null);
            // }
            // claim.setAudios(audios);
        }

        if (claimPart.getFiles() != null && claimPart.getFiles().length != 0) {
            List<Media> medias = mediaServiceImpl.store(claimPart.getFiles(), claim);
            claim.setUpdatedAt(LocalDateTime.now());
            claim.setMedias(medias);
            claim = repository.save(claim);
        }
    }

    @Override
    public void saveTempClaimOffline(SaveRequest claimPart, ClaimType type) throws Exception {
        ClaimRequest claimToSave = claimPart.getClaimRequest();
        Claim claim = Claim
                .builder().build();
        User collector;
        try {
            collector = authServiceImpl.getById(claimToSave.getCollectorId());
            claim.setCollector(collector);
        } catch (Exception e) {
            throw new Exception("Collector " + claimToSave.getCollectorId() + " of the temp claim not found");
        }
        if (claimToSave.getId() != null) {
            Claim oldClaim = repository.findById(claimToSave.getId())
                    .orElseThrow(() -> new Exception("Aucune réclamation ne porte ce code"));
            claim = oldClaim;
            claim.setId(oldClaim.getId());
            // claim.setCode(null);
        } else {
            if (claimToSave.getCode() != null && !claimToSave.getCode().isEmpty()) {
                claim.setCode(claimToSave.getCode());
            } else {
                String code = generateCode(collector.getServicePoint().getUuid(), collector.getCode(), type);
                claim.setCode(code);
            }
        }
        if (claimToSave.getCreatedAt() != null && !claimToSave.getCreatedAt().isEmpty()) {
            claim.setCreatedAt(Utils.convertStrWithTToLocalDateTime(claimToSave.getCreatedAt()));
        } else {
            claim.setCreatedAt(LocalDateTime.now());
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
            try {
                collectionChannel = collectionChannelService.getById(claimToSave.getCollectionChannelId());
                claim.setCollectionChannel(collectionChannel);
            } catch (Exception e) {
                throw new Exception("Canal de collecte introuvable");
            }
        }
        ServicePoint servicePoint;
        if (claimToSave.getServicePointId() != null) {
            try {
                servicePoint = servicePointServiceImpl.getById(claimToSave.getServicePointId());
                claim.setServicePoint(servicePoint);
            } catch (Exception e) {
                throw new Exception("Point Service introuvable");
            }
        }

        Product product;
        if (claimToSave.getProductId() != null) {
            try {
                product = productServiceImpl.getById(claimToSave.getProductId());
                claim.setProduct(product);
            } catch (Exception e) {
                throw new Exception("Product introuvable");
            }
        }

        Objet objet;
        if (claimToSave.getObjetId() != null) {
            try {
                objet = objetServcieImpl.getById(claimToSave.getObjetId());
                claim.setObjet(objet);
            } catch (Exception e) {
                throw new Exception("Objet introuvable");
            }
        }

        Language language;
        if (claimToSave.getLanguageId() != null) {
            try {
                language = languageServiceImpl.getById(claimToSave.getLanguageId());
                claim.setLanguage(language);
            } catch (Exception e) {
                throw new Exception("Langage introuvable");
            }
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

        if (claimToSave.getCrew() != null) {
            claim.setCrew(claimToSave.getCrew());
        }

        if (claimToSave.getFolderCode() != null) {
            claim.setFolderCode(claimToSave.getFolderCode());
        }

        if (claimToSave.getContent() != null) {
            claim.setContent(claimToSave.getContent());
        }

        claim.setCollector(collector);
        claim.setStatus(ClaimStatus.TEMP_SAVED);

        if (claimToSave.getReceiptDateTime() != null && !claimToSave.getReceiptDateTime().isEmpty()) {
            claim.setReceiptDateTime(Utils.convertStrToLocalDateTime(claimToSave.getReceiptDateTime()));
        }
        claim.setOnlineUploadDateTime(claimToSave.getOnlineUploadDateTime());
        claim = repository.save(claim);

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
            claim.setMedias(medias);
            claim = repository.save(claim);
        }
    }

    @Override
    public void saveDenunOffline(SaveDenunRequest claimPart, ClaimType type) throws Exception {
        DenunRequest claimToSave = claimPart.getClaimRequest();
        User collector;
        try {
            collector = authServiceImpl.getById(claimToSave.getCollectorId());
        } catch (Exception e) {
            throw new Exception("Collector " + claimToSave.getCollectorId() + " of the denun not found");
        }

        
        Claim claim = Claim
                .builder()

                .type(type)
                // .codeClient(codeClient)
                .content(claimToSave.getContent())
                .collector(collector).status(ClaimStatus.SAVED)
                .createdAt(LocalDateTime.now())
                .receiptDateTime(Utils.convertStrToLocalDateTime(claimToSave.getReceiptDateTime()))
                .build();
        if (claimToSave.getId() != null) {
            claim.setId(claimToSave.getId());
            claim.setCode(claimToSave.getCode());
            // Only TEMP_SAVED can be saved
            Claim oldClaim = repository.findById(claimToSave.getId())
                    .orElseThrow(() -> new ClaimException("Claim with this code doesn't exist"));
                    // claim.setCodeClient(oldClaim.getCodeClient());
            

        } else {
            if (claimToSave.getCode() == null || claimToSave.getCode() == "") {
                String code = generateCode(collector.getServicePoint().getUuid(), collector.getCode(), type);
                claim.setCode(code);
            } else {
                claim.setCode(claimToSave.getCode());
            }
        }

        CollectionChannel collectionChannel;
        if (claimToSave.getCollectionChannelId() != null) {
            try {
                collectionChannel = collectionChannelService.getById(claimToSave.getCollectionChannelId());
                claim.setCollectionChannel(collectionChannel);
            } catch (Exception e) {
                throw new Exception("Canal de collecte introuvable");
            }
        }
        ServicePoint servicePoint;
        if (claimToSave.getServicePointId() != null) {
            try {
                servicePoint = servicePointServiceImpl.getById(claimToSave.getServicePointId());
                claim.setServicePoint(servicePoint);
            } catch (Exception e) {
                throw new Exception("Point Service introuvable");
            }
        }

        Product product;
        if (claimToSave.getProductId() != null) {
            try {
                product = productServiceImpl.getById(claimToSave.getProductId());
                claim.setProduct(product);
            } catch (Exception e) {
                throw new Exception("Product introuvable");
            }
        }

        Objet objet;
        if (claimToSave.getObjetId() != null) {
            try {
                objet = objetServcieImpl.getById(claimToSave.getObjetId());
                claim.setObjet(objet);
            } catch (Exception e) {
                throw new Exception("Objet introuvable");
            }
        }

        Language language;
        if (claimToSave.getLanguageId() != null) {
            try {
                language = languageServiceImpl.getById(claimToSave.getLanguageId());
                claim.setLanguage(language);
            } catch (Exception e) {
                throw new Exception("Langage introuvable");
            }
        }
        if (claimToSave.getCreatedAt() != null && !claimToSave.getCreatedAt().isEmpty()) {
            claim.setCreatedAt(Utils.convertStrWithTToLocalDateTime(claimToSave.getCreatedAt()));
        } else {
            claim.setCreatedAt(LocalDateTime.now());
        }
        claim.setOnlineUploadDateTime(claimToSave.getOnlineUploadDateTime());
        claim = repository.save(claim);

        if (claimPart.getFiles() != null && claimPart.getFiles().length != 0) {
            List<Media> medias = mediaServiceImpl.store(claimPart.getFiles(), claim);
            claim.setUpdatedAt(LocalDateTime.now());
            claim.setMedias(medias);
            claim = repository.save(claim);
        }
    }

    @Override
    public void saveTempDenunOffline(SaveDenunRequest claimPart, ClaimType type) throws Exception {
        DenunRequest claimToSave = claimPart.getClaimRequest();
        Claim claim = Claim
                .builder().build();
        User collector;
        try {
            collector = authServiceImpl.getById(claimToSave.getCollectorId());
            claim.setCollector(collector);
        } catch (Exception e) {
            throw new Exception("Collector" + claimToSave.getCollectorId() + " of the temp denun not found");
        }
        if (claimToSave.getId() != null) {
            Claim oldClaim = repository.findById(claimToSave.getId())
                    .orElseThrow(() -> new Exception("Aucune réclamation ne porte ce code"));
            claim = oldClaim;
        } else {
            if (claimToSave.getCode() != null && !claimToSave.getCode().isEmpty()) {
                claim.setCode(claimToSave.getCode());
            } else {
                String code = generateCode(collector.getServicePoint().getUuid(), collector.getCode(), type);
                claim.setCode(code);
            }
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
            try {
                collectionChannel = collectionChannelService.getById(claimToSave.getCollectionChannelId());
                claim.setCollectionChannel(collectionChannel);
            } catch (Exception e) {
                throw new Exception("Collection channelle choosed not found");
            }
        }

        if (claimToSave.getCreatedAt() != null && !claimToSave.getCreatedAt().isEmpty()) {
            claim.setCreatedAt(Utils.convertStrWithTToLocalDateTime(claimToSave.getCreatedAt()));
        } else {
            claim.setCreatedAt(LocalDateTime.now());
        }
        ServicePoint servicePoint;
        if (claimToSave.getServicePointId() != null) {
            try {
                servicePoint = servicePointServiceImpl.getById(claimToSave.getServicePointId());
                claim.setServicePoint(servicePoint);
            } catch (Exception e) {
                throw new Exception("Service Point choosed not found");
            }
        }

        Product product;
        if (claimToSave.getProductId() != null) {
            try {
                product = productServiceImpl.getById(claimToSave.getProductId());
                claim.setProduct(product);
            } catch (Exception e) {
                throw new Exception("Product choosed not found");
            }
        }

        Objet objet;
        // System.out.println("objet id");
        // System.out.println(claimToSave.getObjetId());
        if (claimToSave.getObjetId() != null) {
            try {
                objet = objetServcieImpl.getById(claimToSave.getObjetId());
                claim.setObjet(objet);
            } catch (Exception e) {
                throw new Exception("Objet choosed not found");
            }
        }

        Language language;
        if (claimToSave.getLanguageId() != null) {
            try {
                language = languageServiceImpl.getById(claimToSave.getLanguageId());
                claim.setLanguage(language);
            } catch (Exception e) {
                throw new Exception("Objet choosed not found");
            }
        }

        if (claimToSave.getContent() != null) {
            claim.setContent(claimToSave.getContent());
        }

        claim.setCollector(collector);
        claim.setStatus(ClaimStatus.TEMP_SAVED);
        // claim.setCreatedAt(LocalDateTime.now());
        if (claimToSave.getReceiptDateTime() != null && !claimToSave.getReceiptDateTime().isEmpty()) {
            claim.setReceiptDateTime(Utils.convertStrToLocalDateTime(claimToSave.getReceiptDateTime()));
        }
        claim.setOnlineUploadDateTime(claimToSave.getOnlineUploadDateTime());
        claim = repository.save(claim);

        if (claimPart.getFiles() != null && claimPart.getFiles().length != 0) {
            // System.out.println("test");
            // System.out.println(claim.getCode());
            List<Media> medias = mediaServiceImpl.store(claimPart.getFiles(), claim);
            claim.setUpdatedAt(LocalDateTime.now());
            claim.setMedias(medias);
            claim = repository.save(claim);
        }
    }

    @Override
    public Long countClaims() {
        return repository.count();
    }

    @Override
    public Claim transmitClaim(Claim claim) throws Exception {
        UserDetails collectorDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User connectedUser = User.builder().build();
        connectedUser = authServiceImpl.getByEmail(collectorDetails.getUsername());  // Assure-toi que ce service retourne l'utilisateur complet
         // Récupérer le point de service de l'utilisateur connecté
          
         ServicePoint servicePoint = connectedUser.getServicePoint();
        if (claim.getStatus().equals(ClaimStatus.SAVED)) {
            claim.setTransmitted(true);
            claim.setUpdatedAt(LocalDateTime.now());
            //a qui transmettre
        
            User transmittedTo = null;
            List<User> pilote = authServiceImpl.getUsersByRoles(Arrays.asList(Role.PILOTE));

            // Étape 1 : Vérifier si l'utilisateur est un RA
            if (connectedUser.isRa()) {

                // Étape 2 : Vérifier si direction_id du point de service est null
                if (servicePoint.getDirection_id() == null) {
                    // Si direction_id est null, trouver le premier PILOTE dans le point de service actuel
                    
                    if (transmittedTo == null) {
                        transmittedTo = pilote.get(0);
                    
                        // Si aucun PILOTE n'a été trouvé, lever une exception
                        if (transmittedTo == null) {
                            throw new Exception("Aucun RA ni PILOTE trouvé pour le point de service parent.");
                        }
                    }
                } else {
                    // Étape 3 : Si direction_id n'est pas null, trouver le point de service correspondant à direction_id
                    ServicePoint parentServicePoint = spRepository.findById(servicePoint.getDirection_id())
                            .orElseThrow(() -> new Exception("Point de service parent non trouvé."));

                    // Étape 4 : Trouver les utilisateurs du point de service parent
                    transmittedTo = authServiceImpl.findRaByServicePoint(parentServicePoint.getId());
          

                    // Étape 6 : Si aucun RA n'a été trouvé, chercher le PILOTE dans ce point de service
                    if (transmittedTo == null) {
                        transmittedTo = pilote.get(0);
                    
                        // Si aucun PILOTE n'a été trouvé, lever une exception
                        if (transmittedTo == null) {
                            throw new Exception("Aucun RA ni PILOTE trouvé pour le point de service parent.");
                        }
                    }
                }
            } else {
               
              // Étape 1 : Vérifier s'il existe un RA dans le point de service
          
                transmittedTo = authServiceImpl.findRaByServicePoint(servicePoint.getId());
          
              
                // Étape 3 : Si aucun RA n'a été trouvé, chercher le PILOTE 
                if (transmittedTo == null) {
                    transmittedTo = pilote.get(0);
                
                    // Si aucun PILOTE n'a été trouvé, lever une exception
                    if (transmittedTo == null) {
                        throw new Exception("Aucun RA ni PILOTE trouvé pour le point de service parent.");
                    }
                }
            }

            // Transmettre la réclamation à l'utilisateur trouvé
            claim.setTransmittedTo(transmittedTo);

            // Sauvegarder la réclamation mise à jour
            claim = repository.save(claim);
            final Claim finalClaim = claim;
            final User finalTransmittedTo = transmittedTo;

            // TODO send mail
            // Initialisation de la liste
            List<User> destis = new ArrayList<>();

            // Ajouter transmittedTo à la liste destis
            if (transmittedTo != null) {
                destis.add(transmittedTo);
            } else {
                throw new Exception("Le destinataire (transmittedTo) est null, impossible de l'ajouter à la liste.");
            }

            // if (!pilote.isEmpty()) {                           
                // Envoi de mail en parallèle
                CompletableFuture.runAsync(() -> {
                    try {
                        Double apercuContent = finalClaim.getContent().length() * 0.5;
                        String message = "" +
                                "Bonjour " + finalTransmittedTo.getFirstandlastname() + ",\n\n" +
                                "Nous vous informons qu'un utilisateur a transmis la gestion d'une réclamation/dénonciation à votre attention, car il est dans l'incapacité de la traiter.\n"
                                +
                                "* Code de la Réclamation : " + finalClaim.getCodeClient() + "\n" +
                                "* Aperçu de la réclamation : " + finalClaim.getContent().substring(0, apercuContent.intValue())
                                + "...\n\n" +
                                "* Date d'enregistrement : " + Utils.convertLocalDateTimeToStr(finalClaim.getReceiptDateTime())
                                + "\n" +
                                "Veuillez prendre les mesures nécessaires pour permettre le traitement de cette réclamation dans les meilleurs délais.\n\n"
                                +
                                "Cordialement,\n" ;
                                // "Transmis par : " + claim.getCollector().getFirstandlastname() + "\n" +
                                // "Poste : " + claim.getCollector().getPoste().getLibelle();
                        Utils.sendmail(destis, "TRANSMISSION DE TRAITEMENT", message, null, "", settingServiceImpl);
                                                        
                        Log successLog = Log.builder()
                            .libelle("Mail notification création session")
                            .content("Success mail notification création session")
                            .createdAt(LocalDateTime.now())
                            .type(LogType.INFO)
                            .userId(0L)
                            .userIpAddress("")
                            .target(LogTarget.APP)
                            .build();

                        logServiceImpl.saveLog(successLog);                           
                    } catch (Exception e) {                        
                        if (e != null) {
                            Log log2 = Log
                                    .builder()
                                    .libelle("Echec mail notification création session")
                                    .content(e.getMessage())
                                    .createdAt(LocalDateTime.now())
                                    .type(LogType.ERROR)
                                    .userId(0L)
                                    .userIpAddress("")
                                    .target(LogTarget.APP)
                                    .build();

                            logServiceImpl.saveLog(log2);
                        }
                    }

                    try {
                        String message = "La réclamation " + finalClaim.getCode()
                                + " vous a été transmis pour prise en charge. Merci de la prendre en charge.";
                        Utils.sendSms(destis, message, settingServiceImpl);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

                return claim;
            // } else {
            //     throw new Exception("Plateforme mal configurée. Pilote introuvable");
            // }

        } else {
            throw new Exception("Le statut de la réclamation est invalide");
        }
    }

    /**
     * Get list of claim where user is in guest list of a chat session
     * 
     * @param excludeClaims contains all claims that we don't want repetition. And
     *                      we add to it all claim where the user is in guest List
     * @param user          is the user who need to be in guest list of the chat
     *                      session
     */
    @Override
    public List<Claim> getClaimsWhenUserIsInGuestChat(User user, List<Claim> excludeClaims) {
        List<Chat> chats = chatRepository.findByGuestsIn(Arrays.asList(user));
        List<Claim> claims = excludeClaims;
        for (Chat chat : chats) {
            if (!excludeClaims.contains(chat.getClaim()) && Arrays
                    .asList(ClaimStatus.SAVED, ClaimStatus.AFFECTED, ClaimStatus.TO_APPROUVED, ClaimStatus.DESAPPROUVED)
                    .contains(chat.getClaim().getStatus())) {
                claims.add(chat.getClaim());
            }

        }
        return claims;
    }

    @Override
    public List<Claim> getClaimsWhenUserIsInGuestChatSuper(User user, List<Claim> excludeClaims) {
        List<Chat> chats = chatRepository.findByGuestsIn(Arrays.asList(user));
        List<Claim> claims = excludeClaims;
        for (Chat chat : chats) {
            if (!excludeClaims.contains(chat.getClaim()) && Arrays
                    .asList(ClaimStatus.SAVED, ClaimStatus.AFFECTED, ClaimStatus.TO_APPROUVED, ClaimStatus.DESAPPROUVED,
                            ClaimStatus.UNSATISFIED, ClaimStatus.PARTIAL_SATISFIED, ClaimStatus.CLASSED)
                    .contains(chat.getClaim().getStatus())) {
                claims.add(chat.getClaim());
            }

        }
        return claims;
    }

    @Override
    public Claim saveBotClaim(SaveRequest claimPart, ClaimType type) throws Exception {
        ClaimRequest claimToSave = claimPart.getClaimRequest();
        // User collector;
        // try {
        //     collector = authServiceImpl.getById(claimToSave.getCollectorId());
        // } catch (Exception e) {
        //     throw new Exception("Collector " + claimToSave.getCollectorId() + " of the claim not found");
        // }

        String message = "" +
        "Cher(e) utilisteur, " +
        "une nouvelle réclamation collectée avec GPR BOT. Cette réclamation  nécessite votre attention."
        + "\n\n";
       
        Claim claim = Claim
                .builder()
                .clientFirstAndLastName(claimToSave.getClientFirstAndLastName())
                .code(claimToSave.getCodeClient())
                // .gender(Gender.valueOf(claimToSave.getGender()))
                .type(ClaimType.CLAIM)
                // .address(claimToSave.getAddress())
                .tel(claimToSave.getPhone())
                // .crew(claimToSave.getCrew())
                // .folderCode(claimToSave.getFolderCode())
                .content(claimToSave.getContent())
                .collector(null)
                .status(ClaimStatus.TEMP_SAVED)
                // .createdAt(LocalDateTime.now())
                // .receiptDateTime(LocalDateTime.now())
                .build();

            

                List<Claim> oldClaims = repository.findByTelAndTypeAndStatus(claimToSave.getPhone(), ClaimType.CLAIM, ClaimStatus.TEMP_SAVED);
                if(!oldClaims.isEmpty()){
                  
                    for (Claim oldClaim : oldClaims) {
                        
                        if(oldClaim.getCreatedAt().isBefore(LocalDateTime.now()) ){
                            Long durationUntil = LocalDateTime.now().until(oldClaim.getCreatedAt(), ChronoUnit.MINUTES);
                            if(durationUntil <= 60){
                                //we are in a session conversation 
                                claim.setCreatedAt(oldClaim.getCreatedAt());
                                claim.setReceiptDateTime(oldClaim.getReceiptDateTime());
                                claim.setGender(oldClaim.getGender());
                                claim.setAddress(oldClaim.getAddress());
                                claim.setContent(oldClaim.getContent() + "\n"+claim.getContent());
                                claim.setCollector(oldClaim.getCollector());
                                claim.setCollectionChannel(oldClaim.getCollectionChannel());
                                claim.setServicePoint(oldClaim.getServicePoint());
                                claim.setProduct(oldClaim.getProduct());
                                claim.setObjet(oldClaim.getObjet());
                                claim.setLanguage(oldClaim.getLanguage());
                                claim.setInChatSession(true);
                                claim.setId(oldClaim.getId());
                                claim.setCode(oldClaim.getCode());
                        
                            } else {
                                //Create a new claim
                                claim.setCreatedAt(LocalDateTime.now());
                                claim.setReceiptDateTime(LocalDateTime.now());
                                claim.setInChatSession(false);
                            }
                            
                        } 
                    }
                } else {
                    claim.setCreatedAt(LocalDateTime.now());
                    claim.setReceiptDateTime(LocalDateTime.now());
                }


        // if (claimToSave.getStatus() != null) {
        //     claim.setStatus(claimToSave.getStatus());
        // } else {
        //     claim.setStatus(ClaimStatus.SAVED);
        // }

        //TODO: FInd id for the claim based on the claim
        


        // if (claimToSave.getId() != null) {
        //     claim.setId(claimToSave.getId());
        //     claim.setCode(claimToSave.getCode());
        //     // Only TEMP_SAVED can be saved
        //     Claim oldClaim = repository.findById(claimToSave.getId())
        //             .orElseThrow(() -> new ClaimException("Claim with this code doesn't exist"));

        //     // if (oldClaim.getStatus() != ClaimStatus.TEMP_SAVED) {
        //     // throw new ClaimException(
        //     // "Invalid operation! this claim is not temporarly saved, you can't change it
        //     // again");
        //     // }

        // } else {
        //     if (claimToSave.getCode() == null || claimToSave.getCode() == "") {
        //         String code = generateCode(collector.getServicePoint().getUuid(), collector.getCode(), type);
        //         claim.setCode(code);
        //     } else {
        //         claim.setCode(claimToSave.getCode());
        //     }
        // }
        // if(claimToSave.getCode() == null || claimToSave.getCode()== "") {
        // String code = generateCode(collector.getServicePoint().getUuid(),
        // collector.getCode(), type);
        // claim.setCode(code);
        // }
       
        claim = repository.save(claim);
        Log log = Log
                .builder()
                .content("code: " + claim.getCode())
                .createdAt(LocalDateTime.now())
                .type(LogType.INFO)
                .userId(claim.getCollector().getId())
                .userIpAddress(claimPart.getRemoteAddress())
                .build();
        if (type.equals(ClaimType.CLAIM)) {
            log.setLibelle("Nouvelle réclamation bot");
            log.setTarget(LogTarget.CLAIM);
        } else {
            log.setLibelle("Nouvelle dénonciation bot");
            log.setTarget(LogTarget.DENUNCIACION);
        }

        logServiceImpl.saveLog(log);

        // if (claimPart.getFiles() != null && claimPart.getFiles().length != 0) {
        //     List<Media> medias = mediaServiceImpl.store(claimPart.getFiles(), claim);
        //     claim.setUpdatedAt(LocalDateTime.now());
        //     // claim.setMedias(medias);
        // }
        // if (claimPart.getAudios() != null && claimPart.getAudios().length != 0) {
        //     List<ClaimAudio> audios = claimAudioServiceImpl.store(claimPart.getAudios(), claim);
        //     claim.setUpdatedAt(LocalDateTime.now());
        //     // for (ClaimAudio audio : audios) {
        //     // audio.setClaim(null);
        //     // }
        //     // claim.setAudios(audios);
        // }

        claim = repository.save(claim);
        final Claim finalClaim = claim;
        
        List<User> usersToContact = authServiceImpl.getEmailReceiversForNotif(claim.getServicePoint());

        Double apercuContent = claim.getContent().length() * 0.5;
                                          
        // Envoi de mail en parallèle
        CompletableFuture.runAsync(() -> {
            try {
                Utils.sendmail(usersToContact, " Notification d'enregistrement de réclamation", message, null,
                        " ", settingServiceImpl);
                                        
                Log successLog = Log.builder()
                    .libelle("Mail notification enregistrement réclamation")
                    .content("Success mail notification enregistrement réclamation")
                    .createdAt(LocalDateTime.now())
                    .type(LogType.INFO)
                    .userId(0L)
                    .userIpAddress("")
                    .target(LogTarget.APP)
                    .build();

                logServiceImpl.saveLog(successLog);                           
            } catch (Exception e) {
                if (e != null) {
                    Log log2 = Log
                            .builder()
                            .libelle("Echec mail notification enregistrement réclamation")
                            .content(e.getMessage())
                            .createdAt(LocalDateTime.now()) 
                            .type(LogType.ERROR)
                            .userId(0L)
                            .userIpAddress(claimPart.getRemoteAddress())
                            .target(LogTarget.APP)
                            .build();
    
                    logServiceImpl.saveLog(log2);
                }
            }

            try {
                Utils.sendSms(usersToContact,
                        "Nouvelle réclamation enregistrée de niveau de gravité "
                                + finalClaim.getObjet().getRisqueLevel().name(), settingServiceImpl);
            } catch (Exception e) {
                Log log2 = Log
                        .builder()
                        .libelle("Echec sms notification")
                        .content(e.getMessage())
                        .createdAt(LocalDateTime.now())
                        .type(LogType.ERROR)
                        .userId(0L)
                        .userIpAddress(claimPart.getRemoteAddress())
                        .target(LogTarget.APP)
                        .build();
    
                logServiceImpl.saveLog(log2);
            }
        });    


        return claim;

    }

   
    @Override
    public List<Claim> getAllByStatusIn(List<ClaimStatus> status) {
        return repository.findByStatusIn(status);
    }
    
    @Transactional
    @Override
    public void convertClaimToDenunciation(Long id) throws NotFoundException {
        Claim claim = repository.findById(id).orElseThrow(() -> new NotFoundException());

        UserDetails collectorDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User connectedUser;
        try {
            connectedUser = authServiceImpl.getByEmail(collectorDetails.getUsername());
        } catch (Exception e) {
            throw new RuntimeException("Impossible de récupérer l'utilisateur connecté", e);
        }

        claim.setType(ClaimType.DENUNCIACION);

        claim.setTel(null);
        claim.setGender(null);
        claim.setClientFirstAndLastName(null);
        claim.setAddress(null);
        claim.setCrew(null);
        claim.setFolderCode(null);
        claim.setLanguage(null);

        claim.setConvertedAt(LocalDateTime.now());
        claim.setConvertedBy(connectedUser);

        // Mettre à jour le code et codeClient en changeant uniquement le préfixe
        if (claim.getCodeClient() != null && claim.getCodeClient().startsWith("REC-")) {
            claim.setCodeClient("DEN-" + claim.getCodeClient().substring(4));
        }

        if (claim.getCode() != null && claim.getCode().startsWith("rec")) {
            claim.setCode("den" + claim.getCode().substring(3));
        }
        
        // Modifier aussi le type de tous les ExtraContent liés
        if (claim.getExtraContents() != null) {
            for (ExtraContent extra : claim.getExtraContents()) {
                extra.setType(ClaimType.DENUNCIACION);
                extraContentRepository.save(extra);
            }
        }

        repository.save(claim);
    }
    
    @Transactional
    @Override
    public void convertClaimToSuggestion(Long idClaim, Long idSuggestion) throws NotFoundException {
        Claim claim = repository.findById(idClaim).orElseThrow(() -> new NotFoundException());
        Suggestion suggestion = suggestionRepository.findById(idSuggestion).orElseThrow(() -> new NotFoundException());

        UserDetails collectorDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User connectedUser;
        try {
            connectedUser = authServiceImpl.getByEmail(collectorDetails.getUsername());
        } catch (Exception e) {
            throw new RuntimeException("Impossible de récupérer l'utilisateur connecté", e);
        }

        suggestion.setCreatedAt(claim.getCreatedAt());
        suggestion.setConvertedAt(LocalDateTime.now());
        suggestion.setConvertedBy(connectedUser);
        
        suggestionRepository.save(suggestion);  

        if (claim.getExtraContents() != null) {
            for (ExtraContent extra : claim.getExtraContents()) {
                extra.setClaim(null);
                extra.setSuggestion(suggestion);
                extra.setType(ClaimType.SUGGESTION);
                extraContentRepository.save(extra);
            }
        }

        List<Media> medias = mediaRepository.findByClaimId(claim.getId());
        for (Media media : medias) {
            media.setClaim(null);
            media.setSuggestion(suggestion);
            mediaRepository.save(media);
        }

        List<ClaimAudio> audios = claimAudioRepository.findByClaimId(claim.getId());
        for (ClaimAudio audio : audios) {
            audio.setClaim(null);
            audio.setSuggestion(suggestion);
            claimAudioRepository.save(audio);
        }

        repository.delete(claim);
    }
    
    @Transactional
    @Override
    public void deleteById(Long id) throws NotFoundException {
        Claim claim = repository.findById(id).orElseThrow(() -> new NotFoundException());
        // Supprimer les éléments liés à la réclamation
        mediaRepository.deleteByClaimId(id);
        claimAudioRepository.deleteByClaimId(id);
        extraContentRepository.deleteByClaimId(id);
        
        // Enfin, supprimer la réclamation elle-même
        repository.delete(claim);
    }

    @Transactional
    @Override
    public void deleteById_2(Long id) throws NotFoundException {
        Claim claim = repository.findById(id).orElseThrow(() -> new NotFoundException());
        
        claim.getMedias().clear();
        repository.save(claim);
        
        mediaRepository.deleteByClaimId(id); 
        claimAudioRepository.deleteByClaimId(id); 
        extraContentRepository.deleteByClaimId(id);
        
        repository.delete(claim);
    }
}
