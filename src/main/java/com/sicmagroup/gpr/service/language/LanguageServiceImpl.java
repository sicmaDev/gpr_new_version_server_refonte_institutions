package com.sicmagroup.gpr.service.language;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.stereotype.Service;

import com.sicmagroup.gpr.domain.model.Language;
import com.sicmagroup.gpr.domain.model.Poste;
import com.sicmagroup.gpr.domain.model.Product;
import com.sicmagroup.gpr.repository.LanguageRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LanguageServiceImpl implements LanguageService {

    private final LanguageRepository repository;

    @Override
    public List<Language> getAll() {
        return repository.findByIsDeleted(false);
    }

    @Override
    public List<Language> getAllDeleted() {
        return repository.findByIsDeleted(true);
    }

    @Override
    public Language getById(Long id) throws NotFoundException {
        return repository.findById(id).orElseThrow(() -> new NotFoundException());
    }

    @Override
    public Language saveLanguage(Language language) {
        language.setUuid(generateUuid());
        return repository.save(language);
    }

    @Override
    public Language updateLanguage(Language language) {
        return repository.save(language);
    }

    @Override
    public Language deleteTempLanguage(Long id) throws NotFoundException {
        Language language = repository.findById(id).orElseThrow(() -> new NotFoundException());
        language.setDeleted(true);
        language.setDeletedAt(LocalDateTime.now());
        language = repository.save(language);
        return language;
    }

    @Override
    public void deleteLanguage(Language language) throws Exception {
        try {
            repository.delete(language);
        } catch (Exception e) {
            throw new Exception("Impossible de supprimer cette langue car il intervient dans plusieurs opérations.");
        }
    }

    @Override
    public Language getDeletedById(Long id, boolean deleted) throws NotFoundException {
        return repository.findByIdAndIsDeleted(id, deleted).orElseThrow(() -> new NotFoundException());

    }
     private String generateUuid() {
        String code = "lg-" + UUID.randomUUID().toString().substring(0, 5);

        while (repository.findByUuid(code).isPresent()) {
            code = "lg-" + UUID.randomUUID().toString().substring(0, 5);
        }

        return code;
    }
     @Override
    public Language findByUuid(String uuid){
        try {
            return repository.findByUuid(uuid).orElseThrow(() -> new Exception("Cette langue  n'existe pas."));
        } catch (Exception e) {
            throw new RuntimeException("Cette langue  n'existe pas.", e);
        }
    }

}
