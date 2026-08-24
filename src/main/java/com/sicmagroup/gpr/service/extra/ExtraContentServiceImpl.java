package com.sicmagroup.gpr.service.extra;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.stereotype.Service;

import com.sicmagroup.gpr.api.extra.ExtraRequest;
import com.sicmagroup.gpr.domain.model.ExtraContent;
import com.sicmagroup.gpr.domain.model.User;
import com.sicmagroup.gpr.repository.ExtraContentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExtraContentServiceImpl implements ExtraContentService {

    private final ExtraContentRepository repository;

    @Override
    public List<ExtraContent> getAll(ExtraContent extraContent) {
        return repository.findAll();
    }

    @Override
    public ExtraContent getById(Long id) throws NotFoundException {
        return repository.findById(id).orElseThrow();

    }

    @Override
    public ExtraContent saveExtraContent(ExtraContent extraContent) throws Exception {

        return repository.save(extraContent);
    }

    @Override
    public void deleteExtraContent(Long id, User connectedUser) throws Exception {
        ExtraContent extraContent = repository.findById(id).orElseThrow();

        if (extraContent.getUser() == null || !extraContent.getUser().getId().equals(connectedUser.getId())) {
            throw new Exception("Vous n'êtes pas autorisé à supprimer ce contenu");
        }

        repository.delete(extraContent);
    }

}