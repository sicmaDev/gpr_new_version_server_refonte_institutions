package com.sicmagroup.gpr.api.config.faq;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sicmagroup.gpr.domain.dto.ApiResponseDto;
import com.sicmagroup.gpr.domain.dto.DocumentationDto;
import com.sicmagroup.gpr.domain.model.Documentation;
import com.sicmagroup.gpr.domain.model.Faq;
import com.sicmagroup.gpr.service.documentation.DocumentationServiceImpl;
import com.sicmagroup.gpr.service.faq.FaqServiceImpl;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

import javax.naming.NameNotFoundException;

import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/v1/config/faq")
@RequiredArgsConstructor

public class FaqController {

    private final FaqServiceImpl serviceImpl;
    private final ModelMapper modelMapper;
    private final DocumentationServiceImpl serviceDocImpl;

    @GetMapping(value = "/list")
    public ResponseEntity<ApiResponseDto> getFaqList() {
        ApiResponseDto apiResponseDto = ApiResponseDto
                .builder()
                .status(true)
                .content(serviceImpl.allFaqs())
                .build();
        return ResponseEntity.ok(apiResponseDto);
    }

    // @GetMapping(value = "/help")
    // public ResponseEntity<ApiResponseDto> getHelpList() {
    //      List<Documentation> rDocumentations = serviceDocImpl.list();
    //      List<Faq> rFaqs = serviceImpl.allFaqs();
    //     ApiResponseDto apiResponseDto = ApiResponseDto
    //             .builder()
    //             .status(true)
    //             .content(serviceImpl.getHelp())
    //             .build();
    //     return ResponseEntity.ok(apiResponseDto);
    // }

    @PostMapping(value = "/add")
    public ResponseEntity<ApiResponseDto> postMethodName(@RequestBody FaqRequest request) {
        ApiResponseDto apiResponseDto;

        Faq faq = serviceImpl.storeFaq(request);
        apiResponseDto = ApiResponseDto
                .builder()
                .status(true)
                .content(faq)
                .build();
        return ResponseEntity.ok(apiResponseDto);
    }

    @PutMapping(value = "/{id}/update")
    public ResponseEntity<ApiResponseDto> updateFaq(@PathVariable(name = "id") Long id,
            @RequestBody FaqRequest request) {
        request.setId(id);
        ApiResponseDto apiResponseDto;
        try {
            Faq faq = serviceImpl.updateFaq(request);
            apiResponseDto = ApiResponseDto
                    .builder()
                    .status(true)
                    .content(faq)
                    .build();
            
        } catch (NameNotFoundException e) {
            apiResponseDto = ApiResponseDto
                    .builder()
                    .status(false)
                    .content(e.getMessage())
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponseDto);
        }
        return ResponseEntity.ok(apiResponseDto);

    }

    @DeleteMapping(value = "/{id}/delete")
    public ResponseEntity<ApiResponseDto> deleteFaq(@PathVariable(name = "id") Long id) {
        ApiResponseDto apiResponseDto;
        try {
            serviceImpl.deleteFaq(id);
            apiResponseDto = ApiResponseDto
                    .builder()
                    .status(true)
                    .content("Faq supprimée")
                    .build();
            
        } catch (Exception e) {
            apiResponseDto = ApiResponseDto
                    .builder()
                    .status(false)
                    .content(e.getMessage())
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponseDto);
        }
        return ResponseEntity.ok(apiResponseDto);
    }


}
