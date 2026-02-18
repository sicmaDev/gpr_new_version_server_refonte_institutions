package com.sicmagroup.gpr.api.superset;

import com.sicmagroup.gpr.service.superset.SupersetService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@CrossOrigin(origins = "http://localhost:3000")
public class SupersetController {

    private final SupersetService supersetService;

    public SupersetController(SupersetService supersetService) {
        this.supersetService = supersetService;
    }

    @GetMapping("/api/guest-token")
    public org.springframework.http.ResponseEntity<?> getGuestToken(@RequestParam String dashboardId) {
        try {
            String token = supersetService.getGuestToken(dashboardId);
            Map<String, String> response = new HashMap<>();
            response.put("token", token);
            return org.springframework.http.ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return org.springframework.http.ResponseEntity.status(500).body(error);
        }
    }
}
