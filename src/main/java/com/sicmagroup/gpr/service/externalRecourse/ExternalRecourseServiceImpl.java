package com.sicmagroup.gpr.service.externalRecourse;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.stereotype.Service;

import com.sicmagroup.gpr.domain.model.ExternalRecourse;
import com.sicmagroup.gpr.domain.model.Objet;
import com.sicmagroup.gpr.repository.ExternalRecourseRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExternalRecourseServiceImpl implements ExternalRecourseService {

    private final ExternalRecourseRepository repository;

    @Override
    public List<ExternalRecourse> getAll() {
        return repository.findByIsDeleted(false);
    }

    @Override
    public List<ExternalRecourse> getAllDeleted() {
        return repository.findByIsDeleted(true);
    }

    @Override
    public ExternalRecourse getById(Long id) throws Exception {
        return repository.findById(id).orElseThrow(() -> new Exception("Le recours externe n'existe pas"));
    }

    @Override
    public ExternalRecourse saveExternalRecourse(ExternalRecourse externalRecourse) {
        return repository.save(externalRecourse);
    }

    @Override
    public ExternalRecourse updateExternalRecourse(ExternalRecourse externalRecourse) {
        return repository.save(externalRecourse);
    }

    @Override
    public ExternalRecourse deleteTempExternalRecourse(Long id) throws NotFoundException {
        ExternalRecourse externalRecourse = repository.findById(id).orElseThrow(() -> new NotFoundException());
        externalRecourse.setDeleted(true);
        externalRecourse.setDeletedAt(LocalDateTime.now());
        externalRecourse = repository.save(externalRecourse);
        return externalRecourse;
    }

    @Override
    public void deleteExternalRecourse(ExternalRecourse externalRecourse) throws Exception {
        try {
            repository.delete(externalRecourse);
        } catch (Exception e) {
            throw new Exception(
                    "Impossible de supprimer cet recours externe car il intervient dans plusieurs opérations.");
        }
    }

    @Override
    public ExternalRecourse getDeletedById(Long id, boolean deleted) throws NotFoundException {
        return repository.findByIdAndIsDeleted(id, deleted).orElseThrow(() -> new NotFoundException());
    }

}
