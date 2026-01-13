package com.sicmagroup.gpr.service.auth;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sicmagroup.gpr.api.auth.AuthenticationRequest;
import com.sicmagroup.gpr.api.auth.AuthenticationResponse;
import com.sicmagroup.gpr.api.auth.UpdatePwdRequest;
import com.sicmagroup.gpr.api.auth.UpdateRequest;
import com.sicmagroup.gpr.api.config.setting.BotRequest;
import com.sicmagroup.gpr.api.config.setting.InstitutionRequest;
import com.sicmagroup.gpr.api.config.setting.MailRequest;
import com.sicmagroup.gpr.api.config.setting.SmsRequest;
import com.sicmagroup.gpr.api.config.user.AddEmailReceiver;
import com.sicmagroup.gpr.api.config.user.ForgetPasswordRequest;
import com.sicmagroup.gpr.api.config.user.RegisterRequest;
import com.sicmagroup.gpr.domain.dto.AlertDto;
import com.sicmagroup.gpr.domain.dto.ApiResponseDto;
import com.sicmagroup.gpr.domain.dto.CategorieObjetDto;
import com.sicmagroup.gpr.domain.dto.ClaimDto;
import com.sicmagroup.gpr.domain.dto.CollectionChannelDto;
import com.sicmagroup.gpr.domain.dto.ErrorResponse;
import com.sicmagroup.gpr.domain.dto.ExistingSolutionDto;
import com.sicmagroup.gpr.domain.dto.ExistingSolutionResponse;
import com.sicmagroup.gpr.domain.dto.ExternalRecourseDto;
import com.sicmagroup.gpr.domain.dto.LanguageDto;
import com.sicmagroup.gpr.domain.dto.ObjetDto;
import com.sicmagroup.gpr.domain.dto.PosteDto;
import com.sicmagroup.gpr.domain.dto.ProductDto;
import com.sicmagroup.gpr.domain.dto.ServicePointDto;
import com.sicmagroup.gpr.domain.dto.SuggestionDto;
import com.sicmagroup.gpr.domain.dto.UserDto;
import com.sicmagroup.gpr.domain.dto.claimResponse.ObjetResponse;
import com.sicmagroup.gpr.domain.dto.claimResponse.PosteResponse;
import com.sicmagroup.gpr.domain.dto.claimResponse.ServicePointResponse;
import com.sicmagroup.gpr.domain.enumeration.ClaimStatus;
import com.sicmagroup.gpr.domain.enumeration.ClaimType;
import com.sicmagroup.gpr.domain.enumeration.ConfigExportEnum;
import com.sicmagroup.gpr.domain.enumeration.Habilitation;
import com.sicmagroup.gpr.domain.enumeration.LogTarget;
import com.sicmagroup.gpr.domain.enumeration.LogType;
import com.sicmagroup.gpr.domain.enumeration.Role;
import com.sicmagroup.gpr.domain.enumeration.SatisfactionStatus;
import com.sicmagroup.gpr.domain.model.CategorieObjet;
import com.sicmagroup.gpr.domain.model.Claim;
import com.sicmagroup.gpr.domain.model.CollectionChannel;
import com.sicmagroup.gpr.domain.model.ExistingSolution;
import com.sicmagroup.gpr.domain.model.ExternalRecourse;
import com.sicmagroup.gpr.domain.model.Language;
import com.sicmagroup.gpr.domain.model.Log;
import com.sicmagroup.gpr.domain.model.Objet;
import com.sicmagroup.gpr.domain.model.Poste;
import com.sicmagroup.gpr.domain.model.Product;
import com.sicmagroup.gpr.domain.model.ServicePoint;
import com.sicmagroup.gpr.domain.model.Setting;
import com.sicmagroup.gpr.domain.model.Solution;
import com.sicmagroup.gpr.domain.model.Suggestion;
import com.sicmagroup.gpr.domain.model.User;
import com.sicmagroup.gpr.repository.CategorieObjetRepository;
import com.sicmagroup.gpr.repository.ClaimRepository;
import com.sicmagroup.gpr.repository.CollectionChannelRespository;
import com.sicmagroup.gpr.repository.ExistingSolutionRepository;
import com.sicmagroup.gpr.repository.ExternalRecourseRepository;
import com.sicmagroup.gpr.repository.LanguageRepository;
import com.sicmagroup.gpr.repository.ObjetRepository;
import com.sicmagroup.gpr.repository.PosteRepository;
import com.sicmagroup.gpr.repository.ProductRepository;
import com.sicmagroup.gpr.repository.ServicePointRepository;
import com.sicmagroup.gpr.repository.SuggestionRepository;
import com.sicmagroup.gpr.repository.UserRepository;
import com.sicmagroup.gpr.service.MailService;
import com.sicmagroup.gpr.service.claim.ClaimService;
import com.sicmagroup.gpr.service.faq.FaqServiceImpl;
import com.sicmagroup.gpr.service.jwt.JwtServiceImpl;
import com.sicmagroup.gpr.service.log.LogServiceImpl;
import com.sicmagroup.gpr.service.setting.SettingServiceImpl;
import com.sicmagroup.gpr.utils.Constante;
import com.sicmagroup.gpr.utils.Utils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtServiceImpl jwtServiceImpl;
    private final AuthenticationManager authenticationManager;
    private final ModelMapper modelMapper;
    private final PosteRepository posteRepository;
    private final ServicePointRepository servicePointRepository;
    private final ProductRepository productRepository;
    private final ObjetRepository objetRepository;
    private final LanguageRepository languageRepository;
    private final CollectionChannelRespository collectionChannelRespository;
    private final ExternalRecourseRepository externalRecourseRepository;
    private final ClaimRepository claimRepository;
    private final SuggestionRepository suggestionRepository;
    private final FaqServiceImpl faqServiceImpl;
    private final ExistingSolutionRepository existingSolutionRepository;
    private final CategorieObjetRepository categorieObjetRepository;
    private final SettingServiceImpl settingServiceImpl;
    private final LogServiceImpl logServiceImpl;
    private final MailService mailService;

    @Override
    public AuthenticationResponse register(RegisterRequest request) throws AuthenticationException {
        Poste poste = posteRepository.findById(request.getPosteId()).get();
        ServicePoint servicePoint = servicePointRepository.findById(request.getServicePointId()).get();

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new AuthenticationException("Error this email already exist");
        }
        int totalUser = 0;
        String license = "";
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
                ObjectMapper mapper = new ObjectMapper();
                JsonNode licenseObj = mapper.readTree("" + license + "");

                String activationRequest = licenseObj.get("activationRequest").asText();
                String[] splitARequest = activationRequest.split(",");
                String[] splitInfo = splitARequest[1].split(":");
                totalUser = Integer.parseInt(splitInfo[1]);
            }

            // settings.put("data", sb.toString());

        } catch (IOException e) {
            e.printStackTrace();
        }

        if (license != "" && totalUser != 0) {

            long totalActuUser = userRepository.count();
            if (totalActuUser < totalUser) {
                // possible de créer un nouvel user
                User user = User.builder()
                        .firstandlastname(request.getFirstAndLastName())
                        .email(request.getEmail())
                        .password(passwordEncoder.encode(request.getPassword()))
                        .additionalrole(Role.valueOf(request.getAdditionalRole()))
                        .tel(request.getTel())
                        .poste(poste)
                        .isRa(request.isRa())
                        .servicePoint(servicePoint)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();

                String code = "usr-" + UUID.randomUUID().toString().substring(0, 5);

                while (userRepository.findByCode(code).isPresent()) {
                    code = "usr-" + UUID.randomUUID().toString().substring(0, 5);
                }
                user.setCode(code);
                if (request.getAdditionalRole() == null) {
                    user.setAdditionalrole(Role.MOLDUE);
                } else {
                    user.setAdditionalrole(Role.valueOf(request.getAdditionalRole()));
                }

                user = userRepository.save(user);
                final User userForMail = user;
                // HashMap<String, Object> extras = new HashMap<>();
                // extras.put("additionalRole", user.getAdditionalrole());
                // extras.put("habilitations", user.getHabilitations());
                String jwtToken = jwtServiceImpl.generateToken(user);

                UserDto userDto = UserDto
                        .builder()
                        .id(user.getId())
                        .firstAndLastName(user.getFirstandlastname())
                        .email(user.getEmail())
                        .code(user.getCode())
                        .additionalRole(user.getAdditionalrole())
                        .isRa(user.isRa())
                        .posteDto(convertToResponse(poste))
                        .servicePointDto(convertToResponse(servicePoint))
                        .build();
                HashMap<String, Object> content = new HashMap<String, Object>();
                content.put("user", userDto);
                content.put("token", jwtToken);

                // Envoi de mail en parallèle
                // CompletableFuture.runAsync(() -> {
                    try {
                        //envoi de mail au user
                       
                        String message = """
                        <html>
                        <body style="font-family: Arial, sans-serif; background-color: #f7f9fc; padding: 20px;">
                            <div style="max-width: 600px; margin: auto; background: #ffffff; border-radius: 8px; box-shadow: 0 2px 6px rgba(0,0,0,0.1); padding: 25px;">
                            
                            <h2 style="color: #004aad; text-align: center;">Création de votre compte GPR</h2>

                            <p>Bonjour <strong>%s</strong>,</p>

                            <p>Votre compte a été créé avec succès sur la plateforme <strong>GPR</strong> (Gestion des Plaintes et Réclamations).</p>

                            <p style="margin-bottom: 10px;"><strong>Vos identifiants de connexion :</strong></p>
                            <div style="background-color: #f0f4ff; border-left: 4px solid #004aad; padding: 10px 15px; border-radius: 4px;">
                                <p style="margin: 0;"><strong>Email :</strong> %s</p>
                                <p style="margin: 0;"><strong>Mot de passe :</strong> %s</p>
                            </div>

                            <p style="margin-top: 20px;">
                                Nous vous recommandons de modifier votre mot de passe dès votre première connexion afin de garantir la sécurité de votre compte.
                            </p>

                            <p style="margin-top: 30px;">Cordialement,<br>
                            <strong>L’équipe GPR</strong></p>

                            <p style="font-size: 12px; color: gray; text-align: center; margin-top: 30px;">
                                Cet email a été généré automatiquement. Merci de ne pas y répondre.
                            </p>
                            </div>
                        </body>
                        </html>
                        """.formatted(userForMail.getFirstandlastname(), userForMail.getEmail(), request.getPassword());

                        
                        mailService.sendMail(userForMail.getEmail(), "Création de compte", message, null);

                                                
                        Log successLog = Log.builder()
                            .libelle("Mail notification création compte")
                            .content("Success mail notification création compte")
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
                                    .libelle("Echec mail notification création compte")
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
                // });



                return AuthenticationResponse.builder()
                        .response(ApiResponseDto
                                .builder()
                                .status(true)
                                .content(content)
                                .build())
                        .build();
            } else {
                // bloquer la création d'user
                return AuthenticationResponse.builder()
                        .response(ApiResponseDto
                                .builder()
                                .status(false)
                                .content(ErrorResponse.builder().message(
                                        "Limite de compte utilisateur atteint. Contactez nous sur info@sicmagroup.com pour faire une demande d'augmentation.")
                                        .title("Limite de compte utilisateur atteint. Contactez nous sur info@sicmagroup.com pour faire une demande d'augmentation.")
                                        .build())
                                .build())
                        .build();
            }

        } else {
            // lincence n'existe pas donc peut pas créer d'utilisateur
            return AuthenticationResponse.builder()
            .response(ApiResponseDto
                    .builder()
                    .status(false)
                    .content(ErrorResponse.builder().message(
                            "Configurer votre institution avant d'effectuer cette action")
                            .title("Configurer votre institution avant d'effectuer cette action")
                            .build())
                    .build())
            .build();
        }

    }
    
    @Override
    public AuthenticationResponse publicRegister(RegisterRequest request) throws AuthenticationException {
        Poste poste = posteRepository.findById(request.getPosteId()).orElseThrow(() ->
            new RuntimeException("Poste non trouvé"));
        ServicePoint servicePoint = servicePointRepository.findById(request.getServicePointId()).orElseThrow(() ->
            new RuntimeException("Service Point non trouvé"));

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new AuthenticationException("Error this email already exist");
        }

        int totalUser = 0;
        String license = "";
        try {
            File file = new File("data.txt");
            BufferedReader br = new BufferedReader(new FileReader(file));
            StringBuffer sb = new StringBuffer();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
            br.close();

            license = sb.toString();
            if (!license.isEmpty()) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode licenseObj = mapper.readTree(license);
                String activationRequest = licenseObj.get("activationRequest").asText();
                String[] splitARequest = activationRequest.split(",");
                String[] splitInfo = splitARequest[1].split(":");
                totalUser = Integer.parseInt(splitInfo[1]);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        if (!license.isEmpty() && totalUser != 0) {
            long totalActuUser = userRepository.count();
            if (totalActuUser < totalUser) {
                String code = "usr-" + UUID.randomUUID().toString().substring(0, 5);
                while (userRepository.findByCode(code).isPresent()) {
                    code = "usr-" + UUID.randomUUID().toString().substring(0, 5);
                }

                User user = User.builder()
                        .firstandlastname(request.getFirstAndLastName())
                        .email(request.getEmail())
                        .password(passwordEncoder.encode(request.getPassword()))
                        .additionalrole(Role.valueOf(request.getAdditionalRole()))
                        .tel(request.getTel())
                        .poste(poste)
                        .isRa(request.isRa())
                        .isDeleted(true)
                        .isRattached(true)
                        .servicePoint(servicePoint)
                        .code(code)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();

                userRepository.save(user);

                // Envoi de mail en parallèle
                // CompletableFuture.runAsync(() -> {
                    try {
                        //envoi de mail au user
                       String message = """
                        <html>
                            <body style="font-family: Arial, sans-serif; background-color: #f7f7f7; padding: 20px;">
                                <div style="max-width: 600px; margin: auto; background: white; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); padding: 20px;">
                                    <h2 style="color: #004080; text-align: center;">Création de votre compte GPR</h2>
                                    <p>Bonjour <strong>%s</strong>,</p>
                                    <p>
                                        Votre compte a bien été créé sur la plateforme de gestion des plaintes et réclamations (<strong>GPR</strong>).
                                        <br>Veuillez noter que l’activation est en attente de validation par l’administrateur.
                                        <br>Vous serez notifié dès que votre compte sera validé.
                                    </p>
                                    <div style="margin-top: 20px; background-color: #f0f8ff; border-left: 4px solid #004080; padding: 10px 15px;">
                                        <p style="margin: 0;"><strong>Identifiants d'accès :</strong></p>
                                        <p style="margin: 5px 0;">✉️ <strong>Email :</strong> %s</p>
                                        <p style="margin: 5px 0;">🔑 <strong>Mot de passe :</strong> %s</p>
                                    </div>
                                    <p style="margin-top: 30px;">Cordialement,<br>L’équipe GPR</p>
                                    <p style="font-size: 12px; color: gray; text-align: center; margin-top: 30px;">
                                        Cet email a été généré automatiquement. Merci de ne pas y répondre.
                                    </p>
                                </div>
                            </body>
                        </html>
                    """.formatted(user.getFirstandlastname(),user.getEmail(),request.getPassword());

                        mailService.sendMail(user.getEmail(),"Création de compte",message,null);

                                                
                        Log successLog = Log.builder()
                            .libelle("Mail notification création compte")
                            .content("Success mail notification création compte")
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
                                    .libelle("Echec mail notification création compte")
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
                // });

                return AuthenticationResponse.builder()
                        .response(ApiResponseDto
                                .builder()
                                .status(true)
                                .content("Inscription réussie. En attente de validation par un administrateur.")
                                .build())
                        .build();

            } else {
                return AuthenticationResponse.builder()
                        .response(ApiResponseDto
                                .builder()
                                .status(false)
                                .content(ErrorResponse.builder()
                                        .title("Limite atteinte")
                                        .message("Limite de compte utilisateur atteinte. Contactez-nous sur info@sicmagroup.com pour une extension.")
                                        .build())
                                .build())
                        .build();
            }
        } else {
            return AuthenticationResponse.builder()
                    .response(ApiResponseDto
                            .builder()
                            .status(false)
                            .content(ErrorResponse.builder()
                                    .title("Licence manquante")
                                    .message("Configurer votre institution avant d'effectuer cette action")
                                    .build())
                            .build())
                    .build();
        }
    }

    @Override
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        User user;
        User userFirst = userRepository
            .findByEmail(request.getEmail())
            .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
        
        if (userFirst.isDeleted()) {
            return AuthenticationResponse.builder()
                .response(ApiResponseDto.builder()
                    .status(false)
                    .content(Map.of(
                        "message", "Votre compte est désactivé. Veuillez contacter l’administrateur."
                    ))
                    .build())
                .build();

        }
        try {
             user = userRepository.findByEmailAndIsDeleted(request.getEmail(),false).orElseThrow();
        } catch (Exception e) {
           return AuthenticationResponse.builder()
                .response(ApiResponseDto
                        .builder()
                        .status(false)
                        .content(null)
                        .build())
                .build();

        }
        

        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

       
        String jwtToken = jwtServiceImpl.generateToken(user);

        UserDto userDto = convertToDto(user);
        // get setting info
        // servicePopint
        List<ServicePoint> allServicePoints = servicePointRepository.findByIsDeleted(false);
        List<ServicePointDto> allServicePointDtos = allServicePoints.stream().map(this::convertToDto)
                .collect(Collectors.toList());
        // poste
        List<Poste> allPostes = posteRepository.findByIsDeleted(false);
        List<PosteDto> allPosteDtos = allPostes.stream().map(this::convertToDto).collect(Collectors.toList());
        // Product
        List<Product> allProducts = productRepository.findByIsDeleted(false);
        List<ProductDto> allProductDtos = allProducts.stream().map(this::convertToDto).collect(Collectors.toList());
        // objet
        List<Objet> allObjets = objetRepository.findByIsDeleted(false);
        List<ObjetResponse> allObjetDtos = allObjets.stream().map(this::convertToResponse).collect(Collectors.toList());
        // language
        List<Language> allLanguages = languageRepository.findByIsDeleted(false);
        List<LanguageDto> allLanguageDtos = allLanguages.stream().map(this::convertToDto).collect(Collectors.toList());
        // collectionChannel
        List<CollectionChannel> allCollectionChannels = collectionChannelRespository.findByIsDeleted(false);
        List<CollectionChannelDto> allCollectionChannelDtos = allCollectionChannels.stream().map(this::convertToDto)
                .collect(Collectors.toList());
        // ExternalRecourse
        List<ExternalRecourse> allExternalRecourses = externalRecourseRepository.findByIsDeleted(false);
        List<ExternalRecourseDto> allExternalRecourseDtos = allExternalRecourses.stream().map(this::convertToDto)
                .collect(Collectors.toList());
        // User
        List<User> allUsers = userRepository.findByIsDeleted(false);
        List<UserDto> allUserDtos = allUsers.stream().map(this::convertToDto).collect(Collectors.toList());
        // existing solutions
        List<ExistingSolutionResponse> allExistingSolutions = existingSolutionRepository.findAll().stream()
                .map(this::convertToResponse).collect(Collectors.toList());
        // categorie objet
        List<CategorieObjetDto> allCategorieObjetDtos = categorieObjetRepository.findAll().stream()
                .map(this::convertToDto).collect(Collectors.toList());
        // institution
        // Settings
        HashMap<String, Object> settings = new HashMap<String, Object>();
        try {
            Setting setting = settingServiceImpl.getbySlug(Constante.INSTITUTION_SLUG);
            ObjectMapper objectMapper = new ObjectMapper();
            InstitutionRequest institutionRequest = objectMapper.readValue(setting.getValue(),
                    InstitutionRequest.class);
            settings.put("institution", institutionRequest);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        try {
           
            Setting mail = settingServiceImpl.getbySlug(Constante.MAIL_SLUG);
            ObjectMapper objectMapper = new ObjectMapper();
            MailRequest mailRequest = objectMapper.readValue(mail.getValue(), MailRequest.class);
            settings.put("mail", mailRequest);

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        try {
        
            Setting sms = settingServiceImpl.getbySlug(Constante.SMS_SLUG);
            ObjectMapper objectMapper = new ObjectMapper();
            SmsRequest smsRequest = objectMapper.readValue(sms.getValue(), SmsRequest.class);
            settings.put("sms", smsRequest);


        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        try {
        
            Setting bot = settingServiceImpl.getbySlug(Constante.BOT_SLUG);
            ObjectMapper objectMapper = new ObjectMapper();
            BotRequest botRequest = objectMapper.readValue(bot.getValue(), BotRequest.class);
            settings.put("bot", botRequest);

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        settings.put("servicePoints", allServicePointDtos);
        settings.put("postes", allPosteDtos);
        settings.put("products", allProductDtos);
        settings.put("objets", allObjetDtos);
        settings.put("languages", allLanguageDtos);
        settings.put("collectionChannels", allCollectionChannelDtos);
        settings.put("externalRecourses", allExternalRecourseDtos);
        settings.put("users", allUserDtos);
        settings.put("help", faqServiceImpl.getHelp());
        settings.put("presolution", allExistingSolutions);
        settings.put("categorie_objet", allCategorieObjetDtos);
        settings.put("others", settingServiceImpl.getAll());

        // recuperer le contenu du fichier data
        settingServiceImpl.updateLicence();
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
            settings.put("data", sb.toString());

        } catch (IOException e) {
            settings.put("data", "");
            e.printStackTrace();
        }

        HashMap<String, Object> content = new HashMap<String, Object>();
        content.put("user", userDto);
        content.put("token", jwtToken);
        content.put("settings", settings);
        return AuthenticationResponse.builder()
                .response(ApiResponseDto
                        .builder()
                        .status(true)
                        .content(content)
                        .build())
                .build();
    }

    @Override
    public ResponseEntity<ApiResponseDto> getAuthData() {
        User user;
      
        try {
            UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication()
                    .getPrincipal();
            user = getByEmail(userDetails.getUsername());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponseDto
                            .builder()
                            .status(false)
                            .content(ErrorResponse.builder().message("No token")
                                    .title("Votre compte ou token n'est plus valable").build())
                            .build());

        }

        UserDto userDto = convertToDto(user);
        // get setting info
        List<ServicePoint> allServicePoints = servicePointRepository.findByIsDeleted(false);
        List<ServicePointDto> allServicePointDtos = allServicePoints.stream().map(this::convertToDto)
                .collect(Collectors.toList());
        // poste
        List<Poste> allPostes = posteRepository.findByIsDeleted(false);
        List<PosteDto> allPosteDtos = allPostes.stream().map(this::convertToDto).collect(Collectors.toList());
        // Product
        List<Product> allProducts = productRepository.findByIsDeleted(false);
        List<ProductDto> allProductDtos = allProducts.stream().map(this::convertToDto).collect(Collectors.toList());
        // objet
        List<Objet> allObjets = objetRepository.findByIsDeleted(false);
        List<ObjetResponse> allObjetDtos = allObjets.stream().map(this::convertToResponse).collect(Collectors.toList());
        // language
        List<Language> allLanguages = languageRepository.findByIsDeleted(false);
        List<LanguageDto> allLanguageDtos = allLanguages.stream().map(this::convertToDto).collect(Collectors.toList());
        // collectionChannel
        List<CollectionChannel> allCollectionChannels = collectionChannelRespository.findByIsDeleted(false);
        List<CollectionChannelDto> allCollectionChannelDtos = allCollectionChannels.stream().map(this::convertToDto)
                .collect(Collectors.toList());
        // ExternalRecourse
        List<ExternalRecourse> allExternalRecourses = externalRecourseRepository.findByIsDeleted(false);
        List<ExternalRecourseDto> allExternalRecourseDtos = allExternalRecourses.stream().map(this::convertToDto)
                .collect(Collectors.toList());
        // User
        List<User> allUsers = userRepository.findByIsDeleted(false);
        List<UserDto> allUserDtos = allUsers.stream().map(this::convertToDto).collect(Collectors.toList());
        // existing solutions
        List<ExistingSolutionResponse> allExistingSolutions = existingSolutionRepository.findAll().stream()
                .map(this::convertToResponse).collect(Collectors.toList());
        // categorie objet
        List<CategorieObjetDto> allCategorieObjetDtos = categorieObjetRepository.findAll().stream()
                .map(this::convertToDto).collect(Collectors.toList());
        // institution
        // Settings
        HashMap<String, Object> settings = new HashMap<String, Object>();
        try {
            Setting setting = settingServiceImpl.getbySlug(Constante.INSTITUTION_SLUG);
            ObjectMapper objectMapper = new ObjectMapper();
            InstitutionRequest institutionRequest = objectMapper.readValue(setting.getValue(),
                    InstitutionRequest.class);
            settings.put("institution", institutionRequest);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        try {
           
            Setting mail = settingServiceImpl.getbySlug(Constante.MAIL_SLUG);
            ObjectMapper objectMapper = new ObjectMapper();
            MailRequest mailRequest = objectMapper.readValue(mail.getValue(), MailRequest.class);
            settings.put("mail", mailRequest);

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        try {
        
            Setting sms = settingServiceImpl.getbySlug(Constante.SMS_SLUG);
            ObjectMapper objectMapper = new ObjectMapper();
            SmsRequest smsRequest = objectMapper.readValue(sms.getValue(), SmsRequest.class);
            settings.put("sms", smsRequest);


        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        try {
        
            Setting bot = settingServiceImpl.getbySlug(Constante.BOT_SLUG);
            ObjectMapper objectMapper = new ObjectMapper();
            BotRequest botRequest = objectMapper.readValue(bot.getValue(), BotRequest.class);
            settings.put("bot", botRequest);

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        settings.put("servicePoints", allServicePointDtos);
        settings.put("postes", allPosteDtos);
        settings.put("products", allProductDtos);
        settings.put("objets", allObjetDtos);
        settings.put("languages", allLanguageDtos);
        settings.put("collectionChannels", allCollectionChannelDtos);
        settings.put("externalRecourses", allExternalRecourseDtos);
        settings.put("users", allUserDtos);
        settings.put("help", faqServiceImpl.getHelp());
        settings.put("presolution", allExistingSolutions);
        settings.put("categorie_objet", allCategorieObjetDtos);
        settings.put("others", settingServiceImpl.getAll());

        // recuperer le contenu du fichier data
        settingServiceImpl.updateLicence();
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
            settings.put("data", sb.toString());

        } catch (IOException e) {
            settings.put("data", "");
            e.printStackTrace();
        }

        HashMap<String, Object> content = new HashMap<String, Object>();
        

        content.put("user", userDto);
        content.put("settings", settings);
        return ResponseEntity.ok(ApiResponseDto
                .builder()
                .status(true)
                .content(content)
                .build());

    }

    @Override
    public ResponseEntity<ApiResponseDto> forgetPassword(ForgetPasswordRequest request) {

        try {

            User user = userRepository.findByEmailAndIsDeleted(request.getEmail(),false).orElseThrow();
            char[] password = generatePassword(8);

            List<User> userMailTo = new ArrayList<>();
            userMailTo.add(user);
            
            // Envoi de mail en parallèle
            // CompletableFuture.runAsync(() -> {
                try {
                    String message = """
                        <html>
                            <body style="font-family: Arial, sans-serif; background-color: #f7f7f7; padding: 20px;">
                                <div style="max-width: 600px; margin: auto; background: white; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); padding: 20px;">
                                    <h2 style="color: #004080; text-align: center;">Réinitialisation de mot de passe - GPR</h2>
                                    
                                    <p>Bonjour <strong>%s</strong>,</p>
                                    <p>
                                        Votre mot de passe a été <strong>réinitialisé avec succès</strong> sur la plateforme
                                        de gestion des plaintes et réclamations (<strong>GPR</strong>).
                                    </p>
                                    
                                    <div style="margin-top: 20px; background-color: #f0f8ff; border-left: 4px solid #004080; padding: 10px 15px;">
                                        <p style="margin: 0;"><strong>Vos informations de connexion :</strong></p>
                                        <p style="margin: 5px 0;">✉️ <strong>Email :</strong> %s</p>
                                        <p style="margin: 5px 0;">🔑 <strong>Mot de passe :</strong> %s</p>
                                    </div>

                                    <p style="margin-top: 25px; color: #cc0000; font-size: 0.9em;">
                                        ⚠️ Ce message contient des informations sensibles. Ne le partagez avec personne.
                                    </p>

                                    <p style="margin-top: 30px;">Cordialement,<br>L’équipe GPR</p>

                                    <p style="font-size: 12px; color: gray; text-align: center; margin-top: 30px;">
                                        Cet email a été généré automatiquement. Merci de ne pas y répondre.
                                    </p>
                                </div>
                            </body>
                        </html>
                    """.formatted(user.getFirstandlastname(),user.getEmail(),new String(password));


                        mailService.sendMail(userMailTo,"Réinitialisation de mot de passe - GPR",message,null);
                        Log successLog = Log.builder()
                            .libelle("Mail notification mot de passe oublié")
                            .content("Success mail notification mot de passe oublié")
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
                            .libelle("Echec mail notification mot de passe oublié")
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
            // });
            
            user.setPassword(passwordEncoder.encode(new String(password)));
            userRepository.save(user);

            return ResponseEntity.ok(ApiResponseDto
                    .builder()
                    .status(true)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponseDto
                            .builder()
                            .status(false)
                            .content(ErrorResponse.builder().message(e.getMessage())
                                    .title("Erreur").build())
                            .build());
        }
    }
    

    @Override
    public HashMap<String, Object> exportConfig(ConfigExportEnum type) {
        HashMap<String, Object> result = new HashMap<>();
        result.put("type", type.name());

        if (type.equals(ConfigExportEnum.claims) || type.equals(ConfigExportEnum.denunciations)) {
            ClaimType claimType = type.equals(ConfigExportEnum.claims) ? ClaimType.CLAIM : ClaimType.DENUNCIACION;
            try {
                List<Claim> claims = claimRepository.findByType(claimType);
                List<ClaimDto> claimDtos = claims.stream().map((Claim claim) -> {
                    return modelMapper.map(claim, ClaimDto.class);
                })
                        .collect(Collectors.toList());

                result.put("data", claimDtos);
                result.put("totals", claimDtos.size());
            } catch (Exception e) {
                result.put("data", "NULL");
                result.put("totals", 0);
            }
        } else if (type.equals(ConfigExportEnum.suggestions)) {
            try {
                List<Suggestion> suggestions = suggestionRepository.findAll();
                List<SuggestionDto> suggestionDtos = suggestions.stream().map((Suggestion suggest) -> {
                    return modelMapper.map(suggest, SuggestionDto.class);
                })
                        .collect(Collectors.toList());

                result.put("data", suggestionDtos);
                result.put("totals", suggestionDtos.size());
            } catch (Exception e) {
                result.put("data", "NULL");
                result.put("totals", 0);

            }
        }

        else if (type.equals(ConfigExportEnum.configs)) {
            List<ServicePoint> allServicePoints = servicePointRepository.findAll();
            List<ServicePointDto> allServicePointDtos = allServicePoints.stream().map(this::convertToDto)
                    .collect(Collectors.toList());
            
            List<Poste> allPostes = posteRepository.findByIsDeleted(false);
            List<PosteDto> allPosteDtos = allPostes.stream().map(this::convertToDto).collect(Collectors.toList());
            // Product
            List<Product> allProducts = productRepository.findByIsDeleted(false);
            List<ProductDto> allProductDtos = allProducts.stream().map(this::convertToDto).collect(Collectors.toList());
            // objet
            List<Objet> allObjets = objetRepository.findByIsDeleted(false);
            List<ObjetResponse> allObjetDtos = allObjets.stream().map(this::convertToResponse)
                    .collect(Collectors.toList());
            // language
            List<Language> allLanguages = languageRepository.findByIsDeleted(false);
            List<LanguageDto> allLanguageDtos = allLanguages.stream().map(this::convertToDto)
                    .collect(Collectors.toList());
            // collectionChannel
            List<CollectionChannel> allCollectionChannels = collectionChannelRespository.findByIsDeleted(false);
            List<CollectionChannelDto> allCollectionChannelDtos = allCollectionChannels.stream().map(this::convertToDto)
                    .collect(Collectors.toList());
            // ExternalRecourse
            List<ExternalRecourse> allExternalRecourses = externalRecourseRepository.findByIsDeleted(false);
            List<ExternalRecourseDto> allExternalRecourseDtos = allExternalRecourses.stream().map(this::convertToDto)
                    .collect(Collectors.toList());
            // User
            List<User> allUsers = userRepository.findByIsDeleted(false);
            List<UserDto> allUserDtos = allUsers.stream().map(this::convertToDto).collect(Collectors.toList());
            // existing solutions
            List<ExistingSolutionResponse> allExistingSolutions = existingSolutionRepository.findAll().stream()
                    .map(this::convertToResponse).collect(Collectors.toList());
            // categorie objet
            List<CategorieObjetDto> allCategorieObjetDtos = categorieObjetRepository.findAll().stream()
                    .map(this::convertToDto).collect(Collectors.toList());
            // institution
            // Settings
            HashMap<String, Object> settings = new HashMap<String, Object>();
            try {
                Setting setting = settingServiceImpl.getbySlug(Constante.INSTITUTION_SLUG);
                ObjectMapper objectMapper = new ObjectMapper();
                InstitutionRequest institutionRequest = objectMapper.readValue(setting.getValue(),
                        InstitutionRequest.class);
                settings.put("institution", institutionRequest);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            try {

                Setting mail = settingServiceImpl.getbySlug(Constante.MAIL_SLUG);
                ObjectMapper objectMapper = new ObjectMapper();
                MailRequest mailRequest = objectMapper.readValue(mail.getValue(), MailRequest.class);
                settings.put("mail", mailRequest);

            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            try {

                Setting sms = settingServiceImpl.getbySlug(Constante.SMS_SLUG);
                ObjectMapper objectMapper = new ObjectMapper();
                SmsRequest smsRequest = objectMapper.readValue(sms.getValue(), SmsRequest.class);
                settings.put("sms", smsRequest);

            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            try {

                Setting bot = settingServiceImpl.getbySlug(Constante.BOT_SLUG);
                ObjectMapper objectMapper = new ObjectMapper();
                BotRequest botRequest = objectMapper.readValue(bot.getValue(), BotRequest.class);
                settings.put("bot", botRequest);

            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            settings.put("servicePoints", allServicePointDtos);
            settings.put("postes", allPosteDtos);
            settings.put("products", allProductDtos);
            settings.put("objets", allObjetDtos);
            settings.put("languages", allLanguageDtos);
            settings.put("collectionChannels", allCollectionChannelDtos);
            settings.put("externalRecourses", allExternalRecourseDtos);
            settings.put("users", allUserDtos);
            settings.put("help", faqServiceImpl.getHelp());
            settings.put("presolution", allExistingSolutions);
            settings.put("categorie_objet", allCategorieObjetDtos);
            settings.put("others", settingServiceImpl.getAll());

            
            result.put("data", settings);
        }

        // }

        return result;
    }
    
    private static char[] generatePassword(int length) {
        String capitalCaseLetters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lowerCaseLetters = "abcdefghijklmnopqrstuvwxyz";
        String specialCharacters = "!@#$";
        String numbers = "1234567890";
        String combinedChars = capitalCaseLetters + lowerCaseLetters + specialCharacters + numbers;
        Random random = new Random();
        char[] password = new char[length];

        password[0] = lowerCaseLetters.charAt(random.nextInt(lowerCaseLetters.length()));
        password[1] = capitalCaseLetters.charAt(random.nextInt(capitalCaseLetters.length()));
        password[2] = specialCharacters.charAt(random.nextInt(specialCharacters.length()));
        password[3] = numbers.charAt(random.nextInt(numbers.length()));

        for (int i = 4; i < length; i++) {
            password[i] = combinedChars.charAt(random.nextInt(combinedChars.length()));
        }
        return password;
    }

    private ExistingSolutionResponse convertToResponse(ExistingSolution exSolution) {
        ExistingSolutionResponse existingSolutionResponse = modelMapper.map(exSolution, ExistingSolutionResponse.class);
        return existingSolutionResponse;
    }

    private ObjetResponse convertToResponse(Objet objet) {
        ObjetResponse objetResponse = modelMapper.map(objet, ObjetResponse.class);
        if (objet.getExistingSolutions() != null) {
            objetResponse.setExistingSolutions(
                    objet.getExistingSolutions().stream().map(this::convertToResponse).collect(Collectors.toList()));
        }

        if (objet.getCategorie() != null) {
            objetResponse.setCategorie(null);
            objetResponse.setCategorie(convertToDto(objet.getCategorie()));
        }

        return objetResponse;
    }

    private CategorieObjetDto convertToDto(CategorieObjet categorieObjet) {
        CategorieObjetDto dto = modelMapper.map(categorieObjet, CategorieObjetDto.class);
        return dto;
    }

    private ExistingSolutionDto convertToDto(ExistingSolution solution) {
        ExistingSolutionDto existingSolutionDto = modelMapper.map(solution, ExistingSolutionDto.class);
        existingSolutionDto.setObjetDto(convertToDto(solution.getObjet()));
        return existingSolutionDto;
    }

    private UserDto convertToDto(User user) {
        UserDto userDto = modelMapper.map(user, UserDto.class);
        userDto.setPosteDto(convertToResponse(user.getPoste()));
        userDto.setServicePointDto(convertToResponse(user.getServicePoint()));
        return userDto;
    }

    private ServicePointDto convertToDto(ServicePoint servicepoint1) {
        ServicePointDto servicePointDto = modelMapper.map(servicepoint1, ServicePointDto.class);
        return servicePointDto;
    }

    private PosteDto convertToDto(Poste poste1) {
        PosteDto posteDto = modelMapper.map(poste1, PosteDto.class);
        return posteDto;
    }

    private PosteResponse convertToResponse(Poste poste1) {
        PosteResponse posteResponse = modelMapper.map(poste1, PosteResponse.class);
        return posteResponse;
    }

    private ServicePointResponse convertToResponse(ServicePoint servicepoint1) {
        ServicePointResponse servicePointResponse = modelMapper.map(servicepoint1, ServicePointResponse.class);
        return servicePointResponse;
    }

    private ProductDto convertToDto(Product product) {
        ProductDto productDto = modelMapper.map(product, ProductDto.class);
        return productDto;
    }

    private ObjetDto convertToDto(Objet objet) {
        ObjetDto objetDto = new ObjetDto();
        if (objet.getCategorie() != null) {
            objetDto.setCategorie(objet.getCategorie().getId());
        }

        objetDto.setCreatedAt(objet.getCreatedAt());
        objetDto.setUpdatedAt(objet.getUpdatedAt());
        objetDto.setDescription(objet.getDescription());
        objetDto.setLibelle(objet.getLibelle());
        objetDto.setRisqueLevel(objet.getRisqueLevel());
        objetDto.setProcessingTime(objet.getProcessingTime());
        objetDto.setId(objet.getId());
        return objetDto;
    }

    private LanguageDto convertToDto(Language language) {
        LanguageDto languageDto = modelMapper.map(language, LanguageDto.class);
        return languageDto;
    }

    private CollectionChannelDto convertToDto(CollectionChannel collectionChannel) {
        CollectionChannelDto collectionChannelDto = modelMapper.map(collectionChannel, CollectionChannelDto.class);
        return collectionChannelDto;
    }

    private ExternalRecourseDto convertToDto(ExternalRecourse externalRecourse) {
        ExternalRecourseDto externalRecourseDto = modelMapper.map(externalRecourse, ExternalRecourseDto.class);
        return externalRecourseDto;
    }

    private User convertFromDtoToEntity(UserDto userDto) {
        User user = modelMapper.map(userDto, User.class);

        if (user.getId() != null) {
            // ServicePoint oldServicePoint = serviceImpl.getById(servicePointDto.getId());

        } else {

        }
        return user;
    }

    private Poste convertFromDtoToEntity(PosteDto posteDto) {
        Poste poste = modelMapper.map(posteDto, Poste.class);

        if (poste.getId() != null) {
            // ServicePoint oldServicePoint = serviceImpl.getById(servicePointDto.getId());
            poste.setUpdatedAt(LocalDateTime.now());

            if (posteDto.isDeleted()) {
                poste.setDeleted(posteDto.isDeleted());
                poste.setDeletedAt(LocalDateTime.now());
            }

        } else {
            poste.setCreatedAt(LocalDateTime.now());
            poste.setDeleted(false);
        }
        return poste;
    }

    private ServicePoint convertFromDtoToEntity(ServicePointDto servicePointDto) {
        ServicePoint servicePoint = modelMapper.map(servicePointDto, ServicePoint.class);

        if (servicePointDto.getId() != null) {
            // ServicePoint oldServicePoint = serviceImpl.getById(servicePointDto.getId());
            servicePoint.setUpdatedAt(LocalDateTime.now());

            if (servicePointDto.isDeleted()) {
                servicePoint.setDeleted(servicePointDto.isDeleted());
                servicePoint.setDeletedAt(LocalDateTime.now());
            }

        } else {
            servicePoint.setCreatedAt(LocalDateTime.now());
            servicePoint.setDeleted(false);
        }
        return servicePoint;
    }

    @Override
    public List<User> getAll() {
        return userRepository.findByIsDeleted(false);
    }

    @Override
    public List<User> getAllDeleted() {
        return userRepository.findByIsDeleted(true);
    }

    @Override
    public List<User> all() {
        return userRepository.findAll();
    }

    @Override
    public boolean isActif(Long id) {
        try {
            User user = userRepository.findByIdAndIsDeleted(id, false).orElseThrow();

            return true;

        } catch (Exception e) {
            return false;
        }
        
    }

    @Override
    public User getById(Long id) throws NotFoundException {
        return userRepository.findById(id).orElseThrow(() -> new NotFoundException());
    }

    @Override
    public User getDeletedById(Long id, boolean deleted) throws NotFoundException {
        return userRepository.findByIdAndIsDeleted(id, deleted).orElseThrow(() -> new NotFoundException());
    }

    @Override
    public User updateUser(Long id, RegisterRequest userDto) throws NotFoundException {
        User userOld = userRepository.findById(id).orElseThrow(() -> new NotFoundException());
        Poste poste = posteRepository.findById(userDto.getPosteId()).get();
        ServicePoint servicePoint = servicePointRepository.findById(userDto.getServicePointId()).get();
        User user = User.builder()
                .id(userDto.getId())
                .firstandlastname(userDto.getFirstAndLastName())
                .email(userDto.getEmail())
                .additionalrole(Role.valueOf(userDto.getAdditionalRole()))
                .tel(userDto.getTel())
                .poste(poste)
                .isRa(userDto.isRa())
                .servicePoint(servicePoint)
                .build();
        if (userDto.getPassword() != null && !userDto.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(userDto.getPassword()));
        } else {
            user.setPassword(userOld.getPassword());
        }

        user.setUpdatedAt(LocalDateTime.now());
        user.setCreatedAt(userOld.getCreatedAt());
        if (userOld.getCode() == null || userOld.getCode() == "") {
            String code = "usr-" + UUID.randomUUID().toString().substring(0, 5);

            while (userRepository.findByCode(code).isPresent()) {
                code = "usr-" + UUID.randomUUID().toString().substring(0, 5);
            }
            user.setCode(code);
        } else {
            user.setCode(userOld.getCode());
        }

        // juste avant la sauvegarde
        if (user.isRa()) {
            Optional<User> existingRa = userRepository
                .findByServicePointAndIsRaTrue(user.getServicePoint());
            
            if (existingRa.isPresent() && !existingRa.get().getId().equals(user.getId())) {
                throw new IllegalArgumentException("Un RA existe déjà pour ce point de service.");
            }
        }

        user = userRepository.save(user);
        return user;
    }

    @Override
    public User deleteTempUser(Long id) throws NotFoundException {
        User userOld = userRepository.getReferenceById(id);
        if(isTheLastH12(userOld)){
            throw new NotFoundException();
        };
        
        userOld.setDeleted(true);
        userOld.setDeletedAt(LocalDateTime.now());
        userOld = userRepository.save(userOld);

        final User userForMail = userOld;
        // Envoi de mail en parallèle
        // CompletableFuture.runAsync(() -> {
            try {
                //envoi de mail au user
                String message = """
                    <html>
                        <body style="font-family: Arial, sans-serif; background-color: #f7f7f7; padding: 20px;">
                            <div style="max-width: 600px; margin: auto; background: white; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); padding: 20px;">
                            
                            <h2 style="color: #004080; text-align: center;">Désactivation de votre compte - GPR</h2>
                            
                            <p>Bonjour <strong>%s</strong>,</p>
                            
                            <p>
                                Votre compte sur la plateforme de gestion des plaintes et réclamations (<strong>GPR</strong>) a été désactivé.
                            </p>
                            
                            <p>
                                Vous n’avez désormais plus accès aux fonctionnalités de la plateforme. 
                                Si vous pensez qu’il s’agit d’une erreur ou souhaitez réactiver votre compte, veuillez contacter l’administrateur.
                            </p>
                            
                            <p style="margin-top: 30px;">Cordialement,<br>L’équipe GPR</p>

                            <p style="font-size: 12px; color: gray; text-align: center; margin-top: 30px;">
                                Cet email a été généré automatiquement. Merci de ne pas y répondre.
                            </p>
                            </div>
                        </body>
                    </html>
                    """.formatted(userForMail.getFirstandlastname());

                mailService.sendMail(userForMail.getEmail(),"Désactivation de compte",message,null);

                                        
                Log successLog = Log.builder()
                    .libelle("Mail notification désactivation compte")
                    .content("Success mail notification désactivation compte")
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
                            .libelle("Echec mail notification désactivation compte")
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
        // });

        return userOld;
    }

    @Override
    public User enabledUser(Long id) throws NotFoundException {
        User userOld = userRepository.getReferenceById(id);
        userOld.setDeleted(false);
        userOld.setDeletedAt(LocalDateTime.now());
        userOld = userRepository.save(userOld);

        final User userForMail = userOld;
        String statusText;

        if (userForMail.isRattached()) {
            statusText = "validé";
        } else{
            statusText = "réactivé";
        } 
        // Envoi de mail en parallèle
        // // CompletableFuture.runAsync(() -> {
            try {
                //envoi de mail au user
                String message = """
                    <html>
                        <body style="font-family: Arial, sans-serif; background-color: #f7f7f7; padding: 20px;">
                            <div style="max-width: 600px; margin: auto; background: white; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); padding: 20px;">
                            
                            <h2 style="color: #004080; text-align: center;">Activation de votre compte - GPR</h2>
                            
                            <p>Bonjour <strong>%s</strong>,</p>
                            
                            <p>
                                Votre compte sur la plateforme de gestion des plaintes et réclamations (<strong>GPR</strong>) a été %s avec succès.
                            </p>
                            
                            <p>
                                Vous pouvez désormais vous connecter et accéder à toutes les fonctionnalités disponibles.
                            </p>
                            
                            <p style="margin-top: 30px;">Cordialement,<br>L’équipe GPR</p>

                            <p style="font-size: 12px; color: gray; text-align: center; margin-top: 30px;">
                                Cet email a été généré automatiquement. Merci de ne pas y répondre.
                            </p>
                            </div>
                        </body>
                    </html>
                    """.formatted(userForMail.getFirstandlastname(),statusText);

                mailService.sendMail(userForMail.getEmail(),"Validation de compte",message,null);

                                        
                Log successLog = Log.builder()
                    .libelle("Mail notification validation compte")
                    .content("Success mail notification validation compte")
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
                            .libelle("Echec mail notification création compte")
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
        // // });

        return userOld;
    }

    public boolean isTheLastH12(User user){
        // List<User> users = userRepository.fin
        // List<
        if(user.getPoste().getHabilitations().contains(Habilitation.H12.name())){
            List<Poste> postes = posteRepository.findByHabilitationsContaining(Habilitation.H12.name());
            int numbreUser = 0;
            for (Poste poste : postes) {
                
                numbreUser = numbreUser+ userRepository.findByPosteAndIsDeleted(poste, false).size();
            }
            if(numbreUser <=1){
                return true;
            }
        }
            return false;
    }
    public boolean isTheLastH12(Long id){
        try {
            
       
        User user = userRepository.findById(id).orElseThrow();
        if(user.getPoste().getHabilitations().contains(Habilitation.H12.name())){
            List<Poste> postes = posteRepository.findByHabilitationsContaining(Habilitation.H12.name());
            int numbreUser = 0;
            for (Poste poste : postes) {
                numbreUser = numbreUser+ poste.getUsers().size();
            }

            if(numbreUser <=1){
                return true;
            }

        }
            return false;

        } catch (Exception e) {
            return false;
        }
    }

    public void checkSingleRaPerServicePoint(Long userId, RegisterRequest request) {
        System.out.println("requets : "+request.isRa()+ " lol ="+request.getAdditionalRole());
        // Vérification du rôle additionnel et du flag isRa
        if (request.isRa() && request.getAdditionalRole() != null && !request.getAdditionalRole().isEmpty()) {
            Role role = null;
            try {
                role = Role.valueOf(request.getAdditionalRole());
            } catch (IllegalArgumentException e) {
                // Si la valeur du rôle n’existe pas dans l’enum, on ignore simplement
            }

            if (role != null && (role == Role.PILOTE || role == Role.DE)) {
                throw new IllegalArgumentException("Un utilisateur RA ne peut pas avoir le rôle additionnel PILOTE ou DE.");
            }
        }


        // Vérification de l'unicité du RA pour le point de service
        if (request.isRa() && request.getServicePointId() != null) {
            ServicePoint sp = servicePointRepository.findById(request.getServicePointId())
                    .orElseThrow(() -> new IllegalArgumentException("Point de service non trouvé"));

            Optional<User> existingRa = userRepository.findByServicePointAndIsRaTrue(sp);

            if (existingRa.isPresent()) {
                // Si on met à jour un utilisateur différent du RA existant → erreur
                if (userId == null || !existingRa.get().getId().equals(userId)) {
                    throw new IllegalArgumentException("Un RA existe déjà pour ce point de service.");
                }
            }
        }
    }



    @Override
    public void deleteUser(User user) throws Exception {
        final User userForMail = user; 

        if (!user.getPoste().getHabilitations().contains("H12")) {
            try {
                if (user.isRattached() && user.isDeleted()) {
                    // Envoi du mail de rejet
                    // CompletableFuture.runAsync(() -> {
                        try {
                            String messageUserRejete = """
                            <html>
                            <body style="font-family: Arial, sans-serif; background-color: #f7f7f7; padding: 20px;">
                                <div style="max-width: 600px; margin: auto; background: white; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); padding: 20px;">
                                
                                <h2 style="color: #004080; text-align: center;">Rejet de votre compte - GPR</h2>
                                
                                <p>Bonjour <strong>%s</strong>,</p>
                                
                                <p>
                                    Nous vous informons que votre compte sur <strong>GPR</strong> a été <strong>rejeté</strong> et n'a pas été validé.
                                </p>
                                
                                <p>
                                    Si vous pensez que cette action a été effectuée par erreur ou si vous avez des questions, n'hésitez pas à contacter votre administrateur.
                                </p>
                                
                                <p style="margin-top: 25px; color: #cc0000; font-size: 0.9em;">
                                    ⚠️ Ce message est confidentiel. Merci de ne pas le divulguer.
                                </p>
                                
                                <p style="margin-top: 30px;">Cordialement,<br>L’équipe GPR</p>

                                <p style="font-size: 12px; color: gray; text-align: center; margin-top: 30px;">
                                    Cet email a été généré automatiquement. Merci de ne pas y répondre.
                                </p>

                                </div>
                            </body>
                            </html>
                            """.formatted(userForMail.getFirstandlastname());

                            mailService.sendMail(userForMail.getEmail(),"Rejet de compte sur GPR",messageUserRejete,null);

                            Log successLog = Log.builder()
                                .libelle("Mail notification Rejet compte")
                                .content("Success mail notification rejet compte")
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
                                        .libelle("Echec mail notification création compte")
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
                    // });
                } else {
                    // Envoi du mail
                    // CompletableFuture.runAsync(() -> {
                        try {
                            String messageCompteSupprime = """
                            <html>
                            <body style="font-family: Arial, sans-serif; background-color: #f7f7f7; padding: 20px;">
                                <div style="max-width: 600px; margin: auto; background: white; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); padding: 20px;">

                                <h2 style="color: #004080; text-align: center;">Suppression de votre compte - GPR</h2>

                                <p>Bonjour <strong>%s</strong>,</p>

                                <p>
                                    Nous vous informons que votre compte sur <strong>GPR</strong> a été supprimé.
                                </p>

                                <p>
                                    Si vous pensez que cette action a été effectuée par erreur ou si vous avez des questions, n'hésitez pas à contacter votre administrateur.
                                </p>

                                <p style="margin-top: 25px; color: #cc0000; font-size: 0.9em;">
                                    ⚠️ Ce message est confidentiel. Merci de ne pas le divulguer.
                                </p>

                                <p style="margin-top: 30px;">Cordialement,<br>L’équipe GPR</p>

                                <p style="font-size: 12px; color: gray; text-align: center; margin-top: 30px;">
                                    Cet email a été généré automatiquement. Merci de ne pas y répondre.
                                </p>

                                </div>
                            </body>
                            </html>
                            """.formatted(userForMail.getFirstandlastname());

                            mailService.sendMail(user.getEmail(),"Suppression de compte sur GPR",messageCompteSupprime,null);

                    
                            Log successLog = Log.builder()
                                .libelle("Mail notification suppression compte")
                                .content("Success mail notification suppression compte")
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
                                    .libelle("Echec mail notification création compte")
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
                    // });
                }             

                userRepository.delete(user);
            } catch (Exception e) {
                throw new Exception(
                "Impossible de supprimer cet utilisateur car il intervient dans plusieurs opérations.");
            }
        } else {
            List<User> users = userRepository.findAll();
            boolean existAnotherAdmin = false;
            for (User userL : users) {
                if (userL != user && userL.getPoste().getHabilitations().contains("H12")) {
                    existAnotherAdmin = true;
                    break;
                }
            }

            if (existAnotherAdmin) {
                try {
                    if (user.isRattached() && user.isDeleted()) {
                        // Envoi du mail de rejet
                        // CompletableFuture.runAsync(() -> {
                            try {
                               String messageUserRejete = """
                                <html>
                                <body style="font-family: Arial, sans-serif; background-color: #f7f7f7; padding: 20px;">
                                    <div style="max-width: 600px; margin: auto; background: white; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); padding: 20px;">

                                    <h2 style="color: #004080; text-align: center;">Rejet de votre compte - GPR</h2>

                                    <p>Bonjour <strong>%s</strong>,</p>

                                    <p>
                                        Nous vous informons que votre compte sur <strong>GPR</strong> a été <strong style="color: #cc0000;">rejeté</strong> et n'a pas été validé.
                                    </p>

                                    <p>
                                        Si vous pensez que cette action a été effectuée par erreur ou si vous avez des questions, n'hésitez pas à contacter votre administrateur.
                                    </p>

                                    <p style="margin-top: 25px; color: #cc0000; font-size: 0.9em;">
                                        ⚠️ Ce message est confidentiel. Merci de ne pas le divulguer.
                                    </p>

                                    <p style="margin-top: 30px;">Cordialement,<br>L’équipe GPR</p>

                                    <p style="font-size: 12px; color: gray; text-align: center; margin-top: 30px;">
                                        Cet email a été généré automatiquement. Merci de ne pas y répondre.
                                    </p>

                                    </div>
                                </body>
                                </html>
                                """.formatted(userForMail.getFirstandlastname());

                                mailService.sendMail(userForMail.getEmail(),"Rejet de compte sur GPR",messageUserRejete,null);

                                Log successLog = Log.builder()
                                    .libelle("Mail notification Rejet compte")
                                    .content("Success mail notification rejet compte")
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
                                            .libelle("Echec mail notification création compte")
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
                        // });
                    } else {
                        // Envoi du mail
                        // CompletableFuture.runAsync(() -> {
                            try {
                                String message = """
                                <html>
                                <body style="font-family: Arial, sans-serif; background-color: #f7f7f7; padding: 20px;">
                                    <div style="max-width: 600px; margin: auto; background: white; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); padding: 20px;">

                                    <h2 style="color: #004080; text-align: center;">Suppression de votre compte - GPR</h2>

                                    <p>Bonjour <strong>%s</strong>,</p>

                                    <p>
                                        Nous vous informons que votre compte sur <strong>GPR</strong> a été supprimé.
                                    </p>

                                    <p>
                                        Si vous pensez que cette action a été effectuée par erreur ou si vous avez des questions, n'hésitez pas à contacter votre administrateur.
                                    </p>

                                    <p style="margin-top: 25px; color: #cc0000; font-size: 0.9em;">
                                        ⚠️ Ce message est confidentiel. Merci de ne pas le divulguer.
                                    </p>

                                    <p style="margin-top: 30px;">Cordialement,<br>L’équipe GPR</p>

                                    <p style="font-size: 12px; color: gray; text-align: center; margin-top: 30px;">
                                        Cet email a été généré automatiquement. Merci de ne pas y répondre.
                                    </p>

                                    </div>
                                </body>
                                </html>
                                """.formatted(userForMail.getFirstandlastname());

                                mailService.sendMail(user.getEmail(),"Suppression de compte sur GPR",message,null);

                    
                        
                                Log successLog = Log.builder()
                                    .libelle("Mail notification suppression compte")
                                    .content("Success mail notification suppression compte")
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
                                        .libelle("Echec mail notification création compte")
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
                        // });
                    }             

                    userRepository.delete(user);
                } catch (Exception e) {
                    throw new Exception(
                            "Impossible de supprimer cet utilisateur car il intervient dans plusieurs opérations.");
                }
            } else {
                throw new Exception(
                        "Impossible de supprimer cet utilisateur car il est le seul en mesure de configurer l'outil.");
            }
        }

    }

    @Override
    public List<User> getUsersByRoles(List<Role> roles) {
        return userRepository.findByAdditionalroleIn(roles);
    }

    @Override
    public User getByEmail(String email) throws Exception {
        return userRepository.findByEmailAndIsDeleted(email,false).orElseThrow(() -> new Exception("Utilisateur introuvable"));
    }

    @Override
    public void updateAccountUser(UpdateRequest request) throws Exception {
        User user = userRepository.findById(request.getId())
                .orElseThrow(() -> new Exception("Utilisateur introuvable"));

        user.setFirstandlastname(request.getFirstname());
        user.setEmail(request.getEmail());
        user.setTel(request.getPhone());

        user.setUpdatedAt(LocalDateTime.now());

        userRepository.save(user);

        // SecurityContextHolder.getContext().getAuthentication().setAuthenticated(false);

    }

    @Override
    public void updateAccountPwdUser(UpdatePwdRequest request) throws Exception {
        User user = userRepository.findById(request.getId())
                .orElseThrow(() -> new Exception("Utilisateur introuvable"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new Exception("Ancien mot de passe invalide.");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));

        user.setUpdatedAt(LocalDateTime.now());

        userRepository.save(user);

    }

    private List<AlertDto> alertClaimAndDenun(ClaimType type) {

        List<ClaimStatus> lStatus = Arrays.asList(ClaimStatus.CLASSED, ClaimStatus.SATISFIED, ClaimStatus.UNSATISFIED,
                ClaimStatus.PARTIAL_SATISFIED, ClaimStatus.TEMP_SAVED);
        if (type == ClaimType.DENUNCIACION) {
            lStatus = Arrays.asList(ClaimStatus.CLASSED, ClaimStatus.SATISFIED, ClaimStatus.UNSATISFIED,
                    ClaimStatus.PARTIAL_SATISFIED, ClaimStatus.TEMP_SAVED, ClaimStatus.TREAT);
        }
        List<Claim> allClaims = claimRepository.findByTypeAndStatusNotIn(type, lStatus);
        boolean isOneSolutionMeasured = false;
        List<AlertDto> claimAlertDtos = new ArrayList<>();
        AlertDto alertDto = AlertDto.builder().build();
        // List<Claim> TmpallClaims = allClaims;
        for (Claim claim : allClaims) {
            // vérifier s'il y a au moins une solution ne pas prendre en compte

            if (!claim.getSolutions().isEmpty() && type == ClaimType.CLAIM) {
                for (Solution solution : claim.getSolutions()) {
                    if (solution.getSatisfactionMeasure() != null) {
                        // allClaims.remove(claim);
                        isOneSolutionMeasured = true;
                        break;
                    }
                }
            }
            if (!isOneSolutionMeasured) {
                LocalDateTime calculateDate = claim.getReceiptDateTime().plusDays(claim.getObjet().getProcessingTime());
                if (LocalDateTime.now().isAfter(calculateDate)) {
                    Long hoursRetard = calculateDate.until(LocalDateTime.now(), ChronoUnit.HOURS);
                    Long days = hoursRetard / 24;
                    Long hours = hoursRetard % 24;
                    alertDto = AlertDto
                            .builder()
                            .claimClient(claim.getClientFirstAndLastName())
                            .claimCode(claim.getCode())
                            .claimId(claim.getId())
                            .retardDay(days + " jr(s) " + hours + " heure(s)")
                            .declenchedDate(calculateDate)
                            .receiptDateTime(claim.getReceiptDateTime())
                            .status(claim.getStatus())
                            .type(type)
                            .build();
                    claimAlertDtos.add(alertDto);
                }
            }
        }

        return claimAlertDtos;
    }

    @Override
    public HashMap<String, Object> getDashboard() {
        List<Claim> claims = claimRepository.findByTypeAndStatusNot(ClaimType.CLAIM, ClaimStatus.TEMP_SAVED);
        List<Claim> denuns = claimRepository.findByTypeAndStatusNot(ClaimType.DENUNCIACION, ClaimStatus.TEMP_SAVED);
        List<Suggestion> suggestions = suggestionRepository
                .findByStatusNot(ClaimStatus.TEMP_SAVED);
        List<ClaimStatus> treatClaimStatus = Arrays.asList(ClaimStatus.TREAT, ClaimStatus.SATISFIED,
                ClaimStatus.UNSATISFIED, ClaimStatus.CLASSED, ClaimStatus.LITIGATION, ClaimStatus.PARTIAL_SATISFIED);
        HashMap<String, Object> dashboard = new HashMap<String, Object>();
        dashboard.put("claims", claims.size());
        dashboard.put("denuns", denuns.size());
        dashboard.put("suggest", suggestions.size());
        dashboard.put("claimsAffected", 0);
        dashboard.put("claimsTreat", 0);
        // TOTAL Claim + DEnun + Suggest
        dashboard.put("plainteSuggest", claims.size() + denuns.size() + suggestions.size());
        // nombre de claim affecté
        int value = 0;
        int totalSatisfied = 0;
        for (Claim claim : claims) {
            if (claim.getStatus().equals(ClaimStatus.AFFECTED)) { // claim affected
                value = (int) dashboard.get("claimsAffected");
                dashboard.replace("claimsAffected", value + 1);
            } else if (treatClaimStatus.contains(claim.getStatus())) { // claimtreat
                value = (int) dashboard.get("claimsTreat");
                dashboard.replace("claimsTreat", value + 1);
                if (claim.getStatus().equals(ClaimStatus.SATISFIED)) {
                    totalSatisfied++;
                }
            }
        }
        // taux satisfaction
        List<ClaimStatus> status = Arrays.asList(ClaimStatus.SATISFIED);
       
        List<Claim> claimsTreat = new ArrayList<>();
        claimsTreat = claimRepository.findByTypeAndStatusIn(ClaimType.CLAIM, status);
    

        List<ClaimStatus> allSatisfaction = Arrays.asList(ClaimStatus.SATISFIED, ClaimStatus.UNSATISFIED,
                ClaimStatus.PARTIAL_SATISFIED,ClaimStatus.CLASSED,ClaimStatus.LITIGATION);
        List<Claim> allClaims = claimRepository.findByTypeAndStatusIn(ClaimType.CLAIM, allSatisfaction);

        // dashboard.put("tauxSatisfaction",
        //        Utils.percentCalculator(Long.valueOf(claimsTreat.size()), Long.valueOf(allClaims.size())));
        
         // Formater le résultat avec deux chiffres après la virgule
        DecimalFormat df = new DecimalFormat("#.00");
        String tauxSatisfactionFormate = df.format(Utils.percentCalculator(Long.valueOf(claimsTreat.size()), Long.valueOf(allClaims.size())));

        // Ajout au dashboard
        dashboard.put("tauxSatisfaction", tauxSatisfactionFormate);

        
                List<AlertDto> retardClaims = alertClaimAndDenun(ClaimType.CLAIM);
        retardClaims.addAll(alertClaimAndDenun(ClaimType.DENUNCIACION));
        dashboard.put("claimDenunRetard", retardClaims);
        dashboard.put("TotalclaimDenunRetard", retardClaims.size());

        return dashboard;
    }

    @Override
    public List<User> getEmailReceivers() {
        return userRepository.findByIsEmailReceiver(true);
    }

    @Override
    public User addEmailReceivers(AddEmailReceiver choosedIds) throws Exception {
        if (choosedIds.getPoste().equals("RA")) {
            User user = userRepository.findById(choosedIds.getIds())
                    .orElseThrow(() -> new Exception("Utilisateur sélectionné introuvable"));
            // verification existence d'un ancien RA pour le point de service de l'user
            Optional<User> existingRa = userRepository.findByIsEmailReceiverAndIsRaAndServicePoint(true, true,
                    user.getServicePoint());
            if (!existingRa.isPresent()) {
                user.setEmailReceiver(true);
                user.setRa(true);
                user.setCoodonateur(false);
                user.setTitre(choosedIds.getPoste());
                user = userRepository.save(user);
                return user;
            } else {
                throw new Exception(
                        "Un RA a déjà été selectionné pour le point de service " + user.getServicePoint().getLibelle());
            }

        } else if (choosedIds.getPoste().equals("COORDONNATEUR")) {
            User user = userRepository.findById(choosedIds.getIds())
                    .orElseThrow(() -> new Exception("Utilisateur sélectionné introuvable"));
            user.setEmailReceiver(true);
            user.setRa(false);
            user.setCoodonateur(true);
            user.setTitre(choosedIds.getPoste());
            user = userRepository.save(user);
            return user;
        } else {
            // DE, PILOTE, AUTRE, etc
            User user = userRepository.findById(choosedIds.getIds())
                    .orElseThrow(() -> new Exception("Utilisateur sélectionné introuvable"));
            user.setEmailReceiver(true);
            user.setRa(false);
            user.setCoodonateur(false);
            user.setTitre(choosedIds.getPoste());
            user = userRepository.save(user);
            return user;
        }

    }

    @Override
    public void removeEmailReceiver(Long id) throws Exception {
        User user = userRepository.findById(id).orElseThrow(() -> new Exception("Utilisateur sélectionné introuvable"));
        user.setEmailReceiver(false);
        user.setRa(false);
        user.setCoodonateur(false);
        user = userRepository.save(user);
    }

    @Override
    public List<User> getEmailReceiversForNotif(ServicePoint servicePointIndexe) {
        List<User> emailReceivers = this.getEmailReceivers();
        List<User> receivers = new ArrayList<>();

        for (User user : emailReceivers) {
            if (user.isRa() && user.getServicePoint().getId() == servicePointIndexe.getId()) {
                receivers.add(user);
            } else if (!user.isRa()) {
                receivers.add(user);
            }
        }
        System.err.println(receivers);
        return receivers;
    }

    public List<User> getUsersByServicePoint(ServicePoint servicePoint) {
        return userRepository.findByServicePoint(servicePoint);
    }

    public User findRaByServicePoint(Long servicePoint) {
        return userRepository.findRaByServicePointId(servicePoint)
                .orElse(null);  // Retourne null si aucun RA n'est trouvé
    }
}
