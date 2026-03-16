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

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.yaml.snakeyaml.util.UriEncoder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sicmagroup.gpr.api.config.setting.EmailRequest;
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

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.HttpServletRequest;

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

    public static Boolean testSmsConfig(String number, String message, SettingServiceImpl settingServiceImpl)
            throws Exception {

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            Setting sms = settingServiceImpl.getbySlug(Constante.SMS_SLUG);
            if (sms != null) {
                SmsRequest smsRequest = objectMapper.readValue(sms.getValue(), SmsRequest.class);
                String baseUrl = smsRequest.getUrl();
                String token = smsRequest.getValMdp();
                String sender = smsRequest.getValEmetteur();
                String account = smsRequest.getValId();
                message = UriEncoder.encode(message);
                HttpURLConnection con;

                baseUrl += smsRequest.getLibId() + "=" + account + "&" + smsRequest.getLibMdp() + "=" + token + "&" + smsRequest.getLibEmetteur() + "=" + sender + "&"
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

                    return true;

                } else {
                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(con.getErrorStream()));
                    String inputLine;
                    StringBuffer content = new StringBuffer();
                    while ((inputLine = in.readLine()) != null) {
                        content.append(inputLine);
                    }
                    in.close();
                    return false;

                }
            }
            return false;

        } catch (IOException e) {

            return false;
        } catch (Exception ex) {
            return false;
        }
    }

    public static Boolean sendSmsToClient(String number, String message, SettingServiceImpl settingServiceImpl) throws Exception {
        try {
          
            ObjectMapper objectMapper = new ObjectMapper();
            Setting sms = settingServiceImpl.getbySlug(Constante.SMS_SLUG);
            if (sms != null) {
                SmsRequest smsRequest = objectMapper.readValue(sms.getValue(), SmsRequest.class);
                String baseUrl = smsRequest.getUrl();
                String token = smsRequest.getValMdp();
                String sender = smsRequest.getValEmetteur();
                String account = smsRequest.getValId();
                message = UriEncoder.encode(message);
                HttpURLConnection con;

                baseUrl += smsRequest.getLibId() + "=" + account + "&" + smsRequest.getLibMdp() + "=" + token + "&" + smsRequest.getLibEmetteur() + "=" + sender + "&"
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

                    return true;

                } else {
                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(con.getErrorStream()));
                    String inputLine;
                    StringBuffer content = new StringBuffer();
                    while ((inputLine = in.readLine()) != null) {
                        content.append(inputLine);
                    }
                    in.close();
                    return false;

                }
            }
            return false;

        } catch (IOException e) {

            return false;
        } catch (Exception ex) {
            return false;
        }
    }

    
    
    public static Boolean testMailConfig(String to, String subject, String body, String cc, String from,
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
                return true;
            } else {
                return false;
            }

        } catch (MailException ex) {
            return false;

        } catch (Exception e) {

            return false;
        }

    }

    // @Async
    // public static Future<String> sendmail(String to, String subject, String body, String cc, String from,
    //     SettingServiceImpl settingServiceImpl) {

    //     JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
    //     ObjectMapper objectMapper = new ObjectMapper();

    //     try {
    //         Setting mail = settingServiceImpl.getbySlug(Constante.MAIL_SLUG);
    //         if (mail != null) {
    //             MailRequest mailRequest = objectMapper.readValue(mail.getValue(), MailRequest.class);

    //             mailSender.setHost(mailRequest.getHost());
    //             mailSender.setPort(Integer.parseInt(mailRequest.getPort()));
    //             mailSender.setUsername(mailRequest.getUser());
    //             mailSender.setPassword(mailRequest.getPwd());

    //             Properties props = mailSender.getJavaMailProperties();
    //             props.put("mail.transport.protocol", "smtp");
    //             props.put("mail.smtp.auth", "true");
    //             props.put("mail.smtp.ssl.enable", "true");
    //             props.put("mail.smtp.starttls.enable", "true");
    //             props.put("mail.debug", "true");

    //             // Création du message MIME (supporte HTML)
    //             MimeMessage mimeMessage = mailSender.createMimeMessage();
    //             MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

    //             helper.setTo(to);
    //             helper.setSubject(subject);
    //             helper.setText(body, true); // <<=== HTML activé ici
    //             helper.setFrom(mailRequest.getUser());

    //             if (cc != null && !cc.isEmpty()) {
    //                 String[] listCc = cc.split(",");
    //                 helper.setCc(listCc);
    //             }

    //             mailSender.send(mimeMessage);
    //             System.out.println("✅ Mail HTML envoyé à " + to);
    //         }

    //     } catch (Exception e) {
    //         e.printStackTrace();
    //         System.out.println("❌ Erreur d’envoi de mail à " + to + " : " + e.getMessage());
    //     }

    //     return null;
    // }

    // @Async
    // public static Future<String> sendmail(List<User> usersTo, String subject, String body, String cc, String from,
    //         SettingServiceImpl settingServiceImpl) throws Exception {

    //     JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
    //     ObjectMapper objectMapper = new ObjectMapper();

    //     try {
    //         Setting mail = settingServiceImpl.getbySlug(Constante.MAIL_SLUG);
    //         if (mail != null) {
    //             MailRequest mailRequest = objectMapper.readValue(mail.getValue(), MailRequest.class);

    //             mailSender.setHost(mailRequest.getHost());
    //             if (!mailRequest.getPort().isEmpty()) {
    //                 mailSender.setPort(Integer.parseInt(mailRequest.getPort()));
    //             }
    //             mailSender.setUsername(mailRequest.getUser());
    //             mailSender.setPassword(mailRequest.getPwd());

    //             Properties props = mailSender.getJavaMailProperties();
    //             props.put("mail.transport.protocol", "smtp");
    //             props.put("mail.smtp.auth", "true");
    //             props.put("mail.smtp.ssl.enable", "true");
    //             props.put("mail.smtp.starttls.enable", "true");
    //             props.put("mail.debug", "true");
    //             mailSender.setJavaMailProperties(props);

    //             // Récupérer les emails des utilisateurs
    //             List<String> emails = usersTo.stream().map(User::getEmail).collect(Collectors.toList());

    //             // Créer le MimeMessage pour HTML
    //             MimeMessage message = mailSender.createMimeMessage();
    //             MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

    //             helper.setTo(emails.toArray(new String[0]));
    //             helper.setSubject(subject);
    //             helper.setText(body, true); // <-- true pour indiquer que c'est du HTML
    //             helper.setFrom(mailRequest.getUser());

    //             if (cc != null && !cc.isEmpty()) {
    //                 helper.setCc(cc.split(","));
    //             }

    //             mailSender.send(message);
    //         }
    //     } catch (Exception e) {
    //         throw e;
    //     }

    //     return null;
    // }

    
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
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm", Locale.FRENCH);

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
            // System.out.println("SMS config: " + sms.getValue());
            // System.out.println("SMS config2: " + usersTo.size());

            if (sms != null) {
                SmsRequest smsRequest = objectMapper.readValue(sms.getValue(), SmsRequest.class);
                String baseUrl = smsRequest.getUrl();
                String token = smsRequest.getValMdp();
                String sender = smsRequest.getValEmetteur();
                String account = smsRequest.getValId();
                 
                message = UriEncoder.encode(message);
                HttpURLConnection con;
                for (User to : usersTo) {
                    String urlStr = baseUrl + smsRequest.getLibId() + "=" + account 
                            + "&" + smsRequest.getLibMdp() + "=" + token
                            + "&" + smsRequest.getLibEmetteur() + "=" + sender
                            + "&" + smsRequest.getLibDestinataire() + "=" + to.getTel()
                            + "&" + smsRequest.getLibMessage() + "=" + message;

                    URL url = new URL(urlStr);
                   
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
                String baseUrl = smsRequest.getUrl();
                String token = smsRequest.getValMdp();
                String sender = smsRequest.getValEmetteur();
                String account = smsRequest.getValId();
              
                message = UriEncoder.encode(message);
                HttpURLConnection con;
                String urlStr = baseUrl + smsRequest.getLibId() + "=" + account 
                            + "&" + smsRequest.getLibMdp() + "=" + token
                            + "&" + smsRequest.getLibEmetteur() + "=" + sender
                            + "&" + smsRequest.getLibDestinataire() + "=" + number
                            + "&" + smsRequest.getLibMessage() + "=" + message;
              
                URL url = new URL(urlStr);
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

    public static String generateRandomString(int count) {
        String SALTCHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        StringBuilder salt = new StringBuilder();
        Random rnd = new Random();
        while (salt.length() < count) { // length of the random string.
            int index = (int) (rnd.nextFloat() * SALTCHARS.length());
            salt.append(SALTCHARS.charAt(index));
        }
        String saltStr = salt.toString();
        return saltStr;

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
                System.err.println("licensia1 : ");
        try {
            System.err.println("licensia2 : ");
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
            System.err.println("licensia3 : ");
            license = sb.toString();
            System.err.println("licensia : "+ license);
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
                LocalDateTime createdAt = Utils.convertStrWithTToLocalDateTime(licenseResponse.getCreatedAt());

                LocalDateTime calculateDate = createdAt.plusDays(totalJours);
                Long hoursRetard = LocalDateTime.now().until(calculateDate, ChronoUnit.HOURS);

                if (hoursRetard > 0) {
                    // La licence n'est pas encore expirée, hoursRetard contient le nombre d'heures.
                    long daysRemaining = hoursRetard / 24; // Convertir les heures en jours

                    licenceControl.setActif(true);
                    licenceControl.setDayBefore(daysRemaining);
                    licenceControl.setMaxPoste(Long.parseLong(splitInfo[1]));
                    Double consommation = (totalJours * 0.3);
                    if (daysRemaining <= (consommation.longValue())) {
                        if (daysRemaining == 0) {
                            licenceControl
                                    .setMessage("Votre licence expire dans quelques heures !");
                        } else {
                            licenceControl
                                    .setMessage("Votre licence expire dans  " + daysRemaining + " jr(s) !");
                        }
                    }

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
                    licenceControl.setMessage("Votre licence à expirer depuis " + daysElapsed + " jr(s) !");
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

     public static String getClientIpAddress(HttpServletRequest request) {
        if (request == null) {
            return "UNKNOWN";
        }

        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            // X-Forwarded-For peut contenir plusieurs IPs -> on prend la première
            return ip.split(",")[0].trim();
        }

        ip = request.getHeader("Proxy-Client-IP");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }

        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }

        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }

        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        return ip != null ? ip : "UNKNOWN";
    }



    


}
