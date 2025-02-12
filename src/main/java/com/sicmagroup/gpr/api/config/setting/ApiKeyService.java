package com.sicmagroup.gpr.api.config.setting;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sicmagroup.gpr.domain.model.Setting;
import com.sicmagroup.gpr.service.setting.SettingServiceImpl;
import com.sicmagroup.gpr.utils.Constante;

// @Service
public class ApiKeyService {

    // @Autowired
    // private final SettingServiceImpl serviceImpl=null;
    // private final PasswordEncoder passwordEncoder=null;


    // public boolean validateApiKey(String key, String secret) {
    //     try {
    //         Setting setting = serviceImpl.getbySlug(Constante.API_KEY_SLUG);
    //         ObjectMapper objectMapper = new ObjectMapper();
    //         ApiKeyRequest apiKeyRequest = objectMapper.readValue(setting.getValue(), ApiKeyRequest.class);

    //         return apiKeyRequest.getApi_key().equals(key) && passwordEncoder.matches(apiKeyRequest.getApi_secret(), secret);
    //     }catch (Exception e) {
        
    //         return false;
    //     }
    // }
}
