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
import com.sicmagroup.gpr.api.claim.ClaimDraftRequest;
import com.sicmagroup.gpr.api.claim.ClaimRequest;
import com.sicmagroup.gpr.api.claim.ProposedSolutionRequest;
import com.sicmagroup.gpr.api.claim.SaveRequest;
import com.sicmagroup.gpr.api.denunciation.DenunRequest;
import com.sicmagroup.gpr.api.denunciation.SaveDenunRequest;
import com.sicmagroup.gpr.domain.dto.AlertDto;
import com.sicmagroup.gpr.domain.dto.TrashDto;
import com.sicmagroup.gpr.domain.dto.claimResponse.UserResponse;
import com.sicmagroup.gpr.domain.enumeration.ClaimEventType;
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
import com.sicmagroup.gpr.service.MailService;
import com.sicmagroup.gpr.service.auth.AuthenticationServiceImpl;
import com.sicmagroup.gpr.service.claimAudio.ClaimAudioServiceImpl;
import com.sicmagroup.gpr.service.collectionChannel.CollectionChannelServiceImpl;
import com.sicmagroup.gpr.service.existingSolution.ExistingSolutionServiceImpl;
import com.sicmagroup.gpr.service.externalRecourse.ExternalRecourseServiceImpl;
import com.sicmagroup.gpr.service.extra.ExtraContentServiceImpl;
import com.sicmagroup.gpr.service.claimEvent.ClaimEventServiceImpl;
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
import com.sicmagroup.gpr.service.wgpr.WgprWhatsappBridgeService;
import com.sicmagroup.gpr.utils.CurrentUserUtils;
import com.sicmagroup.gpr.utils.Utils;
import org.springframework.context.annotation.Lazy;

import jakarta.servlet.http.HttpServletRequest;

import com.sicmagroup.gpr.utils.CurrentUserUtils;
import com.sicmagroup.gpr.repository.ServicePointRepository;
import com.sicmagroup.gpr.repository.SuggestionRepository;

