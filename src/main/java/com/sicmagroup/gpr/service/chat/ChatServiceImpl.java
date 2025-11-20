package com.sicmagroup.gpr.service.chat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.springframework.boot.actuate.autoconfigure.health.HealthProperties.Status;
import org.springframework.stereotype.Service;

import com.sicmagroup.gpr.api.chat.ChatInitRequest;
import com.sicmagroup.gpr.api.chat.EjectGuestRequest;
import com.sicmagroup.gpr.api.chat.InviteGuestRequest;
import com.sicmagroup.gpr.api.chat.message.SessionJoinRequest;
import com.sicmagroup.gpr.api.chat.message.SessionJoinResponse;
import com.sicmagroup.gpr.domain.enumeration.ChatStatus;
import com.sicmagroup.gpr.domain.enumeration.ClaimStatus;
import com.sicmagroup.gpr.domain.enumeration.ClaimType;
import com.sicmagroup.gpr.domain.enumeration.LogTarget;
import com.sicmagroup.gpr.domain.enumeration.LogType;
import com.sicmagroup.gpr.domain.enumeration.Role;
import com.sicmagroup.gpr.domain.model.Claim;
import com.sicmagroup.gpr.domain.model.Log;
import com.sicmagroup.gpr.domain.model.User;
import com.sicmagroup.gpr.domain.model.chat.Chat;
import com.sicmagroup.gpr.repository.ClaimRepository;
import com.sicmagroup.gpr.repository.UserRepository;
import com.sicmagroup.gpr.repository.chat.ChatRepository;
import com.sicmagroup.gpr.service.MailService;
import com.sicmagroup.gpr.service.log.LogServiceImpl;
import com.sicmagroup.gpr.service.setting.SettingServiceImpl;
import com.sicmagroup.gpr.utils.Utils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ClaimRepository claimRepository;
    private final UserRepository userRepository;
    private final ChatRepository repository;
    private final LogServiceImpl logServiceImpl;
    private final SettingServiceImpl settingServiceImpl;
    private final MailService mailService;

    @Override
    public Chat init(ChatInitRequest request) throws Exception {
        Claim claim = claimRepository.findById(request.getClaimId())
                .orElseThrow(() -> new Exception("Réclamation introuvable"));
        if (claim.getSession() != null) {
            throw new Exception("Une session est déjà en cours pour cette réclamation");
        }
        User user = userRepository.findById(request.getCreatorId())
                .orElseThrow(() -> new Exception("Compte utilisateur invalide"));
       
            if (Arrays.asList(ClaimStatus.SAVED,
            ClaimStatus.AFFECTED, ClaimStatus.TO_APPROUVED, ClaimStatus.DESAPPROUVED, ClaimStatus.UNSATISFIED, ClaimStatus.PARTIAL_SATISFIED, ClaimStatus.CLASSED).contains(claim.getStatus()) ) {
               
                List<User> members = Arrays.asList(user);
                Chat chat = Chat
                        .builder()
                        .claim(claim)
                        .members(new ArrayList<User>())
                        .guests(new ArrayList<User>())
                        .createdBy(user)
                        .status(ChatStatus.OPEN)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();
                chat = repository.save(chat);
                claim.setSession(chat);
                claimRepository.save(claim);

                for (User u : members) {
                    chat.getMembers().add(u);
                    chat = repository.save(chat);
                    u.getChatsMember().add(chat);
                    userRepository.save(u);
                }
                String type = "Réclamation";
                if (claim.getType().equals(ClaimType.DENUNCIACION)) {
                    type = "Dénonciation";
                }

                final String finalType = type;
                Double apercuContent = claim.getContent().length() * 0.5;
            
                // Envoi de mail en parallèle
                
                    try {
                        String message = """
                        <html>
                        <body style="font-family: Arial, sans-serif; background-color: #f7f7f7; padding: 20px;">
                            <div style="max-width: 600px; margin: auto; background: white; border-radius: 8px; 
                                        box-shadow: 0 2px 8px rgba(0,0,0,0.1); padding: 20px;">

                            <h2 style="color: #004080; text-align: center;">Démarrage de session - GPR</h2>

                            <p>Bonjour <strong>%s</strong>,</p>

                            <p>
                                Votre session a été démarrée avec succès sur la plateforme de gestion des plaintes et réclamations (<strong>GPR</strong>).
                            </p>

                            <div style="margin-top: 20px; background-color: #f0f8ff; border-left: 4px solid #004080; 
                                        padding: 10px 15px;">
                                <p style="margin: 0;"><strong>Détails de la %s :</strong></p>
                                <p style="margin: 5px 0;">📌 <strong>Code %s :</strong> %s</p>
                                <p style="margin: 5px 0;">📅 <strong>Date de réception :</strong> %s</p>
                                <p style="margin: 5px 0;">📝 <strong>Aperçu du contenu :</strong> %s...</p>
                            </div>

                            <p style="margin-top: 20px;">
                                Vous pouvez désormais rejoindre la session pour inviter d'autres utilisateurs et traiter la %s.
                            </p>

                            <p style="margin-top: 30px;">Cordialement,<br>L’équipe GPR</p>

                            <p style="font-size: 12px; color: gray; text-align: center; margin-top: 30px;">
                                Cet email a été généré automatiquement. Merci de ne pas y répondre.
                            </p>

                            </div>
                        </body>
                        </html>
                        """.formatted(user.getFirstandlastname(),finalType,finalType,claim.getCodeClient(),Utils.convertLocalDateTimeToStr(claim.getReceiptDateTime()),claim.getContent().substring(0, apercuContent.intValue()),finalType);

                        mailService.sendMail(members, "Démarrage de session - GPR",message,null);

                                                
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
             

                return chat;
            } else {
                throw new Exception("Status de la réclamation invalide");
                // TODO: Save this in log
            }
        // } else {
        //     throw new Exception("Utilisateur non autorisé. Cette action sera répertoriée.");
        //     // TODO: Save this in log
        // }

    }

    @Override
    public Chat reInit(ChatInitRequest request) throws Exception {
        Claim claim = claimRepository.findById(request.getClaimId())
                .orElseThrow(() -> new Exception("Réclamation introuvable"));
        User user = userRepository.findById(request.getCreatorId())
                .orElseThrow(() -> new Exception("Compte utilisateur invalide"));

       
            if (claim.getStatus().equals(ClaimStatus.PARTIAL_SATISFIED)
                    || claim.getStatus().equals(ClaimStatus.UNSATISFIED)) {
                Chat chat = repository.findByClaim(claim)
                        .orElseThrow(() -> new Exception("Précédente session introuvable"));
                // TODO: Permettre de réouvrir des sessions si jamais on ne retrouvait pas
                // l'ancienne ?

                chat.setStatus(ChatStatus.OPEN);
                chat.setUpdatedAt(LocalDateTime.now());
                chat = repository.save(chat);
            } else {
                throw new Exception("Status de la réclamation invalide");
                // TODO: Save this in log
            }
       
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'reInit'");
    }

    @Override
    public SessionJoinResponse inviteGuest(SessionJoinRequest request) throws Exception {
        Claim claim = claimRepository.findByCode(request.getClaimCode())
                .orElseThrow(() -> new Exception("Réclamation introuvable"));
        Chat chat = repository.findById(claim.getSession().getId())
                .orElseThrow(() -> new Exception("La session est introuvable"));
        User guest = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new Exception("Compte utilisateur invalide"));
        System.out.println(chat.getGuests());
        if (chat.getGuests() != null) {
            if (!chat.getGuests().contains(guest)) {
                chat.getGuests().add(guest);
                chat.setUpdatedAt(LocalDateTime.now());
                guest.getChatsGuest().add(chat);
                guest.setUpdatedAt(LocalDateTime.now());
            }

        } else {
            List<User> guests = new ArrayList<>();
            guests.add(guest);
            guests.addAll(chat.getGuests());
            chat.setGuests(guests);
        }

        chat = repository.save(chat);
        final Chat finalChat = chat;
        
        userRepository.save(guest);
        String type = "Réclamation";
        if (claim.getType().equals(ClaimType.DENUNCIACION)) {
            type = "Dénonciation";
        }
        final String finalType = type;
        // Envoi de mail en parallèle
        
            try {
               String message = """
                <html>
                <body style="font-family: Arial, sans-serif; background-color: #f7f7f7; padding: 20px;">
                    <div style="max-width: 600px; margin: auto; background: white; border-radius: 8px; 
                                box-shadow: 0 2px 8px rgba(0,0,0,0.1); padding: 20px;">

                    <h2 style="color: #004080; text-align: center;">Invitation à participer à une session - GPR</h2>

                    <p>Bonjour <strong>%s</strong>,</p>

                    <p>
                        Vous avez été invité(e) à intervenir dans la session concernant la %s suivante :
                    </p>

                    <div style="margin-top: 20px; background-color: #f0f8ff; border-left: 4px solid #004080; 
                                padding: 10px 15px;">
                        <p style="margin: 0;"><strong>Détails de la %s :</strong></p>
                        <p style="margin: 5px 0;">📌 <strong>Code %s :</strong> %s</p>
                    </div>

                    <p style="margin-top: 20px;">
                        Connectez-vous à la plateforme <strong>GPR</strong> pour participer à la discussion et apporter vos interventions.
                    </p>

                    <p style="margin-top: 30px;">Cordialement,<br>L’équipe GPR</p>

                    <p style="font-size: 12px; color: gray; text-align: center; margin-top: 30px;">
                        Cet email a été généré automatiquement. Merci de ne pas y répondre.
                    </p>

                    </div>
                </body>
                </html>
                """.formatted(guest.getFirstandlastname(),finalType,finalType,finalType,finalChat.getClaim().getCodeClient());

                mailService.sendMail(guest.getEmail(), "Invitation à une session - GPR",message,null);
                                  
                Log successLog = Log.builder()
                    .libelle("Mail notification invitation chat")
                    .content("Success mail notification invitation chat")
                    .createdAt(LocalDateTime.now())
                    .type(LogType.INFO)
                    .userId(0L)
                    .userIpAddress("")
                    .target(LogTarget.APP)
                    .build();

                logServiceImpl.saveLog(successLog); 

                Utils.sendSms(Arrays.asList(guest), message, settingServiceImpl);
            } catch (Exception e) {
                // e.printStackTrace();
                // TODO: save in log
                
                Log log2 = Log
                        .builder()
                        .libelle("Echec mail notification invitation chat")
                        .content(e.getMessage())
                        .createdAt(LocalDateTime.now())
                        .type(LogType.ERROR)
                        .userId(0L)
                        .userIpAddress(null)
                        .target(LogTarget.APP)
                        .build();
    
                logServiceImpl.saveLog(log2);
            }
     

        SessionJoinResponse sessionJoinResponse = SessionJoinResponse
            .builder()
            .claimCode(request.getClaimCode())
            .code(guest.getCode())
            .firstAndLastName(guest.getFirstandlastname())
            .id(guest.getId())
            .isGuest(true)
            .role(guest.getAdditionalrole())
            .status(request.getStatus())
            .build();
        return sessionJoinResponse;
    }

    @Override
    public SessionJoinResponse ejectGuest(SessionJoinRequest request) throws Exception {
        Claim claim = claimRepository.findByCode(request.getClaimCode())
                .orElseThrow(() -> new Exception("Réclamation introuvable"));
        Chat chat = repository.findById(claim.getSession().getId())
                .orElseThrow(() -> new Exception("La session est introuvable"));
        User guest = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new Exception("Compte utilisateur invalide"));

        if (chat.getGuests().contains(guest)) {
            chat.getGuests().remove(guest);
        }

        chat.setUpdatedAt(LocalDateTime.now());
        chat = repository.save(chat);
        final Chat finalChat = chat;
        if (guest.getChatsGuest().contains(chat)) {
            guest.getChatsGuest().remove(chat);
            guest.setUpdatedAt(LocalDateTime.now());
            userRepository.save(guest);
        }
        String type = "Réclamation";
        if (claim.getType().equals(ClaimType.DENUNCIACION)) {
            type = "Dénonciation";
        }

        final String finalType = type;
        // Envoi de mail en parallèle
        
            try {
                String message = """
                <html>
                <body style="font-family: Arial, sans-serif; background-color: #f7f7f7; padding: 20px;">
                    <div style="max-width: 600px; margin: auto; background: white; border-radius: 8px; 
                                box-shadow: 0 2px 8px rgba(0,0,0,0.1); padding: 20px;">

                    <h2 style="color: #cc0000; text-align: center;">Ejection d'une session - GPR</h2>

                    <p>Bonjour <strong>%s</strong>,</p>

                    <p>
                        Vous avez été <strong>exclu(e)</strong> de la discussion concernant la %s :
                    </p>

                    <div style="margin-top: 20px; background-color: #ffe6e6; border-left: 4px solid #cc0000; 
                                padding: 10px 15px;">
                        <p style="margin: 0;">📌 <strong>Code %s :</strong> %s</p>
                    </div>

                    <p style="margin-top: 20px;">
                        Si vous pensez que cette exclusion est une erreur, veuillez contacter le collaborateur ayant initié la session.
                    </p>

                    <p style="margin-top: 30px;">Cordialement,<br>L’équipe GPR</p>

                    <p style="font-size: 12px; color: gray; text-align: center; margin-top: 30px;">
                        Cet email a été généré automatiquement. Merci de ne pas y répondre.
                    </p>

                    </div>
                </body>
                </html>
                """.formatted(guest.getFirstandlastname(),finalType,finalType,finalChat.getClaim().getCodeClient());

                mailService.sendMail(guest.getEmail(),"Ejection d'une session - GPR",message,null);
                                                          
                Log successLog = Log.builder()
                    .libelle("Mail notification éjection chat")
                    .content("Success mail notification éjection chat")
                    .createdAt(LocalDateTime.now())
                    .type(LogType.INFO)
                    .userId(0L)
                    .userIpAddress("")
                    .target(LogTarget.APP)
                    .build();

                logServiceImpl.saveLog(successLog); 
                Utils.sendSms(Arrays.asList(guest), message,settingServiceImpl);
            } catch (Exception e) {
                // e.printStackTrace();
                // TODO: save in log
                
                Log log2 = Log
                        .builder()
                        .libelle("Echec mail notification éjection chat")
                        .content(e.getMessage())
                        .createdAt(LocalDateTime.now())
                        .type(LogType.ERROR)
                        .userId(0L)
                        .userIpAddress(null)
                        .target(LogTarget.APP)
                        .build();
    
                logServiceImpl.saveLog(log2);
            }
     
        
        SessionJoinResponse sessionJoinResponse = SessionJoinResponse
            .builder()
            .claimCode(request.getClaimCode())
            .code(guest.getCode())
            .firstAndLastName(guest.getFirstandlastname())
            .id(guest.getId())
            .isGuest(true)
            .role(guest.getAdditionalrole())
            .status(request.getStatus())
            .build();
        return sessionJoinResponse;
    }

    @Override
    public List<Chat> getChatByGuest(List<User> guestList) {
        List<Chat> chats = repository.findByGuestsIn(guestList);
        return chats;
    }

    @Override
    public Chat joinChat(SessionJoinRequest request) throws Exception {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new Exception("L'utilisateur envoyant le message est introuvable"));
        Claim claim = claimRepository.findByCode(request.getClaimCode())
                .orElseThrow(() -> new Exception("La réclamation est introuvable"));

        Chat chat = repository.findByClaim(claim).orElseThrow(() -> new Exception("La session est introuvable"));

        if (chat != null) {
            chat.getMembers().size();
            if (chat.getMembers() != null && chat.getMembers().isEmpty()) {
                List<User> members = new ArrayList<>();
                members.add(user);
                chat.setMembers(members);
                chat = repository.save(chat);
            } else if (chat.getMembers() != null) {
                if (!chat.getMembers().contains(user)) {
                    List<User> members = chat.getMembers();
                    members.add(user);
                    chat.setMembers(members);
                    chat = repository.save(chat);
                }

            } else {
                List<User> members = new ArrayList<>();
                members.add(user);
                chat.setMembers(members);
                chat = repository.save(chat);
            }
            return chat;
        } else {
            throw new Exception("Session introuvable");
        }
    }

}
