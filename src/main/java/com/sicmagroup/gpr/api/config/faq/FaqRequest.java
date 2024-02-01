package com.sicmagroup.gpr.api.config.faq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FaqRequest {
    private Long id;
    private String libelle;
    private String contenu;

}
