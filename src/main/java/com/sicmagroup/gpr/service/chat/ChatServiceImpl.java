package com.sicmagroup.gpr.service.chat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.boot.actuate.autoconfigure.health.HealthProperties.Status;
import org.springframework.stereotype.Service;

import com.sicmagroup.gpr.api.chat.ChatInitRequest;
import com.sicmagroup.gpr.api.chat.EjectGuestRequest;
import com.sicmagroup.gpr.api.chat.InviteGuestRequest;
import com.sicmagroup.gpr.api.chat.message.SessionJoinRequest;
import com.sicmagroup.gpr.api.chat.message.SessionJoinResponse;
import com.sicmagroup.gpr.domain.enumeration.ChatStatus;
import com.sicmagroup.gpr.domain.enumeration.ClaimStatus;
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
import com.sicmagroup.gpr.service.log.LogServiceImpl;
import com.sicmagroup.gpr.service.setting.SettingServiceImpl;
import com.sicmagroup.gpr.utils.SensitiveConstante;
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

    @Override
    public Chat init(ChatInitRequest request) throws Exception {
        Claim claim = claimRepository.findById(request.getClaimId())
                .orElseThrow(() -> new Exception("Réclamation introuvable"));
        if (claim.getSession() != null) {
            throw new Exception("Une session est déjà en cours pour cette réclamation");
        }
        User user = userRepository.findById(request.getCreatorId())
                .orElseThrow(() -> new Exception("Compte utilisateur invalide"));
        // if (user.getAdditionalrole().equals(Role.MEMBRE_CGR) || user.getAdditionalrole().equals(Role.PR_CGR)) {
            if (Arrays.asList(ClaimStatus.SAVED,
            ClaimStatus.AFFECTED, ClaimStatus.TO_APPROUVED, ClaimStatus.DESAPPROUVED, ClaimStatus.UNSATISFIED, ClaimStatus.PARTIAL_SATISFIED, ClaimStatus.CLASSED).contains(claim.getStatus()) ) {
                // List<User> members = userRepository.findByAdditionalroleIn(Arrays.asList(Role.MEMBRE_CGR, Role.PR_CGR));
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
                try {

                    Double apercuContent = claim.getContent().length() * 0.5;
                    String message = "Bonjour cher membre du CGR, le collègue " +
                            user.getFirstandlastname()
                            + " a démarré une session. \n\n" +
                            "\t * Code réclamation : " + claim.getCode() + " \n" +
                            "Détails de la réclamation :" + "\n\n" +
                            "* Date d'enregistrement : " +
                            Utils.convertLocalDateTimeToStr(claim.getReceiptDateTime())
                            + "\n" +
                            "* Aperçu du contenu : " + claim.getContent().substring(0,
                                    apercuContent.intValue())
                            + "...\n\n" +
                            "Nous vous invitons à rejoindre cette session afin de procéder à son traitement.";
                    Utils.sendmail(members, "Démarrage d'une session", message, null,
                            " ", settingServiceImpl);
                } catch (Exception e) {
                    if (e != null) {
                        Log log2 = Log
                                .builder()
                                .libelle("Echec mail notification")
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

        if (user.getAdditionalrole().equals(Role.MEMBRE_CGR) || user.getAdditionalrole().equals(Role.PR_CGR)) {
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
        } else {
            throw new Exception("Utilisateur non autorisé. Cette action sera répertoriée.");
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

        
        userRepository.save(guest);

        try {
            String message = "Cher " + guest.getFirstandlastname() + ", \n\n" +
                    "Vous êtes invité à intervenir dans les discussions à propos de la réclamation : "
                    + chat.getClaim().getCode() + " \n\n" +
                    "Connectez vous sur la plateforme GPRAssilassimé.";
            Utils.sendmail(guest.getEmail(), "Invitation chat", message, null, "", settingServiceImpl);
            Utils.sendSms(Arrays.asList(guest), message, settingServiceImpl);
        } catch (Exception e) {
            e.printStackTrace();
            // TODO: save in log
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
        if (guest.getChatsGuest().contains(chat)) {
            guest.getChatsGuest().remove(chat);
            guest.setUpdatedAt(LocalDateTime.now());
            userRepository.save(guest);
        }

        try {
            String message = "Cher " + guest.getFirstandlastname() + ", \n\n" +
                    "Vous avez exclus de la discussion sur le traitement de la réclamation : "
                    + chat.getClaim().getCode() + " \n\n" +
                    "Contactez le Président du Comité de Gestion des Réclamations s'il s'agit d'une erreur.";
            Utils.sendmail(guest.getEmail(), "Ejection du chat", message, null, "", settingServiceImpl);
            // Utils.sendSms(Arrays.asList(guest), message);
        } catch (Exception e) {
            e.printStackTrace();
            // TODO: save in log
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
