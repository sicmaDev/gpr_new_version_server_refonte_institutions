package com.sicmagroup.gpr.service.superset;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class SupersetService {

    @Value("${superset.url}")
    private String supersetUrl;

    @Value("${superset.admin.username}")
    private String adminUsername;

    @Value("${superset.admin.password}")
    private String adminPassword;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public SupersetService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
    }

    public String getAccessToken() {
        try {
            String loginUrl = supersetUrl + "/api/v1/security/login";
            Map<String, String> loginRequest = new HashMap<>();
            loginRequest.put("username", adminUsername);
            loginRequest.put("password", adminPassword);
            loginRequest.put("provider", "db");
            loginRequest.put("refresh", "true");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(loginRequest, headers);

            ResponseEntity<JsonNode> response = restTemplate.postForEntity(loginUrl, entity, JsonNode.class);
            return response.getBody().get("access_token").asText();
        } catch (Exception e) {
            System.err.println("Error getting access token: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    public String getGuestToken(String dashboardId) {
        try {
            String accessToken = getAccessToken();
            String guestTokenUrl = supersetUrl + "/api/v1/security/guest_token/";

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> guestTokenRequest = new HashMap<>();
            Map<String, String> user = new HashMap<>();
            user.put("username", "guest");
            user.put("first_name", "Guest");
            user.put("last_name", "User");
            guestTokenRequest.put("user", user);

            Map<String, String> resource = new HashMap<>();
            resource.put("type", "dashboard");
            resource.put("id", dashboardId);
            guestTokenRequest.put("resources", java.util.Collections.singletonList(resource));
            
            guestTokenRequest.put("rls", java.util.Collections.emptyList());

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(guestTokenRequest, headers);

            ResponseEntity<JsonNode> response = restTemplate.postForEntity(guestTokenUrl, entity, JsonNode.class);
            return response.getBody().get("token").asText();
        } catch (Exception e) {
            System.err.println("Error getting guest token: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
}
