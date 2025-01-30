package com.sicmagroup.gpr.service.language;

import java.util.List;

import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;

import com.sicmagroup.gpr.domain.model.Language;


public interface LanguageService {
    public List<Language> getAll();

    public List<Language> getAllDeleted();

    public Language getById(Long id) throws NotFoundException;

    public Language saveLanguage(Language language);

    public Language updateLanguage(Language language);

    public Language deleteTempLanguage(Long id) throws NotFoundException;

    public void deleteLanguage(Language language) throws Exception ;

    public Language getDeletedById(Long id, boolean deleted) throws NotFoundException;
    
    public Language findByUuid(String uuid) throws NotFoundException;
}
