package com.sicmagroup.gpr.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.annotation.Async;
import org.yaml.snakeyaml.util.UriEncoder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sicmagroup.gpr.api.config.setting.MailRequest;
import com.sicmagroup.gpr.api.config.setting.SmsRequest;
import com.sicmagroup.gpr.domain.dto.ApiResponseDto;
import com.sicmagroup.gpr.domain.dto.ErrorResponse;
import com.sicmagroup.gpr.domain.dto.LicenceControl;
import com.sicmagroup.gpr.domain.dto.LicenceDto;
import com.sicmagroup.gpr.domain.dto.reports.RgbColor;
import com.sicmagroup.gpr.domain.enumeration.ClaimType;
import com.sicmagroup.gpr.domain.enumeration.LogTarget;
import com.sicmagroup.gpr.domain.enumeration.LogType;
import com.sicmagroup.gpr.domain.model.Log;
import com.sicmagroup.gpr.domain.model.Setting;
import com.sicmagroup.gpr.domain.model.User;
import com.sicmagroup.gpr.service.setting.SettingServiceImpl;

public class Utils {

    // public static generateCode(String initial, )
    public static String guessFileExtension(String dataString) {
        String type = "";
        if (dataString.contains("application/pdf")) {
            type = "pdf";
        }
        if (dataString.contains("application/vnd.ms-excel")) {
            type = "xls";
        }
        if (dataString.contains("text/csv")) {
            type = "csv";
        }
        if (dataString.contains("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) {
            type = "xlsx";
        }
        if (dataString.contains("application/vnd.openxmlformats-officedocument.wordprocessingml.document")) {
            type = "docx";
        }
        if (dataString.contains("application/msword")) {
            type = "doc";
        }
        if (dataString.contains("image/vnd.microsoft.icon")) {
            type = "ico";
        }
        if (dataString.contains("image/gif")) {
            type = "gif";
        }
        if (dataString.contains("image/jpeg")) {
            type = "jpg";
        }
        if (dataString.contains("image/png")) {
            type = "png";
        }
        if (dataString.contains("image/*")) {
            type = "jpg";
        }
        if (dataString.contains("audio/ogg")) {
            type = "ogg";
        }
        if (dataString.contains("audio/*")) {
            type = "mp3";
        }
        if (dataString.contains("video/*")) {
            type = "mp4";
        }
        if (dataString.contains("application/x-sql")) {
            type = "sql";
        }
        if (dataString.contains("application/x-httpd-php")) {
            type = "php";
        }
        if (dataString.contains("application/javascript")) {
            type = "js";
        }
        if (dataString.contains("application/rtf")) {
            type = "rtf";
        }
        return type;
    }

    @Async
    public static Future<String> sendmail(String to, String subject, String body, String cc, String from,
            SettingServiceImpl settingServiceImpl) {
        SimpleMailMessage message = new SimpleMailMessage();
        JavaMailSenderImpl mailSenderr = new JavaMailSenderImpl();
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            Setting mail = settingServiceImpl.getbySlug(Constante.MAIL_SLUG);
            if (mail != null) {
                MailRequest mailRequest = objectMapper.readValue(mail.getValue(), MailRequest.class);
                // System.out.println("try b");
                mailSenderr.setHost(mailRequest.getHost());
                mailSenderr.setPort(Integer.parseInt(mailRequest.getPort()));
                mailSenderr.setUsername(mailRequest.getUser());
                mailSenderr.setPassword(mailRequest.getPwd());

                Properties props = mailSenderr.getJavaMailProperties();
                props.put("mail.transport.protocol", "smtp");
                props.put("mail.smtp.auth", "true");
                props.put("mail.smtp.ssl.enable", "true");
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.debug", "true");

                if (cc != "" && cc != null) {
                    String[] listCc = cc.split(",");
                    message.setCc(listCc);
                }

                message.setTo(to);
                message.setSubject(subject);
                message.setText(body);
                message.setFrom(mailRequest.getUser());

                mailSenderr.send(message);
            }

        } catch (Exception e) {

            e.printStackTrace();
        }

        return null;

    }

