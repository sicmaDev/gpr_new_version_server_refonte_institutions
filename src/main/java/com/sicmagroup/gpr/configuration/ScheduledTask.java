package com.sicmagroup.gpr.configuration;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
        try {
            Utils.sendmail(usersToContact, " Notification retard de traitement", claimAlertDtos.size()
                    + " Réclamations ont un retard de traitement. Connectez-vous à la plateforme de gps pour proposer des solutions adéquates à ces réclamations.",
                    null,
                    " ", settingServiceImpl);
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

        // denunciation
        claimAlertDtos = claimService.getAllAlertDtosByType(ClaimType.DENUNCIACION);
        try {
            Utils.sendmail(usersToContact, " Notification retard de traitement", claimAlertDtos.size()
                    + " Dénonciation ont un retard de traitement. Connectez-vous à la plateforme de gps pour proposer des solutions adéquates à ces dénonciations.",
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

    }
}
