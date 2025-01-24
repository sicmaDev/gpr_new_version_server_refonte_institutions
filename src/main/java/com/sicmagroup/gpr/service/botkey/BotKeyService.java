package com.sicmagroup.gpr.service.botkey;

import java.util.List;

import com.sicmagroup.gpr.api.claim.SaveRequest;
import com.sicmagroup.gpr.api.suggestion.SuggestionAddRequest;
import com.sicmagroup.gpr.domain.dto.ClaimDto;
import com.sicmagroup.gpr.domain.dto.SuggestionDto;
import com.sicmagroup.gpr.domain.dto.botkey.BotKeyConfigResponse;
import com.sicmagroup.gpr.domain.enumeration.ClaimType;
import com.sicmagroup.gpr.domain.model.Claim;
import com.sicmagroup.gpr.domain.model.Suggestion;

import jakarta.servlet.http.HttpServletRequest;

public interface BotKeyService {
    public Boolean checkApiKeyBoolean(HttpServletRequest httpServletRequest);
    
    public BotKeyConfigResponse getConfig();

    
    public List<SuggestionDto> getSuggestions(String userCode);
    public SuggestionDto getSuggestion(String code);
    public SuggestionDto saveSuggestion (SuggestionAddRequest request,String botName) throws Exception;
    
    
    
    public List<ClaimDto> getClaims(String userCode,ClaimType claimType);
    public Claim getClaim(String code);
    public ClaimDto saveClaim (SaveRequest request,String botName,ClaimType claimType) throws Exception;



    public Claim updateClaim(Long code);

    public Claim deleteClaim(Long code);

    public Claim addFile(Long code);

    public Claim mesureSatistafaction(Long code);

}
