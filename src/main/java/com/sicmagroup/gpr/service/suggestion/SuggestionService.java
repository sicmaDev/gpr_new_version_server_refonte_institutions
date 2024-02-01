package com.sicmagroup.gpr.service.suggestion;

import java.util.List;

import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;

import com.sicmagroup.gpr.api.suggestion.SuggestionAddRequest;
import com.sicmagroup.gpr.api.suggestion.TreatSuggestionRequest;
import com.sicmagroup.gpr.domain.enumeration.ClaimStatus;
import com.sicmagroup.gpr.domain.model.Suggestion;
import com.sicmagroup.gpr.domain.model.User;

public interface SuggestionService {
    
    public List<Suggestion> getAll();

    public List<Suggestion> getAllByStatus(ClaimStatus status);

    public Suggestion getById(Long id) throws Exception;

    public Suggestion saveSuggestion(SuggestionAddRequest request, ClaimStatus status) throws Exception;

    public Suggestion tempSaveSuggestion(SuggestionAddRequest request);

    public Suggestion treatSuggestion(Suggestion suggestion, User treator,TreatSuggestionRequest request) throws Exception;

    public List<Suggestion> getAllByStatusNot(ClaimStatus status);

    public List<Suggestion> getAllByStatusIn(List<ClaimStatus> status);

    public List<Suggestion> getAllByCollectorAndStatus(User collector, ClaimStatus status);

    public void saveSuggestionOffline(SuggestionAddRequest request, ClaimStatus status) throws Exception;

    
 


}