import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import java.util.Comparator;
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
    private final ClaimEventServiceImpl claimEventServiceImpl;
    private final CurrentUserUtils userAuth;
    private final SuggestionRepository suggestionRepository;
    private final MailService mailService;
    @Autowired
    private HttpServletRequest httpServletRequest;

    @Lazy
    @Autowired
    private WgprWhatsappBridgeService wgprBridgeService;

    @Override
    public List<Claim> getAll(ClaimType type) {
        return repository.findByTypeAndIsDeletedFalse(type);
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
                .email(claimToSave.getEmail())
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
        claimEventServiceImpl.log(claim.getId(), claim.getCodeClient(), type, ClaimEventType.SAVED,
                collector.getFirstandlastname(), collector.getEmail(), null);
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

        // Accusé de réception WhatsApp — asynchrone, non bloquant
        final String clientTel = claim.getTel();
        final String codeClient = claim.getCodeClient();
        final String clientName = claim.getClientFirstAndLastName();
        if (clientTel != null && !clientTel.isBlank()) {
            CompletableFuture.runAsync(() -> {
                try { wgprBridgeService.sendAcknowledgment(clientTel, codeClient, clientName, "reclamation"); }
                catch (Exception e) { System.err.println("[WhatGPR] Accusé réclamation non envoyé → " + clientTel + " : " + e.getMessage()); }
            });
        }

        List<User> usersToContact = authServiceImpl.getEmailReceiversForNotif(claim.getServicePoint());
        List<User> pilote = authServiceImpl.getUsersByRoles(Arrays.asList(Role.PILOTE));
        usersToContact.addAll(pilote);

        String message = """
        <html>
        <body style="font-family: Arial, sans-serif; background-color: #f7f7f7; padding: 20px;">
            <div style="max-width: 600px; margin: auto; background: white; border-radius: 8px; 
                        box-shadow: 0 2px 8px rgba(0,0,0,0.1); padding: 20px;">

            <h2 style="color: #004080; text-align: center;">Nouvelle réclamation enregistrée - GPR</h2>

            <p>Bonjour,</p>

            <p>
                Une nouvelle réclamation a été enregistrée avec succès dans votre système. 
                Vous recevez ce mail en tant qu'utilisateur habilité à recevoir les notifications de nouvelles réclamations.
            </p>

            <div style="margin-top: 20px; background-color: #f0f8ff; border-left: 4px solid #004080; 
                        padding: 10px 15px;">
                <p style="margin: 0;"><strong>Détails de la réclamation :</strong></p>
                <p style="margin: 5px 0;">📌 <strong>Code de réclamation :</strong> %s</p>
                <p style="margin: 5px 0;">📅 <strong>Date de réception :</strong> %s</p>
                <p style="margin: 5px 0;">📝 <strong>Objet :</strong> %s</p>
            </div>

            <p style="margin-top: 20px;">
                Nous vous encourageons à examiner cette réclamation dès que possible et à prendre les mesures nécessaires pour la traiter. 
                Votre expertise est essentielle pour assurer une résolution rapide et satisfaisante pour les clients.
            </p>

            <p style="margin-top: 30px;">Cordialement,<br>L’équipe GPR</p>

            <p style="font-size: 12px; color: gray; text-align: center; margin-top: 30px;">
                Cet email a été généré automatiquement. Merci de ne pas y répondre.
            </p>

            </div>
        </body>
        </html>
        """.formatted(claim.getCodeClient(),Utils.convertLocalDateTimeToStr(claim.getReceiptDateTime()),claim.getObjet().getLibelle());

        // Envoi de mail en parallèle
       
            try {
                mailService.sendMail(usersToContact, "Nouvelle réclamation enregistrée - GPR", message, null);
                for (User u : usersToContact) {
                    claimEventServiceImpl.log(claim.getId(), claim.getCodeClient(), type, ClaimEventType.MAIL_SENT_AGENT,
                            collector.getFirstandlastname(), collector.getEmail(), u.getFirstandlastname() + "|" + u.getEmail());
                }
                Log successLog = Log.builder()
                    .libelle("Mail notification d'enregistrement de réclamation")
                    .content("Success mail notification réclamation enregistrée")
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
      

        try {
            Utils.sendSms(usersToContact,
                    "Nouvelle réclamation enregistrée de niveau de gravité "
                            + claim.getObjet().getRisqueLevel().name(), settingServiceImpl);
            for (User u : usersToContact) {
                claimEventServiceImpl.log(claim.getId(), claim.getCodeClient(), type, ClaimEventType.SMS_SENT_AGENT,
                        collector.getFirstandlastname(), collector.getEmail(), u.getFirstandlastname() + "|" + u.getTel());
            }
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
        if (claimToSave.getEmail() != null) {
            claim.setEmail(claimToSave.getEmail());
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
                .userIpAddress(Utils.getClientIpAddress(httpServletRequest))
                .target(claim.getType().equals(ClaimType.CLAIM) ? LogTarget.CLAIM : LogTarget.DENUNCIACION)
                .build();
        logServiceImpl.saveLog(log);
        String type = "Réclamation";
        if (claim.getType().equals(ClaimType.DENUNCIACION)) {
            type = "Dénonciation";
        }

        final String finalType = type;
      
        // Utiliser le message personnalisé s'il existe, sinon texte standard
        String mainMessage = (affectTreatmentRequest.getMessage() != null && !affectTreatmentRequest.getMessage().isEmpty())
                ? affectTreatmentRequest.getMessage()
                : "Le traitement d'une nouvelle "+finalType+" vous a été affecté(e). Cette "+finalType+" nécessite votre attention et votre expertise pour garantir une résolution rapide et satisfaisante.";

        String messageHtml = """
        <html>
        <body style="font-family: Arial, sans-serif; background-color: #f7f7f7; padding: 20px;">
            <div style="max-width: 600px; margin: auto; background: white; border-radius: 8px; 
                        box-shadow: 0 2px 8px rgba(0,0,0,0.1); padding: 20px;">

            <h2 style="color: #004080; text-align: center;">Nouvelle %s affectée - GPR</h2>

            <p>Bonjour <strong>%s</strong>,</p>

            <p>%s</p>

            <div style="margin-top: 20px; background-color: #f0f8ff; border-left: 4px solid #004080; 
                        padding: 10px 15px;">
                <p style="margin: 0;"><strong>Détails de la %s :</strong></p>
                <p style="margin: 5px 0;">📌 <strong>Code de %s :</strong> %s</p>
                <p style="margin: 5px 0;">📅 <strong>Date de réception :</strong> %s</p>
                <p style="margin: 5px 0;">⏱ <strong>Délai de traitement :</strong> %s jours</p>
                <p style="margin: 5px 0;">📝 <strong>Objet :</strong> %s</p>
            </div>

            <p style="margin-top: 20px;">
                Veuillez prendre les mesures nécessaires pour examiner et traiter cette %s dans les plus brefs délais.
            </p>

            <p style="margin-top: 30px;">Cordialement,<br>L’équipe GPR</p>

            <p style="font-size: 12px; color: gray; text-align: center; margin-top: 30px;">
                Cet email a été généré automatiquement. Merci de ne pas y répondre.
            </p>

            </div>
        </body>
        </html>
        """.formatted(finalType,
            affectedTo.getFirstandlastname(),
            mainMessage,
            finalType,
            finalType,
            claim.getCodeClient(),
            Utils.convertLocalDateTimeToStr(claim.getReceiptDateTime()),
            affectTreatmentRequest.getDelai(),
            claim.getObjet().getLibelle(),
            finalType
        );

        historiqueAffectationServiceImpl.storeHistorique(affectTreatmentRequest);
        claimEventServiceImpl.log(claim.getId(), claim.getCodeClient(), claim.getType(), ClaimEventType.AFFECTED,
                affectedBy.getFirstandlastname(), affectedBy.getEmail(), affectedTo.getFirstandlastname());

        final String finalMessage = messageHtml;
        // Envoi de mail en parallèle
       
            try {
                mailService.sendMail(affectedTo.getEmail(), "Nouvelle "+finalType+" affectée - GPR", finalMessage, null);
                claimEventServiceImpl.log(claim.getId(), claim.getCodeClient(), claim.getType(), ClaimEventType.MAIL_SENT_AGENT,
                        affectedBy.getFirstandlastname(), affectedBy.getEmail(), affectedTo.getFirstandlastname() + "|" + affectedTo.getEmail());
                Log successLog = Log.builder()
                    .libelle("Mail notification d'affectation de "+finalType)
                    .content("Success mail notification "+finalType+" affectée")
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
      

        try {
            Utils.sendSms(Arrays.asList(affectedTo), "Le traitement de la réclamation portant le code "+claim.getCodeClient()+" de niveau de gravité "
                    + claim.getObjet().getRisqueLevel().name() + " vous a été affecté.", settingServiceImpl);
            claimEventServiceImpl.log(claim.getId(), claim.getCodeClient(), claim.getType(), ClaimEventType.SMS_SENT_AGENT,
                    affectedBy.getFirstandlastname(), affectedBy.getEmail(), affectedTo.getFirstandlastname() + "|" + affectedTo.getTel());
        } catch (Exception e) {
            Log log2 = Log
                    .builder()
                    .libelle("Echec sms notification")
                    .content(e.getMessage())
                    .createdAt(LocalDateTime.now())
                    .type(LogType.ERROR)
                    .userId(0L)
                    .userIpAddress(Utils.getClientIpAddress(httpServletRequest))
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
        solution2 = solutionServiceImpl.saveSolution(solution2);
        claim.setTreatBy(treator);
        // List<Solution> oldSolutions = claim.getSolutions();
        // oldSolutions.add(solution2);
        claim.getSolutions().add(solution2);

        // Clear draft on treatment
        claim.setDraftSolution(null);
        claim.setDraftCommentaire(null);
        claim.setDraftUserId(null);
        claim.setDraftSavedAt(null);

        claim.setUpdatedAt(LocalDateTime.now());
        System.out.println("Here 8 ");
        claim = repository.save(claim);
        claimEventServiceImpl.log(claim.getId(), claim.getCodeClient(), claim.getType(), ClaimEventType.SOLUTION_PROPOSED,
                treator.getFirstandlastname(), treator.getEmail(), null);
        System.out.println("Here 9 ");

        String type = "Réclamation";
        if (claim.getType().equals(ClaimType.DENUNCIACION)) {
            type = "Dénonciation";
        }

        final Claim finalClaim = claim;
        final String finalType = type;
        final Solution finalSolution2 = solution2;

        if (claim.hasAffectedTreatment() && treator.getCode() == claim.getTreatmentAffectedTo().getCode()) {
            // Is treator is user who receiverd affectation
        
            String message = """
            <html>
            <body style="font-family: Arial, sans-serif; background-color: #f7f7f7; padding: 20px;">
                <div style="max-width: 600px; margin: auto; background: white; border-radius: 8px;
                            box-shadow: 0 2px 8px rgba(0,0,0,0.1); padding: 20px;">

                <h2 style="color: #004080; text-align: center;">%s traitée - GPR</h2>

                <p>Bonjour <strong>%s</strong>,</p>

                <p>
                    La %s portant le code <strong>%s</strong> que vous avez affectée a été examinée par <strong>%s</strong> et une solution a été proposée.
                </p>

                <div style="margin-top: 20px; background-color: #f0f8ff; border-left: 4px solid #004080;
                            padding: 10px 15px;">
                    <p style="margin: 0;"><strong>Détails de la %s :</strong></p>
                    <p style="margin: 5px 0;">📌 <strong>Code :</strong> %s</p>
                    <p style="margin: 5px 0;">📅 <strong>Date de réception :</strong> %s</p>
                    <p style="margin: 5px 0;">📝 <strong>Objet :</strong> %s</p>
                </div>

                <div style="margin-top: 20px; background-color: #e6ffe6; border-left: 4px solid #008000;
                            padding: 10px 15px;">
                    <p style="margin: 0;"><strong>Solution proposée :</strong></p>
                    <p style="margin: 5px 0;">%s</p>
                </div>

                <p style="margin-top: 20px;">
                    Nous vous invitons à examiner cette solution puis à la communiquer au plaignant au besoin.
                </p>

                <p style="margin-top: 30px;">Cordialement,<br>L’équipe GPR</p>

                <p style="font-size: 12px; color: gray; text-align: center; margin-top: 30px;">
                    Cet email a été généré automatiquement. Merci de ne pas y répondre.
                </p>

                </div>
            </body>
            </html>
            """.formatted(finalType,claim.getTreatmentAffectedBy().getFirstandlastname(),finalType,claim.getCodeClient(),treator.getFirstandlastname(),finalType, claim.getCodeClient(),Utils.convertLocalDateTimeToStr(claim.getReceiptDateTime()),claim.getObjet().getLibelle(),finalSolution2.getContent());

            // Envoi de mail en parallèle
           
                try {
                    mailService.sendMail(finalClaim.getTreatmentAffectedBy().getEmail(), finalType+ " traitée - GPR ",message, null);
                    claimEventServiceImpl.log(finalClaim.getId(), finalClaim.getCodeClient(), finalClaim.getType(), ClaimEventType.MAIL_SENT_AGENT,
                            treator.getFirstandlastname(), treator.getEmail(), finalClaim.getTreatmentAffectedBy().getFirstandlastname() + "|" + finalClaim.getTreatmentAffectedBy().getEmail());
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
                try {
                    Utils.sendSms(Arrays.asList(finalClaim.getTreatmentAffectedBy()),
                            "La " + finalType + " portant le code " + finalClaim.getCodeClient() + " a été traitée. Une solution a été proposée.", settingServiceImpl);
                    claimEventServiceImpl.log(finalClaim.getId(), finalClaim.getCodeClient(), finalClaim.getType(), ClaimEventType.SMS_SENT_AGENT,
                            treator.getFirstandlastname(), treator.getEmail(), finalClaim.getTreatmentAffectedBy().getFirstandlastname() + "|" + finalClaim.getTreatmentAffectedBy().getTel());
                } catch (Exception ignored) {}

        } else {
            claim.setStatus(ClaimStatus.TREAT);
            solution2.setStatus(SolutionStatus.APPROVED);
            solution2.setUpdatedAt(LocalDateTime.now());

            List<User> pilote = authServiceImpl.getUsersByRoles(Arrays.asList(Role.PILOTE));
            if (pilote != null && !pilote.isEmpty()) {                            
              
                String message = """
                <html>
                <body style="font-family: Arial, sans-serif; background-color: #f7f7f7; padding: 20px;">
                    <div style="max-width: 600px; margin: auto; background: white; border-radius: 8px;
                                box-shadow: 0 2px 8px rgba(0,0,0,0.1); padding: 20px;">

                    <h2 style="color: #004080; text-align: center;">%s traitée - GPR</h2>

                    <p>Bonjour <strong>%s</strong>, Pilote de la plateforme <strong>GPR</strong></p>

                    <p>
                        La %s portant le code <strong>%s</strong> a été examinée par <strong>%s</strong> et une solution a été proposée.
                    </p>

                    <div style="margin-top: 20px; background-color: #f0f8ff; border-left: 4px solid #004080;
                                padding: 10px 15px;">
                        <p style="margin: 0;"><strong>Détails de la %s :</strong></p>
                        <p style="margin: 5px 0;">📌 <strong>Code :</strong> %s</p>
                        <p style="margin: 5px 0;">📅 <strong>Date de réception :</strong> %s</p>
                        <p style="margin: 5px 0;">📝 <strong>Objet :</strong> %s</p>
                    </div>

                    <div style="margin-top: 20px; background-color: #e6ffe6; border-left: 4px solid #008000;
                                padding: 10px 15px;">
                        <p style="margin: 0;"><strong>Solution proposée :</strong></p>
                        <p style="margin: 5px 0;">%s</p>
                    </div>

                    <p style="margin-top: 20px;">
                        Nous vous invitons à examiner cette solution puis à la communiquer au plaignant au besoin.
                    </p>

                    <p style="margin-top: 30px;">Cordialement,<br>L’équipe GPR</p>

                    <p style="font-size: 12px; color: gray; text-align: center; margin-top: 30px;">
                        Cet email a été généré automatiquement. Merci de ne pas y répondre.
                    </p>

                    </div>
                </body>
                </html>
                """.formatted(finalType, pilote.get(0).getFirstandlastname(),finalType,claim.getCodeClient(),treator.getFirstandlastname(),finalType,claim.getCodeClient(),Utils.convertLocalDateTimeToStr(claim.getReceiptDateTime()),claim.getObjet().getLibelle(),finalSolution2.getContent());

                // Envoi de mail en parallèle
               
                    try {
                        mailService.sendMail(pilote.get(0).getEmail(), "" + finalType + " traitée",message, null);
                        claimEventServiceImpl.log(finalClaim.getId(), finalClaim.getCodeClient(), finalClaim.getType(), ClaimEventType.MAIL_SENT_AGENT,
                                treator.getFirstandlastname(), treator.getEmail(), "Pilote Principal " +pilote.get(0).getFirstandlastname() + "|" + pilote.get(0).getEmail());
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
                                    .userId(treator.getId())
                                    .userIpAddress(Utils.getClientIpAddress(httpServletRequest))
                                    .target(LogTarget.APP)
                                    .build();

                            logServiceImpl.saveLog(log2);
                        }
                    }
                    try {
                        Utils.sendSms(pilote,
                                "La " + finalType + " portant le code " + finalClaim.getCodeClient() + " a été traitée. Une solution a été proposée.", settingServiceImpl);
                        claimEventServiceImpl.log(finalClaim.getId(), finalClaim.getCodeClient(), finalClaim.getType(), ClaimEventType.SMS_SENT_AGENT,
                                treator.getFirstandlastname(), treator.getEmail(), "Pilote Principal " +pilote.get(0).getFirstandlastname() + "|" + pilote.get(0).getTel());
                    } catch (Exception ignored) {}

            }

        }
        // System.out.println("Here 7 ");
         Log log = Log
            .builder()
            .libelle("Affectation de traitement")
            .content("Une solution à la " + type + " portant le code " + claim.getCode() +
                " a été proposée par " + treator.getFirstandlastname() +
                " et un mail de notification a été envoyé.")
            .createdAt(LocalDateTime.now())
            .type(LogType.INFO)
            .userId(treator.getId())
            .userIpAddress(Utils.getClientIpAddress(httpServletRequest))
            .target(claim.getType().equals(ClaimType.CLAIM) ? LogTarget.CLAIM : LogTarget.DENUNCIACION)
            .build();
        logServiceImpl.saveLog(log);
        
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

        List<User> pilotes = authServiceImpl.getUsersByRoles(Arrays.asList(Role.PILOTE));
        // Définir le texte principal en fonction du statut
        String mainMessage;
        String statusText;

        if (status == SatisfactionStatus.SATISFIED) {
            claim.setStatus(ClaimStatus.SATISFIED);
            statusText = "est satisfait(e)";
            mainMessage = "Le client ayant fait la réclamation portant le code <strong>" + claim.getCodeClient() + "</strong> est satisfait de la solution proposée par <strong>" + claim.getTreatBy().getFirstandlastname() + "</strong>.";
        } else if (status == SatisfactionStatus.UNSATISFIED) {
            claim.setStatus(ClaimStatus.UNSATISFIED);
            statusText = "n'est pas satisfait(e)";
            mainMessage = "Le client ayant fait la réclamation portant le code <strong>" + claim.getCodeClient() + "</strong> n'est pas satisfait de la solution proposée par <strong>" + claim.getTreatBy().getFirstandlastname() + "</strong>.";
        
        } else {
            claim.setStatus(ClaimStatus.PARTIAL_SATISFIED);
            statusText = "est partiellement satisfait(e)";
            mainMessage = "Le client ayant fait la réclamation portant le code <strong>" + claim.getCodeClient() + "</strong> est partiellement satisfait de la solution proposée par <strong>" + claim.getTreatBy().getFirstandlastname() + "</strong>.";
        }


        // HTML du mail
        String messageHtml = """
        <html>
        <body style="font-family: Arial, sans-serif; background-color: #f7f7f7; padding: 20px;">
            <div style="max-width: 600px; margin: auto; background: white; border-radius: 8px;
                        box-shadow: 0 2px 8px rgba(0,0,0,0.1); padding: 20px;">

            <h2 style="color: #004080; text-align: center;">Notification de satisfaction client - GPR</h2>

            <p>Bonjour %s, Pilote de la plateforme <strong>GPR</strong></p>

            <p>%s</p>

            <p style="margin-top: 20px;">
                Veuillez vous connecter à la plateforme <strong>GPR</strong> afin de prendre les mesures adéquates concernant cette réclamation si nécessaire.
            </p>

            <p style="margin-top: 30px;">Cordialement,<br>L’équipe GPR</p>

            <p style="font-size: 12px; color: gray; text-align: center; margin-top: 30px;">
                Cet email a été généré automatiquement. Merci de ne pas y répondre.
            </p>

            </div>
        </body>
        </html>
        """.formatted(pilotes.get(0).getFirstandlastname(),mainMessage);
                            
        // Envoi de mail en parallèle
       
            try {
                mailService.sendMail(pilotes.get(0).getEmail(), "Notification de satisfaction client - GPR", messageHtml, null);
                claimEventServiceImpl.log(claim.getId(), claim.getCodeClient(), claim.getType(), ClaimEventType.MAIL_SENT_AGENT,
                        measurer.getFirstandlastname(), measurer.getEmail(), "Pilote Principal " +pilotes.get(0).getFirstandlastname() + "|" + pilotes.get(0).getEmail());
                Log successLog = Log.builder()
                    .libelle("Mail notification mesure de satisfaction réclamation")
                    .content("Success mail notification mesure de satisfaction réclamation")
                    .createdAt(LocalDateTime.now())
                    .type(LogType.INFO)
                    .userId(measurer.getId())
                    .userIpAddress(Utils.getClientIpAddress(httpServletRequest))
                    .target(LogTarget.APP)
                    .build();

                logServiceImpl.saveLog(successLog);                                
            } catch (Exception e) {                    
                if (e != null) {
                    Log log2 = Log
                            .builder()
                            .libelle("Echec mail notification mesure de satisfaction réclamation")
                            .content(e.getMessage())
                            .createdAt(LocalDateTime.now())
                            .type(LogType.ERROR)
                            .userId(0L)
                            .userIpAddress(Utils.getClientIpAddress(httpServletRequest)) 
                            .target(LogTarget.APP)
                            .build();

                    logServiceImpl.saveLog(log2);
                }
            }
      
    

        claim.setUpdatedAt(LocalDateTime.now());

        claim = repository.save(claim);
        ClaimEventType satisfactionEventType = status == SatisfactionStatus.SATISFIED ? ClaimEventType.SATISFIED
                : status == SatisfactionStatus.UNSATISFIED ? ClaimEventType.UNSATISFIED
                : ClaimEventType.PARTIAL_SATISFIED;
        claimEventServiceImpl.log(claim.getId(), claim.getCodeClient(), claim.getType(), satisfactionEventType,
                measurer.getFirstandlastname(), measurer.getEmail(), null);

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
        claimEventServiceImpl.log(claim.getId(), claim.getCodeClient(), claim.getType(), ClaimEventType.REJECTED,
                unApprouver.getFirstandlastname(), unApprouver.getEmail(), commentaire);
        // TODO send mail to CGR User

        List<User> pilotes = authServiceImpl.getUsersByRoles(Arrays.asList(Role.PILOTE));

        String type = "Réclamation";
        if (claim.getType().equals(ClaimType.DENUNCIACION)) {
            type = "Dénonciation";
        }

        String messageHtml = """
        <html>
        <body style="font-family: Arial, sans-serif; background-color: #f7f7f7; padding: 20px;">
            <div style="max-width: 600px; margin: auto; background: white; border-radius: 8px;
                        box-shadow: 0 2px 8px rgba(0,0,0,0.1); padding: 20px;">

            <h2 style="color: #004080; text-align: center;">Solution désapprouvée - GPR</h2>

            <p>Bonjour <strong>%s</strong>,</p>

            <p>
                La solution que vous avez proposée pour la %s portant le code <strong>%s</strong> a été examinée et désapprouvée par <strong>%s</strong>.
            </p>

            <div style="margin-top: 20px; background-color: #f0f8ff; border-left: 4px solid #004080;
                        padding: 10px 15px;">
                <p style="margin: 0;"><strong>Détails de la %s :</strong></p>
                <p style="margin: 5px 0;">📅 <strong>Date de réception :</strong> %s</p>
                <p style="margin: 5px 0;">📝 <strong>Objet :</strong> %s</p>
                <p style="margin: 5px 0;">❌ <strong>Motif de désapprobation :</strong> %s</p>
            </div>

            <p style="margin-top: 20px;">
                Nous vous invitons à examiner attentivement le commentaire laissé et à proposer une nouvelle solution au besoin.
            </p>

            <p style="margin-top: 30px;">Cordialement,<br>L’équipe GPR</p>

            <p style="font-size: 12px; color: gray; text-align: center; margin-top: 30px;">
                Cet email a été généré automatiquement. Merci de ne pas y répondre.
            </p>

            </div>
        </body>
        </html>
        """.formatted(claim.getTreatBy().getFirstandlastname(),type,claim.getCodeClient(),unApprouver.getFirstandlastname(),type,Utils.convertLocalDateTimeToStr(claim.getReceiptDateTime()),claim.getObjet().getLibelle(),commentaire);
        final Claim finalClaim = claim;
        // Envoi de mail en parallèle
       
            try {
                mailService.sendMail(finalClaim.getTreatBy().getEmail(), "Solution désapprouvée - GPR",messageHtml, null);
                claimEventServiceImpl.log(finalClaim.getId(), finalClaim.getCodeClient(), finalClaim.getType(), ClaimEventType.MAIL_SENT_AGENT,
                        unApprouver.getFirstandlastname(), unApprouver.getEmail(), finalClaim.getTreatBy().getFirstandlastname() + "|" + finalClaim.getTreatBy().getEmail());
                    Log successLog = Log.builder()
                        .libelle("Mail notification solution désapprouvée")
                        .content("Success mail notification solution désapprouvée")
                        .createdAt(LocalDateTime.now())
                        .type(LogType.INFO)
                        .userId(unApprouver.getId())
                        .userIpAddress(Utils.getClientIpAddress(httpServletRequest))
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
                            .userIpAddress(Utils.getClientIpAddress(httpServletRequest))
                            .target(LogTarget.APP)
                            .build();
    
                    logServiceImpl.saveLog(log2);
                }    
            }
      

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
        claimEventServiceImpl.log(claim.getId(), claim.getCodeClient(), claim.getType(), ClaimEventType.APPROVED,
                approuver.getFirstandlastname(), approuver.getEmail(), null);
        final Claim finalClaim = claim;
        String type = "Réclamation";
        if (claim.getType().equals(ClaimType.DENUNCIACION)) {
            type = "Dénonciation";
        }

        String messageHtml = """
        <html>
        <body style="font-family: Arial, sans-serif; background-color: #f7f7f7; padding: 20px;">
            <div style="max-width: 600px; margin: auto; background: white; border-radius: 8px;
                        box-shadow: 0 2px 8px rgba(0,0,0,0.1); padding: 20px;">

            <h2 style="color: #004080; text-align: center;">Solution approuvée - GPR</h2>

            <p>Bonjour <strong>%s</strong>,</p>

            <p>
                La solution que vous avez proposée pour la %s portant le code <strong>%s</strong> a été examinée et approuvée par <strong>%s</strong>.
            </p>

            <div style="margin-top: 20px; background-color: #f0f8ff; border-left: 4px solid #004080;
                        padding: 10px 15px;">
                <p style="margin: 0;"><strong>Détails de la %s :</strong></p>
                <p style="margin: 5px 0;">📅 <strong>Date de réception :</strong> %s</p>
                <p style="margin: 5px 0;">📝 <strong>Objet :</strong> %s</p>
            </div>

            <p style="margin-top: 20px;">
                Félicitations ! Votre solution a été validée et peut être mise en œuvre pour finaliser le traitement de cette réclamation.
            </p>

            <p style="margin-top: 30px;">Cordialement,<br>L’équipe GPR</p>

            <p style="font-size: 12px; color: gray; text-align: center; margin-top: 30px;">
                Cet email a été généré automatiquement. Merci de ne pas y répondre.
            </p>

            </div>
        </body>
        </html>
        """.formatted(finalClaim.getTreatmentAffectedTo().getFirstandlastname(),type,finalClaim.getCodeClient(),approuver.getFirstandlastname(),type,Utils.convertLocalDateTimeToStr(finalClaim.getReceiptDateTime()),finalClaim.getObjet().getLibelle());
                            
        // Envoi de mail en parallèle
       
            try {
                mailService.sendMail(finalClaim.getTreatmentAffectedTo().getEmail(), "Solution approuvée - GPR",messageHtml, null);
                claimEventServiceImpl.log(finalClaim.getId(), finalClaim.getCodeClient(), finalClaim.getType(), ClaimEventType.MAIL_SENT_AGENT,
                        approuver.getFirstandlastname(), approuver.getEmail(), finalClaim.getTreatmentAffectedTo().getFirstandlastname() + "|" + finalClaim.getTreatmentAffectedTo().getEmail());
                Log successLog = Log.builder()
                    .libelle("Mail notification solution approuvée")
                    .content("Success mail notification solution approuvée")
                    .createdAt(LocalDateTime.now())
                    .type(LogType.INFO)
                    .userId(0L)
                   .userIpAddress(Utils.getClientIpAddress(httpServletRequest))
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
                            .userIpAddress(Utils.getClientIpAddress(httpServletRequest))
                            .target(LogTarget.APP)
                            .build();
    
                    logServiceImpl.saveLog(log2);
                }
            }      
      

        return claim;
    }

    @Override
    public Claim classedClaim(Claim claim, User classer) {
        claim.setStatus(ClaimStatus.CLASSED);
        claim.setUpdatedAt(LocalDateTime.now());
        claim.setClassedBy(classer);
        claim = repository.save(claim);
        claimEventServiceImpl.log(claim.getId(), claim.getCodeClient(), claim.getType(), ClaimEventType.CLASSED,
                classer.getFirstandlastname(), classer.getEmail(), null);

        // Liste des pilotes
        List<User> pilotes = authServiceImpl.getUsersByRoles(Arrays.asList(Role.PILOTE));

        String messageHtml = """
        <html>
        <body style="font-family: Arial, sans-serif; background-color: #f7f7f7; padding: 20px;">
            <div style="max-width: 600px; margin: auto; background: white; border-radius: 8px;
                        box-shadow: 0 2px 8px rgba(0,0,0,0.1); padding: 20px;">

            <h2 style="color: #004080; text-align: center;">Réclamation classée - GPR</h2>

            <p>Bonjour %s, Pilote de la plateforme <strong>GPR</strong></p>

            <p>
                La réclamation portant le code <strong>%s</strong> a été classée dans le système.
            </p>

            <div style="margin-top: 20px; background-color: #f0f8ff; border-left: 4px solid #004080;
                        padding: 10px 15px;">
                <p style="margin: 0;"><strong>Détails de la réclamation :</strong></p>
                <p style="margin: 5px 0;">📌 <strong>Code réclamation :</strong> %s</p>
                <p style="margin: 5px 0;">📅 <strong>Date de réception :</strong> %s</p>
                <p style="margin: 5px 0;">✅ <strong>Statut :</strong> Classée</p>
            </div>

            <p style="margin-top: 20px;">
                Vous pouvez consulter cette réclamation dans la plateforme <strong>GPR</strong> pour tout suivi nécessaire.
            </p>

            <p style="margin-top: 30px;">Cordialement,<br>L’équipe GPR</p>

            <p style="font-size: 12px; color: gray; text-align: center; margin-top: 30px;">
                Cet email a été généré automatiquement. Merci de ne pas y répondre.
            </p>

            </div>
        </body>
        </html>
        """.formatted(pilotes.get(0).getFirstandlastname(),claim.getCodeClient(),claim.getCodeClient(),Utils.convertLocalDateTimeToStr(claim.getReceiptDateTime()));
        final Claim finalClaim = claim;
       
            try {
                // Envoi du mail à tous les pilotes
                mailService.sendMail(pilotes.get(0).getEmail(),"Réclamation classée - GPR",messageHtml,null);
                claimEventServiceImpl.log(finalClaim.getId(), finalClaim.getCodeClient(), finalClaim.getType(), ClaimEventType.MAIL_SENT_AGENT,
                        classer.getFirstandlastname(), classer.getEmail(), "Pilote Principal " +pilotes.get(0).getFirstandlastname() + "|" + pilotes.get(0).getEmail());
                Log log = Log
                    .builder()
                    .libelle("Classification de réclamation")
                    .content("La réclamation portant le code " + finalClaim.getCode() + " a été classée"+
                                " par " + classer.getFirstandlastname()+".")
                    .type(LogType.INFO)
                     .userId(classer.getId())
                    .userIpAddress(Utils.getClientIpAddress(httpServletRequest))
                    .target(finalClaim.getType().equals(ClaimType.CLAIM) ? LogTarget.CLAIM : LogTarget.DENUNCIACION)
                    .build();
                logServiceImpl.saveLog(log);
            } catch (Exception e) {                        
                if (e != null) {
                    Log log2 = Log
                            .builder()
                            .libelle("Echec mail notification création session")
                            .content(e.getMessage())
                            .createdAt(LocalDateTime.now())
                            .type(LogType.ERROR)
                            .userId(0L)
                            .userIpAddress(Utils.getClientIpAddress(httpServletRequest))
                            .target(LogTarget.APP)
                            .build();

                    logServiceImpl.saveLog(log2);
                }
            }
      
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
        claimEventServiceImpl.log(claim.getId(), claim.getCodeClient(), claim.getType(), ClaimEventType.LITIGATION,
                litigator.getFirstandlastname(), litigator.getEmail(), null);
        LocalDateTime majDate = LocalDateTime.now();
        for (ExternalRecourse externalRecourse : externalRecour) {
            externalRecourse.setUpdatedAt(majDate);
            List<Claim> oldClaimsChoosed = externalRecourse.getClaims();
            oldClaimsChoosed.add(claim);
            externalRecourse.setClaims(oldClaimsChoosed);
            externalRecourseServiceImpl.saveExternalRecourse(externalRecourse);
        }

        // Liste des pilotes / responsables à notifier
        List<User> pilotes = authServiceImpl.getUsersByRoles(Arrays.asList(Role.PILOTE));

        String messageHtml = """
        <html>
        <body style="font-family: Arial, sans-serif; background-color: #f7f7f7; padding: 20px;">
            <div style="max-width: 600px; margin: auto; background: white; border-radius: 8px;
                        box-shadow: 0 2px 8px rgba(0,0,0,0.1); padding: 20px;">

            <h2 style="color: #b00020; text-align: center;">Réclamation au statut Contentieux - GPR</h2>

            <p>Bonjour %s, Pilote de la plateforme <strong>GPR</strong></p>

            <p>
                La réclamation portant le code <strong>%s</strong> a été passée au statut <strong>Contentieux</strong>.
            </p>

            <div style="margin-top: 20px; background-color: #fff0f0; border-left: 4px solid #b00020;
                        padding: 10px 15px;">
                <p style="margin: 0;"><strong>Détails de la réclamation :</strong></p>
                <p style="margin: 5px 0;">📌 <strong>Code réclamation :</strong> %s</p>
                <p style="margin: 5px 0;">📅 <strong>Date de réception :</strong> %s</p>
                <p style="margin: 5px 0;">⚠️ <strong>Statut :</strong> Contentieux</p>
            </div>

            <p style="margin-top: 20px;">
                Veuillez examiner cette réclamation sur la plateforme <strong>GPR</strong> au besoin.
            </p>

            <p style="margin-top: 30px;">Cordialement,<br>L’équipe GPR</p>

            <p style="font-size: 12px; color: gray; text-align: center; margin-top: 30px;">
                Cet email a été généré automatiquement. Merci de ne pas y répondre.
            </p>

            </div>
        </body>
        </html>
        """.formatted(pilotes.get(0).getFirstandlastname(),claim.getCodeClient(),claim.getCodeClient(),Utils.convertLocalDateTimeToStr(claim.getReceiptDateTime()));
        final Claim finalClaim = claim;
       
            try {
                // Envoi du mail à tous les responsables/pilotes
                mailService.sendMail(pilotes.get(0).getEmail(),"Réclamation au statut Contentieux - GPR",messageHtml,null);
                claimEventServiceImpl.log(finalClaim.getId(), finalClaim.getCodeClient(), finalClaim.getType(), ClaimEventType.MAIL_SENT_AGENT,
                        litigator.getFirstandlastname(), litigator.getEmail(), "Pilote Principal " +pilotes.get(0).getFirstandlastname() + "|" + pilotes.get(0).getEmail());
                Log log = Log
                    .builder()
                    .libelle("Classification de réclamation")
                    .content("La réclamation portant le code " + finalClaim.getCode() + " a été passée au statut contentieux"+
                                " par " + litigator.getFirstandlastname()+".")
                    .type(LogType.INFO)
                    .userId(litigator.getId())
                    .userIpAddress(Utils.getClientIpAddress(httpServletRequest))
                    .target(finalClaim.getType().equals(ClaimType.CLAIM) ? LogTarget.CLAIM : LogTarget.DENUNCIACION)
                    .build();
                logServiceImpl.saveLog(log);
            } catch (Exception e) {                        
                if (e != null) {
                    Log log2 = Log
                            .builder()
                            .libelle("Echec mail notification réclamation contentieux")
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
      
        return claim;
    }

    @Override
    public List<Claim> getClaimByStatus(ClaimType type, ClaimStatus status) {
        return repository.findByTypeAndIsDeletedFalseAndStatus(type, status);
    }

    @Override
    public List<Claim> getAllNotTempSave(ClaimType type) {
         return repository.findByTypeAndIsDeletedFalseAndStatusNot(type, ClaimStatus.TEMP_SAVED);
    }

    @Override
    public List<Claim> getAllByTypeStatusCollector(ClaimType type, ClaimStatus status, User collector) {
        return repository.findByTypeAndIsDeletedFalseAndStatusAndCollector(type, status, collector);
    }

    // @Override
    public List<Claim> getAllByTypeAndStatusIn(ClaimType type, List<ClaimStatus> statusList) {
        return repository.findByTypeAndIsDeletedFalseAndStatusIn(type, statusList);
    }

    @Override
    public List<Claim> getAllByTypeAndCollectorAndStatusOrTreatmentAffectedToAndStatusIn(ClaimType type,
            ClaimStatus status, User affectedTo,
            List<ClaimStatus> statusList) {

        List<Claim> resultat = repository.findByTypeAndIsDeletedFalseAndCollectorAndStatusOrTypeAndTreatmentAffectedToAndStatusIn(type,
                affectedTo, status, type, affectedTo, statusList);
        List<Claim> tmp = resultat;

        tmp = resultat.stream().filter(claim -> {
            // 1️⃣ Exclusion : l'utilisateur ne voit pas sa propre réclamation
            //    si celle-ci n'est pas rattachée à son point de service
            if (claim.getCollector().equals(affectedTo)
                    && !claim.getServicePoint().equals(affectedTo.getServicePoint())) {
                return false;
            }
            
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
        List<Claim> req = repository.findByTypeAndIsDeletedFalseAndStatusIn(type, statusList);
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
        List<Claim> req = repository.findByTypeAndIsDeletedFalseAndStatusIn(type, statusList);
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
        claimEventServiceImpl.log(claim.getId(), claim.getCodeClient(), type, ClaimEventType.SAVED,
                collector.getFirstandlastname(), collector.getEmail(), null);
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

        String finalType = "Réclamation";
        if (finalClaim.getType().equals(ClaimType.DENUNCIACION)) {
            finalType = "Dénonciation";
        }

        String messageHtml = """
        <html>
        <body style="font-family: Arial, sans-serif; background-color: #f7f7f7; padding: 20px;">
            <div style="max-width: 600px; margin: auto; background: white; border-radius: 8px;
                        box-shadow: 0 2px 8px rgba(0,0,0,0.1); padding: 20px;">

            <h2 style="color: #004080; text-align: center;">Nouvelle dénonciation enregistrée - GPR</h2>

            <p>Bonjour,</p>

            <p>
                Une nouvelle %s a été enregistrée avec succès dans le système. Vous recevez ce mail en tant qu'utilisateur habilité à être notifié des nouvelles réclamations.
            </p>

            <div style="margin-top: 20px; background-color: #f0f8ff; border-left: 4px solid #004080;
                        padding: 10px 15px;">
                <p style="margin: 0;"><strong>Détails de la %s :</strong></p>
                <p style="margin: 5px 0;">📌 <strong>Code %s :</strong> %s</p>
                <p style="margin: 5px 0;">📅 <strong>Date de réception :</strong> %s</p>
                <p style="margin: 5px 0;">📝 <strong>Objet :</strong> %s</p>
            </div>

            <p style="margin-top: 20px;">
                Nous vous encourageons à examiner cette %s dès que possible et à prendre les mesures nécessaires pour son traitement.
            </p>

            <p style="margin-top: 30px;">Cordialement,<br>L’équipe GPR</p>

            <p style="font-size: 12px; color: gray; text-align: center; margin-top: 30px;">
                Cet email a été généré automatiquement. Merci de ne pas y répondre.
            </p>

            </div>
        </body>
        </html>
        """.formatted(finalType,finalType,finalType,claim.getCodeClient(),Utils.convertLocalDateTimeToStr(claim.getReceiptDateTime()),claim.getObjet().getLibelle(),finalType);
        List<User> usersToContact = authServiceImpl.getEmailReceiversForNotif(claim.getServicePoint());
        List<User> pilote = authServiceImpl.getUsersByRoles(Arrays.asList(Role.PILOTE));
        usersToContact.addAll(pilote); 
        // Envoi de mail en parallèle
       
            try {
                mailService.sendMail(usersToContact,"Nouvelle Dénonciation enregistrée - GPR", messageHtml, null);
                for (User u : usersToContact) {
                    claimEventServiceImpl.log(claim.getId(), claim.getCodeClient(), type, ClaimEventType.MAIL_SENT_AGENT,
                            collector.getFirstandlastname(), collector.getEmail(), u.getFirstandlastname() + "|" + u.getEmail());
                }

                Log successLog = Log.builder()
                    .libelle("Mail notification nouvelle dénonciation")
                    .content("Success mail notification nouvelle dénonciation")
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
                            .claimCodeClient(claim.getCodeClient())
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
            claim.setTransmittedBy(connectedUser);
            // Sauvegarder la réclamation mise à jour
            claim = repository.save(claim);
            String transmittedRoleLabel = transmittedTo.isRa() ? "RA"
                    : (transmittedTo.getAdditionalrole() != null ? transmittedTo.getAdditionalrole().toString()
                    : (transmittedTo.getPoste() != null ? transmittedTo.getPoste().getLibelle() : ""));
            String transmittedSpLabel = transmittedTo.getServicePoint() != null ? transmittedTo.getServicePoint().getLibelle() : "";
            claimEventServiceImpl.log(claim.getId(), claim.getCodeClient(), claim.getType(), ClaimEventType.TRANSMITTED,
                    connectedUser.getFirstandlastname(), connectedUser.getEmail(),
                    transmittedTo.getFirstandlastname() + "|" + transmittedRoleLabel + "|" + transmittedSpLabel);
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
                String type = finalClaim.getType() == ClaimType.CLAIM ? "réclamation" : "dénonciation";
               
                    try {
                      
                        String messageHtml = """
                        <html>
                        <body style="font-family: Arial, sans-serif; background-color: #f7f7f7; padding: 20px;">
                            <div style="max-width: 600px; margin: auto; background: white; border-radius: 8px;
                                        box-shadow: 0 2px 8px rgba(0,0,0,0.1); padding: 20px;">

                            <h2 style="color: #004080; text-align: center;">Transmission de traitement - GPR</h2>

                            <p>Bonjour <strong>%s</strong>,</p>

                            <p>
                                Un utilisateur a transmis la gestion d'une <strong>%s</strong> à votre attention, car il est dans l'incapacité de la traiter.
                            </p>

                            <div style="margin-top: 20px; background-color: #f0f8ff; border-left: 4px solid #004080;
                                        padding: 10px 15px;">
                                <p style="margin: 0;"><strong>Détails de la %s :</strong></p>
                                <p style="margin: 5px 0;">📌 <strong>Code de la %s :</strong> %s</p>
                                <p style="margin: 5px 0;">📅 <strong>Date de réception :</strong> %s</p>
                                <p style="margin: 5px 0;">📝 <strong>Objet :</strong> %s</p>
                               
                            </div>

                            <p style="margin-top: 20px;">
                                Veuillez prendre les mesures nécessaires pour permettre le traitement de cette %s dans les meilleurs délais.
                            </p>

                            <p style="margin-top: 30px;">Cordialement,<br>L’équipe GPR</p>

                            <p style="font-size: 12px; color: gray; text-align: center; margin-top: 30px;">
                                Cet email a été généré automatiquement. Merci de ne pas y répondre.
                            </p>

                            </div>
                        </body>
                        </html>
                        """.formatted(finalTransmittedTo.getFirstandlastname(),type,type,type,finalClaim.getCodeClient(),Utils.convertLocalDateTimeToStr(finalClaim.getReceiptDateTime()),finalClaim.getObjet().getLibelle(),type);

                        mailService.sendMail(destis, "Transmission de traitement - GPR", messageHtml, null);
                                                        
                        Log successLog = Log.builder()
                            .libelle("Mail notification transmission de réclamation")
                            .content("Success mail notification transmission de réclamation")
                            .createdAt(LocalDateTime.now())
                            .type(LogType.INFO)
                            .userId(0L)
                            .userIpAddress(Utils.getClientIpAddress(httpServletRequest))
                            .target(LogTarget.APP)
                            .build();

                        logServiceImpl.saveLog(successLog);                           
                    } catch (Exception e) {                        
                        if (e != null) {
                            Log log2 = Log
                                    .builder()
                                    .libelle("Echec mail notification transmission de réclamation")
                                    .content(e.getMessage())
                                    .createdAt(LocalDateTime.now())
                                    .type(LogType.ERROR)
                                    .userId(0L)
                                   .userIpAddress(Utils.getClientIpAddress(httpServletRequest))
                                    .target(LogTarget.APP)
                                    .build();

                            logServiceImpl.saveLog(log2);
                        }
                    }

                    try {
                        String message = "La "+ type +" portant le code "+ finalClaim.getCodeClient()
                                + " vous a été transmise pour prise en charge.";
                        Utils.sendSms(destis, message, settingServiceImpl);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
              

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

        claim = repository.save(claim);
        final Claim finalClaim = claim;
        
        List<User> pilotes = authServiceImpl.getUsersByRoles(Arrays.asList(Role.PILOTE));
        
        Double apercuContent = claim.getContent().length() * 0.5;

        String messageHtml = """
        <html>
        <body style="font-family: Arial, sans-serif; background-color: #f7f7f7; padding: 20px;">
            <div style="max-width: 600px; margin: auto; background: white; border-radius: 8px;
                        box-shadow: 0 2px 8px rgba(0,0,0,0.1); padding: 20px;">

            <h2 style="color: #004080; text-align: center;">Nouvelle réclamation collectée - GPR BOT</h2>

            <p>Bonjour,</p>

            <p>
                Une nouvelle réclamation a été collectée via <strong>GPR BOT</strong>.
                Cette réclamation nécessite votre attention rapide.
            </p>

            <p style="margin-top: 30px;">Cordialement,<br>L’équipe GPR</p>

            <p style="font-size: 12px; color: gray; text-align: center; margin-top: 30px;">
                Cet email a été généré automatiquement. Merci de ne pas y répondre.
            </p>

            </div>
        </body>
        </html>
        """;
                                          
        // Envoi de mail en parallèle
       
            try {
                mailService.sendMail(pilotes.get(0).getEmail(), "Nouvelle réclamation collectée - GPR BOT", messageHtml, null);
                                        
                Log successLog = Log.builder()
                    .libelle("Mail notification enregistrement réclamation")
                    .content("Success mail notification enregistrement réclamation")
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
        claimEventServiceImpl.log(claim.getId(), claim.getCodeClient(), claim.getType(), ClaimEventType.CONVERTED,
                connectedUser.getFirstandlastname(), connectedUser.getEmail(), "Converti en dénonciation");
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

    //    @Override
    public void deleteById(Claim claim, String reason, User currentUser) throws NotFoundException {
        
        // Vérifie que la réclamation n’est pas déjà supprimée
        if (Boolean.TRUE.equals(claim.isDeleted())) {
            throw new IllegalStateException("Cette réclamation est déjà supprimée.");
        }

        // Marque la réclamation comme supprimée (soft delete)
        claim.setDeleted(true);
        claim.setDeletedAt(LocalDateTime.now());
        claim.setDeletedBy(currentUser);
        claim.setDelete_reason(reason);
        claim.setRestored(false); // par sécurité

        repository.save(claim);

        Log log = Log
                .builder()
                .content("La " + 
                (claim.getType().equals(ClaimType.CLAIM) ? 
                    "réclamation " : "dénonciation ") +
                "portant le code: " + claim.getCode() + 
                " a été supprimée par l'utilisateur: " + currentUser.getFirstandlastname())
                .createdAt(LocalDateTime.now())
                .type(LogType.INFO)
                .userId(currentUser.getId())
                .userIpAddress(Utils.getClientIpAddress(httpServletRequest))
                .build();
        if (claim.getType().equals(ClaimType.CLAIM)) {
            log.setLibelle("Suppression de Réclamation");
            log.setTarget(LogTarget.CLAIM);
        } else {
            log.setLibelle("Suppression de Dénonciation");
            log.setTarget(LogTarget.DENUNCIACION);
        }

        logServiceImpl.saveLog(log);
    }

    public void restoreById(Long id, User currentUser) throws NotFoundException {
        Claim claim = repository.findById(id)
            .orElseThrow(() -> new RuntimeException("Réclamation introuvable"));

        if (!claim.isDeleted()) {
            throw new RuntimeException("La réclamation n'est pas supprimée");
        }

        // Marque la réclamation comme supprimée (soft delete)
        claim.setDeleted(false);
        claim.setDeletedAt(null);
        claim.setDeletedBy(null);
        claim.setDelete_reason(null);
        claim.setRestored(true); 
        claim.setRestoredAt(LocalDateTime.now());
        claim.setRestoredBy(currentUser); 

        repository.save(claim);

        Log log = Log
                .builder()
                .content("La " + 
                (claim.getType().equals(ClaimType.CLAIM) ? 
                    "réclamation " : "dénonciation ") +
                "portant le code: " + claim.getCode() + 
                " a été restaurée par l'utilisateur: " + currentUser.getFirstandlastname())
                .createdAt(LocalDateTime.now())
                .type(LogType.INFO)
                .userId(currentUser.getId())
                .userIpAddress(Utils.getClientIpAddress(httpServletRequest))
                .build();
        if (claim.getType().equals(ClaimType.CLAIM)) {
            log.setLibelle("Restauration de Réclamation");
            log.setTarget(LogTarget.CLAIM);
        } else {
            log.setLibelle("Restauration de Dénonciation");
            log.setTarget(LogTarget.DENUNCIACION);
        }

        logServiceImpl.saveLog(log);

    }


    @Override
    public List<Claim> checkPhoneCrossAgency(String phone, ServicePoint userAgency) {
        List<ClaimStatus> nonTerminatedStatuses = Arrays.asList(
                ClaimStatus.SAVED, ClaimStatus.AFFECTED, ClaimStatus.TO_APPROUVED,
                ClaimStatus.DESAPPROUVED, ClaimStatus.TREAT, ClaimStatus.UNSATISFIED,
                ClaimStatus.PARTIAL_SATISFIED, ClaimStatus.LITIGATION, ClaimStatus.TRANSMITTED
        );
        if (phone == null || phone.isEmpty() || userAgency == null) return Collections.emptyList();
        return repository.findByTelAndServicePointNotAndStatusInAndIsDeletedFalse(phone, userAgency, nonTerminatedStatuses);
    }

    @Override
    public List<Claim> checkPhone(String phone, ServicePoint userAgency, boolean isPilot) {
        List<ClaimStatus> nonTerminatedStatuses = Arrays.asList(
                ClaimStatus.SAVED,
                ClaimStatus.AFFECTED,
                ClaimStatus.TO_APPROUVED,
                ClaimStatus.DESAPPROUVED,
                ClaimStatus.TREAT,
                ClaimStatus.UNSATISFIED,
                ClaimStatus.PARTIAL_SATISFIED,
                ClaimStatus.LITIGATION,
                ClaimStatus.CLASSED,
                ClaimStatus.TRANSMITTED
        );

        List<Claim> claims = Collections.emptyList();
        if (isPilot) {
            if (phone != null && !phone.isEmpty()) {
                claims = repository.findByTelAndStatusInAndIsDeletedFalse(phone, nonTerminatedStatuses);
            }
        } else {
            if (phone != null && !phone.isEmpty()) {
                claims = repository.findByTelAndServicePointAndStatusInAndIsDeletedFalse(phone, userAgency, nonTerminatedStatuses);
            }
        }

        return claims;
    }

    public List<TrashDto> getAllDeleted() {
      
       List<TrashDto> trashList = new ArrayList<>();

        // Réclamations supprimées
        repository.findByIsDeletedTrue().forEach(c -> {
            trashList.add(new TrashDto(
                c.getId(),
                c.getType(),
                c.getCodeClient(),
                c.getClientFirstAndLastName(),
                c.isDeleted(),
                new UserResponse(
                    c.getDeletedBy().getId(),
                    c.getDeletedBy().getCode(),
                    c.getDeletedBy().getFirstandlastname()
                ),
                c.getDeletedAt(),
                c.getDelete_reason()
            ));
        });

        // Suggestions supprimées
        suggestionRepository.findByIsDeletedTrue().forEach(s -> {
            trashList.add(new TrashDto(
                s.getId(),
                ClaimType.SUGGESTION,
                s.getCodeClient(),
                s.getClientFirstAndLastName(),
                s.isDeleted(),
                new UserResponse(
                    s.getDeletedBy().getId(),
                    s.getDeletedBy().getCode(),
                    s.getDeletedBy().getFirstandlastname()
                ),
                s.getDeletedAt(),
                s.getDelete_reason()
            ));
        });

        // Optionnel : trier par date de suppression
        trashList.sort(Comparator.comparing(TrashDto::getDeletedAt).reversed());

        return trashList;
    }

    @Override
    public Claim saveDraft(Long claimId, ClaimDraftRequest request) throws Exception {
        Claim claim = repository.findById(claimId)
                .orElseThrow(() -> new Exception("Réclamation introuvable : " + claimId));
        claim.setDraftSolution(request.getDraftSolution());
        claim.setDraftCommentaire(request.getDraftCommentaire());
        claim.setDraftUserId(request.getUserId());
        claim.setDraftSavedAt(LocalDateTime.now());
        return repository.save(claim);
    }


}
