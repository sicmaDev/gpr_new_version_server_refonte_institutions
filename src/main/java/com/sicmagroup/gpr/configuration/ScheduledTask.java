package com.sicmagroup.gpr.configuration;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.sicmagroup.gpr.domain.dto.AlertDto;
import com.sicmagroup.gpr.domain.enumeration.ClaimType;
import com.sicmagroup.gpr.domain.enumeration.LogTarget;
import com.sicmagroup.gpr.domain.enumeration.LogType;
import com.sicmagroup.gpr.domain.enumeration.Role;
import com.sicmagroup.gpr.domain.model.Log;
import com.sicmagroup.gpr.domain.model.User;
import com.sicmagroup.gpr.service.auth.AuthenticationServiceImpl;
import com.sicmagroup.gpr.service.claim.ClaimServiceImpl;
import com.sicmagroup.gpr.service.log.LogServiceImpl;
import com.sicmagroup.gpr.service.setting.SettingServiceImpl;
import com.sicmagroup.gpr.utils.Utils;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ScheduledTask {

    private final ClaimServiceImpl claimService;
    private final AuthenticationServiceImpl authServiceImpl;
    private final LogServiceImpl logServiceImpl;
    private final SettingServiceImpl settingServiceImpl;

    @Scheduled(fixedDelay = 86400000)
    public void alertNotifier() {

        // claim
        List<AlertDto> claimAlertDtos = claimService.getAllAlertDtosByType(ClaimType.CLAIM);
        List<Role> roles = new ArrayList<>(Arrays.asList(Role.PILOTE, Role.MEMBRE_CGR, Role.PR_CGR));
        List<User> usersToContact = authServiceImpl.getUsersByRoles(roles);

        // Envoi de mail en parallèle
        CompletableFuture.runAsync(() -> {
            try {
                Utils.sendmail(usersToContact, " Notification retard de traitement", claimAlertDtos.size()
                        + " Réclamation(s) ont un retard de traitement. Connectez-vous à la plateforme de GPR pour proposer des solutions adéquates à ces réclamations.",
                        null,
                        " ", settingServiceImpl);
                                        
                Log successLog = Log.builder()
                    .libelle("Mail notification Réclamation Alerte notification")
                    .content("Success mail notification Réclamation Alerte notification")
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
                            .libelle("Echec mail Réclamation Alerte notification")
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

        // denunciation
        List<AlertDto> denunciationAlerts = claimService.getAllAlertDtosByType(ClaimType.DENUNCIACION);

        // Envoi de mail en parallèle
        CompletableFuture.runAsync(() -> {
            try {
                Utils.sendmail(usersToContact, " Notification retard de traitement", denunciationAlerts.size()
                        + " Dénonciation(s) ont un retard de traitement. Connectez-vous à la plateforme de GPR pour proposer des solutions adéquates à ces dénonciations.",
                        null,
                        " ", settingServiceImpl);
            } catch (Exception e) {
                Log log2 = Log
                        .builder()
                        .libelle("Echec mail Dénonciation Alerte notification")
                        .content(e.getMessage())
                        .createdAt(LocalDateTime.now())
                        .type(LogType.ERROR)
                        .userId(0L)
                        .userIpAddress(null)
                        .target(LogTarget.APP)
                        .build();
    
                logServiceImpl.saveLog(log2);
            }
        });
    }
}
