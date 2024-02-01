package com.sicmagroup.gpr.service.faq;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import javax.naming.NameNotFoundException;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import com.sicmagroup.gpr.api.config.faq.FaqRequest;
import com.sicmagroup.gpr.api.config.faq.HelpResponse;
import com.sicmagroup.gpr.domain.dto.DocumentationDto;
import com.sicmagroup.gpr.domain.model.Documentation;
import com.sicmagroup.gpr.domain.model.Faq;
import com.sicmagroup.gpr.repository.DocumentationRepository;
import com.sicmagroup.gpr.repository.FaqRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FaqServiceImpl implements FaqService {

    private final FaqRepository faqRepository;
    private final DocumentationRepository dRepository;
         private final ModelMapper modelMapper;


    @Override
    public Faq storeFaq(FaqRequest request) {
        Faq faq = Faq
                .builder()
                .libelle(request.getLibelle())
                .answer(request.getContenu())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        faq = faqRepository.save(faq);
        return faq;
    }

    @Override
    public Faq updateFaq(FaqRequest request) throws NameNotFoundException {
        Faq faq = faqRepository.findById(Long.valueOf(request.getId()))
                .orElseThrow(() -> new NameNotFoundException("Cette FAQ est introuvable"));
        faq.setLibelle(request.getLibelle());
        faq.setAnswer(request.getContenu());
        faq.setUpdatedAt(LocalDateTime.now());
        faq = faqRepository.save(faq);
        return faq;
    }

    @Override
    public List<Faq> allFaqs() {
        return faqRepository.findAll();
    }

    @Override
    public void deleteFaq(Long id) throws Exception {
        Faq faq = faqRepository.findById(id).orElseThrow(() -> new NameNotFoundException("Cette FAQ est introuvable"));
        
        faqRepository.delete(faq);
    }

    @Override
    public HelpResponse getHelp() {
        HelpResponse helpResponse = HelpResponse
                .builder()
                .faqs(faqRepository.findAll())
                .docs(dRepository.findAll().stream().map(this::convertToDto).collect(Collectors.toList()))
                .build();
        return helpResponse;

    }

    
        private DocumentationDto convertToDto(Documentation documentation){
        DocumentationDto documentationDto =  modelMapper.map(documentation, DocumentationDto.class);
        return documentationDto;
    }

}
