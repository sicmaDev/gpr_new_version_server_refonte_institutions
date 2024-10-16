package com.sicmagroup.gpr.job;

import com.sicmagroup.gpr.domain.model.Log;
import com.sicmagroup.gpr.domain.model.User;
import com.sicmagroup.gpr.service.log.LogServiceImpl;
import com.sicmagroup.gpr.service.setting.SettingServiceImpl;
import com.sicmagroup.gpr.utils.Utils;


import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

@Component
public class NotificationTasklet implements Tasklet {
    private final LogServiceImpl logServiceImpl;
    private final SettingServiceImpl settingServiceImpl;

    // Constructor injection
    public NotificationTasklet(LogServiceImpl logServiceImpl, SettingServiceImpl settingServiceImpl) {
        this.logServiceImpl = logServiceImpl;
        this.settingServiceImpl = settingServiceImpl;
    }

    @Override
    public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
        // Récupérer les informations du contexte
        Map<String, Object> jobParameters = chunkContext.getStepContext().getJobParameters();
        
        // Récupérer les destinataires et le message depuis les paramètres
        List<User> usersToContact = (List<User>) jobParameters.get("usersToContact");
        String message = (String) jobParameters.get("message");

        // Envoyer les notifications (email ou SMS)
        try {
            Utils.sendmail(usersToContact, "Notification", message, null, " ", settingServiceImpl);
        } catch (Exception e) {
            // logError("Echec mail notification", e.getMessage());
        }

        try {
            Utils.sendSms(usersToContact, message, settingServiceImpl);
        } catch (Exception e) {
            // logError("Echec SMS notification", e.getMessage());
        }

        return RepeatStatus.FINISHED;
    }

    // private void logError(String libelle, String message) {
    //     Log log = Log.builder()
    //             .libelle(libelle)
    //             .content(message)
    //             .createdAt(LocalDateTime.now())
    //             .type(LogType.ERROR)
    //             .userId(0L)
    //             .build();
    //     logServiceImpl.saveLog(log);
    // }
}

