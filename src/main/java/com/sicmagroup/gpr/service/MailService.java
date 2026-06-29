package com.sicmagroup.gpr.service;

import java.util.List;
import java.util.Properties;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sicmagroup.gpr.api.config.setting.MailRequest;
import com.sicmagroup.gpr.domain.model.Setting;
import com.sicmagroup.gpr.domain.model.User;
import com.sicmagroup.gpr.service.setting.SettingServiceImpl;
import com.sicmagroup.gpr.utils.Constante;

import jakarta.mail.internet.MimeMessage;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

@Service
public class MailService {

    private final JavaMailSender mailSender;
    private final SettingServiceImpl settingServiceImpl;
    private final ObjectMapper objectMapper;

    public MailService(JavaMailSender mailSender, SettingServiceImpl settingServiceImpl) {
        this.mailSender = mailSender;
        this.settingServiceImpl = settingServiceImpl;
        this.objectMapper = new ObjectMapper();
    }

    @Async("taskExecutor") // Utilise le ThreadPool Spring
    public CompletableFuture<Void> sendMail(String to, String subject, String body, String cc) {
        try {
            Setting mailSetting = settingServiceImpl.getbySlug(Constante.MAIL_SLUG);
            if (mailSetting != null) {
                MailRequest mailRequest = objectMapper.readValue(mailSetting.getValue(), MailRequest.class);

                // Configure dynamiquement JavaMailSender si nécessaire
                if (mailSender instanceof JavaMailSenderImpl) {
                    JavaMailSenderImpl impl = (JavaMailSenderImpl) mailSender;
                    impl.setHost(mailRequest.getHost());
                    impl.setPort(Integer.parseInt(mailRequest.getPort()));
                    impl.setUsername(mailRequest.getUser());
                    impl.setPassword(mailRequest.getPwd());

                    int port = Integer.parseInt(mailRequest.getPort());
                    boolean useSSL = (port == 465);

                    Properties props = impl.getJavaMailProperties();
                    props.put("mail.transport.protocol", "smtp");
                    props.put("mail.smtp.auth", "true");
                    props.put("mail.smtp.ssl.enable", String.valueOf(useSSL));
                    props.put("mail.smtp.starttls.enable", String.valueOf(!useSSL));
                    props.put("mail.smtp.starttls.required", String.valueOf(!useSSL));
                    props.put("mail.smtp.ssl.trust", mailRequest.getHost());
                    // Timeouts pour éviter les connexions mortes après longue inactivité
                    props.put("mail.smtp.connectiontimeout", "10000");
                    props.put("mail.smtp.timeout", "10000");
                    props.put("mail.smtp.writetimeout", "10000");
                    // Désactiver le cache SSL pour éviter les sessions expirées
                    props.put("mail.smtp.ssl.sessioncachetime", "0");
                    props.put("mail.debug", "false");
                }

                // Création du mail HTML
                MimeMessage mimeMessage = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
                helper.setTo(to);
                helper.setSubject(subject);
                helper.setText(body, true);
                helper.setFrom(mailRequest.getUser());

                if (cc != null && !cc.isEmpty()) {
                    helper.setCc(cc.split(","));
                }

                mailSender.send(mimeMessage);
                System.out.println("✅ Mail HTML envoyé à " + to);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("❌ Erreur d’envoi de mail à " + to + " : " + e.getMessage());
        }

        return CompletableFuture.completedFuture(null);
    }

     @Async("taskExecutor")
    public CompletableFuture<Void> sendMail(List<User> usersTo, String subject, String body, String cc) {
        try {
            Setting mailSetting = settingServiceImpl.getbySlug(Constante.MAIL_SLUG);
            if (mailSetting != null) {
                MailRequest mailRequest = objectMapper.readValue(mailSetting.getValue(), MailRequest.class);

                // Configure dynamiquement JavaMailSender si nécessaire
                if (mailSender instanceof JavaMailSenderImpl) {
                    JavaMailSenderImpl impl = (JavaMailSenderImpl) mailSender;
                    impl.setHost(mailRequest.getHost());
                    if (mailRequest.getPort() != null && !mailRequest.getPort().isEmpty()) {
                        impl.setPort(Integer.parseInt(mailRequest.getPort()));
                    }
                    impl.setUsername(mailRequest.getUser());
                    impl.setPassword(mailRequest.getPwd());

                    int port2 = (mailRequest.getPort() != null && !mailRequest.getPort().isEmpty())
                            ? Integer.parseInt(mailRequest.getPort()) : 587;
                    boolean useSSL2 = (port2 == 465);

                    Properties props = impl.getJavaMailProperties();
                    props.put("mail.transport.protocol", "smtp");
                    props.put("mail.smtp.auth", "true");
                    props.put("mail.smtp.ssl.enable", String.valueOf(useSSL2));
                    props.put("mail.smtp.starttls.enable", String.valueOf(!useSSL2));
                    props.put("mail.smtp.starttls.required", String.valueOf(!useSSL2));
                    props.put("mail.smtp.ssl.trust", mailRequest.getHost());
                    props.put("mail.smtp.connectiontimeout", "10000");
                    props.put("mail.smtp.timeout", "10000");
                    props.put("mail.smtp.writetimeout", "10000");
                    props.put("mail.smtp.ssl.sessioncachetime", "0");
                    props.put("mail.debug", "false");
                    impl.setJavaMailProperties(props);
                }

                // Récupérer les emails des utilisateurs
                List<String> emails = usersTo.stream()
                                             .map(User::getEmail)
                                             .collect(Collectors.toList());

                // Création du MimeMessage HTML
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setTo(emails.toArray(new String[0]));
                helper.setSubject(subject);
                helper.setText(body, true); // HTML
                helper.setFrom(mailRequest.getUser());

                if (cc != null && !cc.isEmpty()) {
                    helper.setCc(cc.split(","));
                }

                mailSender.send(message);
                System.out.println("✅ Mail HTML envoyé à " + emails);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("❌ Erreur d’envoi de mail : " + e.getMessage());
        }

        return CompletableFuture.completedFuture(null);
    }


}