    @Async
    public static Future<String> sendmail(List<User> usersTo, String subject, String body, String cc, String from,
            SettingServiceImpl settingServiceImpl) throws Exception {
        SimpleMailMessage message = new SimpleMailMessage();
        JavaMailSenderImpl mailSenderr = new JavaMailSenderImpl();
        ObjectMapper objectMapper = new ObjectMapper();
        // from = "darrell.kidjo@sicmagroup.com";
        try {

            Setting mail = settingServiceImpl.getbySlug(Constante.MAIL_SLUG);
            if (mail != null) {

                MailRequest mailRequest = objectMapper.readValue(mail.getValue(), MailRequest.class);
                // System.out.println("try b");
                mailSenderr.setHost(mailRequest.getHost());
                mailSenderr.setPort(Integer.parseInt(mailRequest.getPort()));
                mailSenderr.setUsername(mailRequest.getUser());
                mailSenderr.setPassword(mailRequest.getPwd());

                Properties props = mailSenderr.getJavaMailProperties();
                props.put("mail.transport.protocol", "smtp");
                props.put("mail.smtp.auth", "true");
                props.put("mail.smtp.ssl.enable", "true");
                props.put("mail.debug", "true");
                mailSenderr.setJavaMailProperties(props);

                if (cc != "" && cc != null) {
                    String[] listCc = cc.split(",");
                    message.setCc(listCc);
                }
                List<String> emails = usersTo.stream().map(user -> user.getEmail()).collect(Collectors.toList());
                System.out.println(emails);
                System.out.println(emails.iterator().next());
                String[] recipients = emails.toArray(new String[0]);
                System.out.println("recipients");

                message.setTo(recipients);
                message.setSubject(subject);
                message.setText(body);
                message.setFrom(mailRequest.getUser());
                mailSenderr.send(message);
            }
        } catch (Exception e) {
            throw e;
            // e.printStackTrace();

        }

        return null;
    }

    public static String convertLocalDateTimeToStr(LocalDateTime dateTime) {
        String resultat;
        // Définir un formateur de date personnalisé
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy - HH:mm", Locale.FRENCH);

        // Formater la date et l'heure
        resultat = dateTime.format(formatter);

        return resultat;
    }

    public static String convertLocalDateTimeToString(LocalDateTime dateTime) {
        String resultat;
        // Définir un formateur de date personnalisé
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm", Locale.FRENCH);

        // Formater la date et l'heure
        resultat = dateTime.format(formatter);

        return resultat;
    }

    public static String convertLocalDateToString(LocalDateTime dateTime) {
        String resultat;
        // Définir un formateur de date personnalisé
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.FRENCH);

        // Formater la date et l'heure
        resultat = dateTime.format(formatter);

