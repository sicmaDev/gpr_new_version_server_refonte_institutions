package com.sicmagroup.gpr.service.setting;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.sicmagroup.gpr.api.config.setting.AddSettingRequest;
import com.sicmagroup.gpr.api.config.setting.BotRequest;
import com.sicmagroup.gpr.api.config.setting.InstitutionRequest;
import com.sicmagroup.gpr.api.config.setting.MailRequest;
import com.sicmagroup.gpr.api.config.setting.SmsRequest;
import com.sicmagroup.gpr.api.config.setting.UpdateSettingRequest;
import com.sicmagroup.gpr.domain.dto.CategorieObjetDto;
import com.sicmagroup.gpr.domain.dto.ClaimDto;
import com.sicmagroup.gpr.domain.dto.Client;
import com.sicmagroup.gpr.domain.dto.CollectionChannelDto;
import com.sicmagroup.gpr.domain.dto.ExistingSolutionResponse;
import com.sicmagroup.gpr.domain.dto.ExternalRecourseDto;
import com.sicmagroup.gpr.domain.dto.LanguageDto;
import com.sicmagroup.gpr.domain.dto.LicenseResponse;
import com.sicmagroup.gpr.domain.dto.PosteDto;
import com.sicmagroup.gpr.domain.dto.ProductDto;
import com.sicmagroup.gpr.domain.dto.ServicePointDto;
import com.sicmagroup.gpr.domain.dto.SuggestionDto;
import com.sicmagroup.gpr.domain.dto.UserDto;
import com.sicmagroup.gpr.domain.dto.claimResponse.ObjetResponse;
import com.sicmagroup.gpr.domain.enumeration.ClaimType;
import com.sicmagroup.gpr.domain.enumeration.ConfigExportEnum;
import com.sicmagroup.gpr.domain.model.CategorieObjet;
import com.sicmagroup.gpr.domain.model.Claim;
import com.sicmagroup.gpr.domain.model.CollectionChannel;
import com.sicmagroup.gpr.domain.model.ExistingSolution;
import com.sicmagroup.gpr.domain.model.ExternalRecourse;
import com.sicmagroup.gpr.domain.model.Language;
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
import com.sicmagroup.gpr.repository.LogRepository;
import com.sicmagroup.gpr.repository.ObjetRepository;
import com.sicmagroup.gpr.repository.PosteRepository;
import com.sicmagroup.gpr.repository.ProductRepository;
import com.sicmagroup.gpr.repository.ServicePointRepository;
import com.sicmagroup.gpr.repository.SettingRepository;
import com.sicmagroup.gpr.repository.SolutionRepository;
import com.sicmagroup.gpr.repository.SuggestionRepository;
import com.sicmagroup.gpr.repository.UserRepository;
import com.sicmagroup.gpr.service.auth.AuthenticationServiceImpl;
import com.sicmagroup.gpr.service.faq.FaqServiceImpl;
import com.sicmagroup.gpr.utils.Constante;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SettingServiceImpl implements SettingService {

    private final SettingRepository repository;
    private final PosteRepository posteRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final CategorieObjetRepository cObjetRepository;
    private final ObjetRepository objetRepository;
    private final SolutionRepository solutionRepository;
    private final LanguageRepository languageRepository;
    private final CollectionChannelRespository channelRespository;
    private final LogRepository logRepository;
    private final ExternalRecourseRepository externalRecourseRepository;
    private final ClaimRepository claimRepository;
    private final SuggestionRepository suggestionRepository;
    private final FaqServiceImpl faqServiceImpl;
    private final ServicePointRepository sPointRepository;
    private final ExistingSolutionRepository existingSolutionRepository;
    private final ModelMapper modelMapper;

    @Override
    public List<Setting> getAll() {
        return repository.findAll();
    }

    @Override
    public Setting save(AddSettingRequest request) {
        Setting setting = Setting
                .builder()
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .libelle(request.getLibelle())
                .value(request.getValue())

                .build();
        return repository.save(setting);
    }

    @Override
    public Setting update(UpdateSettingRequest request) throws Exception {
        Setting oldSetting = repository.findByLibelle(request.getLibelle())
                .orElseThrow(() -> new Exception("The choosen setting doesn't exist"));

        oldSetting.setUpdatedAt(LocalDateTime.now());
        oldSetting.setValue(request.getValue());
        return repository.save(oldSetting);
    }

    @Override
    public Setting getbySlug(String slug) throws Exception {
        Setting setting = repository.findByLibelle(slug)
                .orElseThrow(() -> new Exception("The choosen setting doesn't exist"));
        return setting;
    }

    @Override
    public void updateLicence() {
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
            String license = sb.toString();
            // System.out.println("in the try catch");
            // System.out.println(license);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode licenseObj = mapper.readTree("" + license + "");
            // System.out.println(licenseObj.get("clients").get(0));
            if (licenseObj != null && license != "") {
                String url = "https://lm.gprserver.com/api/v1/license/updateLicenceClientSide";
                RestTemplate restTemplate = new RestTemplate();
                LicenseResponse licenseResponse = new LicenseResponse();
                Client client = new Client(0, licenseObj.get("company").asText(), licenseObj.get("company").asText(),
                        licenseObj.get("email").asText(), licenseObj.get("activationRequest").asText(),
                        LocalDateTime.now(), LocalDateTime.now());
                Set<Client> clients = new HashSet<Client>();
                clients.add(client);
                licenseResponse.setClients(clients);
                licenseResponse.setSerial(licenseObj.get("serial").asText());
                ResponseEntity<LicenseResponse> result = restTemplate.postForEntity(url, licenseResponse,
                        LicenseResponse.class);
                System.out.println("in the function 2");
                // System.out.println(result);
                if (result != null && result.getBody() != null) {
                    try {
                        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
                        HashMap<String, Object> licenseMap = new HashMap<>();
                        licenseMap.put("id", result.getBody().getId());
                        Client c = (Client) result.getBody().getClients().toArray()[0];
                        System.out.println("suite ");
                        System.out.println(c);
                        licenseMap.put("fullname", c.getFullname());
                        licenseMap.put("company", c.getCompany());
                        licenseMap.put("serial", result.getBody().getSerial());
                        licenseMap.put("email", c.getEmail());
                        licenseMap.put("activationRequest", c.getActivationRequest());
                        licenseMap.put("createdAt", c.getCreatedAt().toString());
                        licenseMap.put("updatedAt", c.getUpdatedAt().toString());
                        System.out.println(licenseMap);
                        String json = ow.writeValueAsString(licenseMap);

                        FileWriter fw = new FileWriter("data.txt");
                        fw.write(json);
                        fw.close();
                        System.out.println("Le texte a été écrit avec succès");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    System.out.println("Le texte n'a été pas été écrit avec succès");
                }
            } else {
                System.out.println("Une erreur est survenue.");
            }
        } catch (NullPointerException eNullPointerException) {
            eNullPointerException.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
