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

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.sicmagroup.gpr.api.config.setting.AddSettingRequest;
import com.sicmagroup.gpr.api.config.setting.UpdateSettingRequest;
import com.sicmagroup.gpr.domain.dto.Client;
import com.sicmagroup.gpr.domain.dto.LicenseResponse;
import com.sicmagroup.gpr.domain.model.Setting;
import com.sicmagroup.gpr.repository.SettingRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SettingServiceImpl implements SettingService {

    private final SettingRepository repository;

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
        try
        {
            // Le fichier d'entrée
            File file = new File("data.txt");    
            
            // Créer l'objet File Reader
            FileReader fr = new FileReader(file);  
            
            // Créer l'objet BufferedReader        
            BufferedReader br = new BufferedReader(fr);  
            StringBuffer sb = new StringBuffer();    
            String line;
            while((line = br.readLine()) != null) 
            {
                // ajoute la ligne au buffer
                sb.append(line);      
                sb.append("\n");     
            }
            fr.close();    
            String license = sb.toString();
            // System.out.println("in the try catch");
            // System.out.println(license);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode licenseObj = mapper.readTree(""+license+"");
            // System.out.println(licenseObj.get("clients").get(0));
            if(licenseObj != null && license != ""){
                String url = "https://lm.gprserver.com/api/v1/license/updateLicenceClientSide";
                RestTemplate restTemplate = new RestTemplate();
                LicenseResponse licenseResponse = new LicenseResponse();
                Client client = new Client(0, licenseObj.get("company").asText(), licenseObj.get("company").asText(), licenseObj.get("email").asText(), licenseObj.get("activationRequest").asText(),  LocalDateTime.now(),  LocalDateTime.now());
                Set<Client> clients =  new HashSet<Client>() ;
                clients.add(client);
                licenseResponse.setClients(clients);
                licenseResponse.setSerial(licenseObj.get("serial").asText());
                ResponseEntity<LicenseResponse> result = restTemplate.postForEntity(url,licenseResponse, LicenseResponse.class);
                System.out.println("in the function 2");
                // System.out.println(result);
                if(result != null && result.getBody() != null){
                    try {
                        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
                        HashMap<String, Object> licenseMap = new HashMap<>();
                        licenseMap.put("id", result.getBody().getId());
                        Client c = (Client) result.getBody().getClients().toArray()[0];
                        System.out.println("suite ");
                        System.out.println(c);
                        licenseMap.put("fullname",c.getFullname());
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
        }catch(NullPointerException eNullPointerException){
            eNullPointerException.printStackTrace();
        } catch(IOException e)
        {
          e.printStackTrace();
        }
    }

}
