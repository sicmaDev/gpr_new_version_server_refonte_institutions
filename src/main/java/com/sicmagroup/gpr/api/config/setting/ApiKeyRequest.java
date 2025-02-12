package com.sicmagroup.gpr.api.config.setting;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data 
@AllArgsConstructor 
@NoArgsConstructor
@Builder
public class ApiKeyRequest {
    private String libelle;
    private String description;
    private String api_key;
    private String api_secret;
}
