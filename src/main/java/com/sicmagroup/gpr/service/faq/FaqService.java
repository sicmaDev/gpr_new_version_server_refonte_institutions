package com.sicmagroup.gpr.service.faq;

import java.util.List;

import javax.naming.NameNotFoundException;

import com.sicmagroup.gpr.api.config.faq.FaqRequest;
import com.sicmagroup.gpr.api.config.faq.HelpResponse;
import com.sicmagroup.gpr.domain.model.Faq;

public interface FaqService {
    
    public Faq storeFaq(FaqRequest request);
    public Faq updateFaq(FaqRequest request) throws NameNotFoundException;
    public List<Faq> allFaqs();
    public void deleteFaq(Long id)  throws Exception;

    public HelpResponse getHelp();
}
