package com.sicmagroup.gpr.api.config.setting;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data 
@AllArgsConstructor 
@NoArgsConstructor
@Builder
public class MailRequest {
    private String host;
    private String port;
    private String user;
    private String pwd;
}
