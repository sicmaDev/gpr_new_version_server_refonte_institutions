package com.sicmagroup.gpr.service.poste;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.stereotype.Service;

import com.sicmagroup.gpr.domain.model.Poste;
import com.sicmagroup.gpr.domain.model.ServicePoint;
import com.sicmagroup.gpr.repository.PosteRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PosteServiceImpl implements PosteService {

    private final PosteRepository repository;

    @Override
    public List<Poste> getAll() {
        return repository.findByIsDeleted(false);
    }

    @Override
    public List<Poste> getAllDeleted() {
        return repository.findByIsDeleted(true);
    }

    @Override
    public Poste getById(Long id) throws NotFoundException {
        return repository.findById(id).orElseThrow(() -> new NotFoundException());
    }

    @Override
    public Poste savePoste(Poste poste) {
        return repository.save(poste);
    }

    @Override
    public Poste updatePoste(Poste poste) {
        return repository.save(poste);
    }

    @Override
    public Poste deleteTempPoste(Long id) throws NotFoundException {
        Poste poste = repository.findById(id).orElseThrow(() -> new NotFoundException());
        poste.setDeleted(true);
        poste.setDeletedAt(LocalDateTime.now());
        poste = repository.save(poste);
        return poste;
    }

    @Override
    public void deletePoste(Poste poste) throws Exception {
        try {
            repository.delete(poste);
        } catch (Exception e) {
            throw new Exception("Impossible de supprimer cet poste car il est associé à un ou plusieurs utilisateurs.");
        }
    }

    @Override
    public Poste getDeletedById(Long id, boolean deleted) throws NotFoundException {
        return repository.findByIdAndIsDeleted(id, deleted).orElseThrow(() -> new NotFoundException());
    }

}
