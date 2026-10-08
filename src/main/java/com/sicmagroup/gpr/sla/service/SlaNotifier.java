package com.sicmagroup.gpr.sla.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.sicmagroup.gpr.domain.enumeration.ClaimType;
import com.sicmagroup.gpr.domain.model.Claim;
import com.sicmagroup.gpr.domain.model.User;
import com.sicmagroup.gpr.service.MailService;
import com.sicmagroup.gpr.service.setting.SettingServiceImpl;
import com.sicmagroup.gpr.sla.service.SlaMessages.Line;
import com.sicmagroup.gpr.utils.Utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Envoi des notifications SLA par e-mail (et SMS pour les retards et remontées). Les envois partent après la
 * validation de la transaction : si elle est annulée, rien n'est envoyé. Un échec d'envoi (réglage mail ou
 * SMS absent par exemple) est noté dans les journaux et ne bloque jamais le SLA.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SlaNotifier {

    private final MailService mailService;
    private final SettingServiceImpl settingService;
    private final SlaHierarchy hierarchy;

    /** Destinataires d'un message : à (principal), copie, et personnes à prévenir par SMS. */
    public record Recipients(List<User> to, List<User> cc) {
        public static Recipients of(List<User> to, List<User> cc) {
            return new Recipients(distinct(to), distinct(cc));
        }

        private static List<User> distinct(List<User> users) {
            Map<Long, User> byId = new LinkedHashMap<>();
            if (users != null) {
                for (User u : users) {
                    if (u != null && u.getEmail() != null && !u.getEmail().isBlank()) {
                        byId.putIfAbsent(u.getId(), u);
                    }
                }
            }
            return new ArrayList<>(byId.values());
        }
    }

    /** Le responsable du dossier, avec le RA de l'agence en copie. Sans responsable : le RA seul. */
    public Recipients ownerAndRa(Claim claim, User owner) {
        User ra = hierarchy.agencyRa(claim);
        if (owner == null) {
            return Recipients.of(ra == null ? List.of() : List.of(ra), List.of());
        }
        return Recipients.of(List.of(owner), ra == null ? List.of() : List.of(ra));
    }

    public Recipients regulatory(Claim claim, User owner) {
        List<User> to = new ArrayList<>();
        if (owner != null) {
            to.add(owner);
        }
        User ra = hierarchy.agencyRa(claim);
        if (ra != null) {
            to.add(ra);
        }
        List<User> cc = new ArrayList<>(hierarchy.pilotes());
        cc.addAll(hierarchy.des());
        return Recipients.of(to, cc);
    }

    /** Envoie un e-mail à chaque destinataire principal, les autres en copie. */
    public void mail(Recipients recipients, String subject, java.util.function.Function<String, String> bodyFor) {
        if (recipients.to().isEmpty()) {
            return;
        }
        String cc = recipients.cc().stream().filter(u -> recipients.to().stream().noneMatch(t -> t.getId().equals(u.getId())))
                .map(User::getEmail).collect(Collectors.joining(","));
        for (User to : recipients.to()) {
            String body = bodyFor.apply(greeting(to));
            afterCommit(() -> mailService.sendMail(to.getEmail().trim(), subject, body, cc.isBlank() ? null : cc));
        }
    }

    public void sms(List<User> users, String message) {
        List<User> withPhone = users.stream().filter(u -> u != null && u.getTel() != null && !u.getTel().isBlank())
                .collect(Collectors.toList());
        if (withPhone.isEmpty()) {
            return;
        }
        afterCommit(() -> {
            try {
                Utils.sendSms(withPhone, message, settingService);
            } catch (Exception e) {
                log.warn("SLA : SMS non envoyé : {}", e.toString());
            }
        });
    }

    /**
     * Message d'attente au client d'une réclamation en retard. Renvoie vrai si au moins un canal est utilisable
     * (e-mail ou téléphone renseigné). Jamais pour une dénonciation.
     */
    public boolean clientWaitingMessage(Claim claim) {
        if (claim == null || claim.getType() != ClaimType.CLAIM) {
            return false;
        }
        String text = SlaMessages.clientWaiting(claim.getCodeClient());
        boolean attempted = false;
        String email = claim.getEmail();
        if (email != null && !email.isBlank()) {
            attempted = true;
            afterCommit(() -> mailService.sendMail(email.trim(), "Votre réclamation " + claim.getCodeClient(),
                    "<p>" + text + "</p>", null));
        }
        String tel = claim.getTel();
        if (tel != null && !tel.isBlank()) {
            attempted = true;
            afterCommit(() -> {
                try {
                    Utils.sendSms(tel.trim(), text, settingService);
                } catch (Exception e) {
                    log.warn("SLA : SMS d'attente non envoyé : {}", e.toString());
                }
            });
        }
        return attempted;
    }

    private static String greeting(User user) {
        return "Bonjour " + (user.getFirstandlastname() == null ? "" : user.getFirstandlastname());
    }

    private void afterCommit(Runnable action) {
        Runnable safe = () -> {
            try {
                action.run();
            } catch (Exception e) {
                log.warn("SLA : notification non envoyée : {}", e.toString());
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    safe.run();
                }
            });
        } else {
            safe.run();
        }
    }

    /** Ligne de message pour une plainte (jamais d'identité du client). */
    public static Line line(Claim claim, java.time.LocalDateTime dueAt, String detail) {
        boolean denunciation = claim.getType() == ClaimType.DENUNCIACION;
        String code = denunciation ? claim.getCode() : claim.getCodeClient();
        return new Line(claim.getType(), code,
                claim.getObjet() == null ? null : claim.getObjet().getLibelle(),
                claim.getServicePoint() == null ? null : claim.getServicePoint().getLibelle(), dueAt, detail);
    }
}
