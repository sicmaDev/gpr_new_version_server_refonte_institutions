package com.sicmagroup.gpr.api.config.faq;

import java.util.List;

import com.sicmagroup.gpr.domain.dto.DocumentationDto;
import com.sicmagroup.gpr.domain.model.Documentation;
import com.sicmagroup.gpr.domain.model.Faq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HelpResponse {
    private List<Faq> faqs;
    private List<DocumentationDto> docs;
}
