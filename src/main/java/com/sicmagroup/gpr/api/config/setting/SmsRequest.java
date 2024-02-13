package com.sicmagroup.gpr.api.config.setting;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data 
@AllArgsConstructor 
@NoArgsConstructor
@Builder
public class SmsRequest {
    public String url;
    public String libEmetteur;
    public String valEmetteur;
    public String libDestinataire;
    public String libMessage;
    public String libId;
    public String valId;
    public String libMdp;
    public String valMdp;

    
}