        return resultat;
    }

    public static LocalDateTime convertStrToLocalDateTime(String datetime) {
        try {
            // Définir le format de la chaîne de caractères de date et heure
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

            // Analyser la chaîne de caractères en LocalDateTime
            LocalDateTime parsedDateTime = LocalDateTime.parse(datetime, formatter);

            return parsedDateTime;
        } catch (DateTimeParseException e) {
            // Gérer l'exception si la conversion échoue
            throw new IllegalArgumentException("Impossible de convertir la chaîne en LocalDateTime.", e);
        }
    }

    public static LocalDateTime convertStrWithTToLocalDateTime(String datetime) {
        try {
            // Définir le format de la chaîne de caractères de date et heure
            // DateTimeFormatter formatter =
            // DateTimeFormatter.ofPattern("dd-MM-yyyy'T'HH:mm:ss");

            // Analyser la chaîne de caractères en LocalDateTime
            LocalDateTime parsedDateTime = LocalDateTime.parse(datetime);

            return parsedDateTime;
        } catch (DateTimeParseException e) {
            // Gérer l'exception si la conversion échoue
            throw new IllegalArgumentException("Impossible de convertir la chaîne en LocalDateTime.", e);
        }
    }

    @Async
    public static Future<Boolean> sendSms(List<User> usersTo, String message, SettingServiceImpl settingServiceImpl)
            throws Exception {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            Setting sms = settingServiceImpl.getbySlug(Constante.SMS_SLUG);
            if (sms != null) {
                SmsRequest smsRequest = objectMapper.readValue(sms.getValue(), SmsRequest.class);
                String baseUrl = smsRequest.getUrl();// "http://www.wassasms.com/wassasms/api/web/v3/sends?";
                String token = smsRequest.getValMdp();// "SZhs_fSrSqDn8eITgs77ym17ttv1G8ig";
                String sender = smsRequest.getValEmetteur();// "gps";
                String dlrUrl = "";
                message = UriEncoder.encode(message);
                HttpURLConnection con;
                for (User to : usersTo) {

                    baseUrl += smsRequest.getLibMdp() + "=" + token + "&" + smsRequest.getLibEmetteur() + "=" + sender
                            + "&"
                            + smsRequest.getLibDestinataire() + "=" + to.getTel() + "&" + smsRequest.getLibMessage()
                            + "="
                            + message;
                    URL url = new URL(baseUrl);
                    con = (HttpURLConnection) url.openConnection();
                    con.setRequestMethod("GET");
                    int status = con.getResponseCode();
                    if (status >= 200 && status <= 299) {
                        BufferedReader in = new BufferedReader(
                                new InputStreamReader(con.getInputStream()));
                        String inputLine;
                        StringBuffer content = new StringBuffer();
                        while ((inputLine = in.readLine()) != null) {
                            content.append(inputLine);
                        }
                        in.close();
                        System.out.println("sms send");
                        System.out.println(content);
                    } else {
                        BufferedReader in = new BufferedReader(
                                new InputStreamReader(con.getErrorStream()));
                        String inputLine;
                        StringBuffer content = new StringBuffer();
                        while ((inputLine = in.readLine()) != null) {
                            content.append(inputLine);
                        }
                        in.close();
                        System.out.println("sms not send");
                        System.out.println(content);

                    }
                }
            }

        } catch (IOException e) {
            throw e;
        } catch (Exception ex) {
            throw ex;
        }

        return null;
    }

    @Async
    public static Future<Boolean> sendSms(String number, String message, SettingServiceImpl settingServiceImpl)
            throws Exception {

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            Setting sms = settingServiceImpl.getbySlug(Constante.SMS_SLUG);
            if (sms != null) {
                SmsRequest smsRequest = objectMapper.readValue(sms.getValue(), SmsRequest.class);
                String baseUrl = smsRequest.getUrl();// "http://www.wassasms.com/wassasms/api/web/v3/sends?";
                String token = smsRequest.getValMdp();// "SZhs_fSrSqDn8eITgs77ym17ttv1G8ig";
                String sender = smsRequest.getValEmetteur();// "gps";
                String dlrUrl = "";
                message = UriEncoder.encode(message);
                HttpURLConnection con;

                baseUrl += smsRequest.getLibMdp() + "=" + token + "&" + smsRequest.getLibEmetteur() + "=" + sender + "&"
                        + smsRequest.getLibDestinataire() + "=" + number + "&" + smsRequest.getLibMessage() + "="
                        + message;
                URL url = new URL(baseUrl);
                con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");
                int status = con.getResponseCode();
                if (status >= 200 && status <= 299) {
                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(con.getInputStream()));
                    String inputLine;
                    StringBuffer content = new StringBuffer();
                    while ((inputLine = in.readLine()) != null) {
                        content.append(inputLine);
                    }
                    in.close();
                    System.out.println("sms send");
                    System.out.println(content);
                } else {
                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(con.getErrorStream()));
                    String inputLine;
                    StringBuffer content = new StringBuffer();
                    while ((inputLine = in.readLine()) != null) {
                        content.append(inputLine);
                    }
                    in.close();
                    System.out.println("sms not send");
                    System.out.println(content);

                }
            }

        } catch (IOException e) {
            throw e;
        } catch (Exception ex) {
            throw ex;
        }

        return null;
    }

    public static Double percentCalculator(Long value, Long total) {
        if (total != 0) {
            return (((Double) value.doubleValue() / total) * 100);
        }
        return 0.0;

    }

    public static Double parseDouble(Double toParse) {
        DecimalFormat decimalFormat = new DecimalFormat("#.##");
        String str = decimalFormat.format(toParse);
        System.out.println(str);
        return Double.parseDouble(str.replace(",", "."));
    }

    public static List<RgbColor> generateRandomColor(int total) {
        List<RgbColor> listColor = new ArrayList<>();
        Random random = new Random();
        for (int i = 0; i < total; i++) {
            listColor.add(new RgbColor(random.nextInt(250), random.nextInt(225),
                    random.nextInt(200)));
        }
        return listColor;
    }

    public static List<RgbColor> generateColorsFromColors(List<RgbColor> bgColors) {
        List<RgbColor> listColor = new ArrayList<>();
        for (RgbColor rgbColor : listColor) {
            listColor.add(new RgbColor(rgbColor.getR() == 250 ? rgbColor.getR() - 7 : rgbColor.getR() + 7,
                    rgbColor.getG() + 7,
                    rgbColor.getB() + 7));
        }

        return listColor;
    }

    public static ApiResponseDto verifyLicence() {
        String license = "";
        ApiResponseDto apiResponseDto = ApiResponseDto
                .builder()

                .build();
        try {
            // Le fichier d'entrée
            File file = new File("data.txt");
            // Créer l'objet File Reader
            FileReader fr = new FileReader(file);
            // Créer l'objet BufferedReader
            BufferedReader br = new BufferedReader(fr);
            StringBuffer sb = new StringBuffer();
            String line;
            while ((line = br.readLine()) != null) {
                // ajoute la ligne au buffer
                sb.append(line);
                sb.append("\n");
            }
            fr.close();

            license = sb.toString();

            if (license != "") {
                LicenceControl licenceControl = LicenceControl
                        .builder()

                        .build();
                ObjectMapper mapper = new ObjectMapper();
                LicenceDto licenseResponse = mapper.readValue(license, LicenceDto.class);

                String activationRequest = licenseResponse.getActivationRequest();
                String[] splitARequest = activationRequest.split(",");
                String[] splitInfo = splitARequest[1].split(":");
                int totalJours = Integer.parseInt(splitInfo[0]);

                LocalDateTime calculateDate = licenseResponse.getCreatedAt().plusDays(totalJours);
                Long hoursRetard = calculateDate.until(LocalDateTime.now(), ChronoUnit.HOURS);

                if (hoursRetard > 0) {
                    // La licence n'est pas encore expirée, hoursRetard contient le nombre d'heures.
                    long daysRemaining = hoursRetard / 24; // Convertir les heures en jours

                    licenceControl.setActif(true);
                    licenceControl.setDayBefore(daysRemaining);
                    licenceControl.setMaxPoste(Long.parseLong(splitInfo[1]));
                    Double consommation = (totalJours * 0.3);
                    if (daysRemaining <= (consommation.longValue()))
                        licenceControl
                                .setMessage("Il reste " + daysRemaining + " jr(s) avant expiration de la licence");
                    else
                        licenceControl.setMessage("");
                    apiResponseDto.setStatus(true);
                    apiResponseDto.setContent(licenceControl);

                } else {
                    // La licence est expirée, hoursRetard contient le nombre d'heures restantes.
                    long daysElapsed = Math.abs(hoursRetard) / 24; // Convertir les heures en jours

                    licenceControl.setActif(false);
                    licenceControl.setDayBefore(-daysElapsed);
                    licenceControl.setMaxPoste(Long.parseLong(splitInfo[1]));
                    licenceControl.setMessage("Votre licence à expirer depuis " + daysElapsed + " jr(s).");
                    apiResponseDto.setStatus(true);
                    apiResponseDto.setContent(licenceControl);

                    apiResponseDto.setStatus(true);
                    apiResponseDto.setContent(licenceControl);
                }

            } else {

                apiResponseDto.setStatus(false);
                apiResponseDto.setContent(ErrorResponse.builder().title("Erreur aucune licence active")
                        .message("Erreur aucune licence active").build());

            }

        } catch (IOException e) {

            apiResponseDto = ApiResponseDto
                    .builder()
                    .status(false)
                    .content(ErrorResponse.builder().message(
                            "Une erreur est survenue à la lecture du fichier")
                            .title("Une erreur est survenue à la lecture du fichier")
                            .build())
                    .build();

            e.printStackTrace();

        }

        return apiResponseDto;
    }
}
