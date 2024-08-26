package com.sicmagroup.gpr.api.config.setting;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data 
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MailTestRequest {
    public String message;
    public String to;
    public String subject;
}
