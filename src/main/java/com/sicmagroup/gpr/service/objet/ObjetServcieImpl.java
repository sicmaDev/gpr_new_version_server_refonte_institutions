package com.sicmagroup.gpr.service.objet;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.stereotype.Service;

import com.sicmagroup.gpr.domain.model.Objet;
import com.sicmagroup.gpr.repository.ObjetRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ObjetServcieImpl implements ObjetService {

    private final ObjetRepository repository;

    @Override
    public List<Objet> getAll() {
        return repository.findByIsDeleted(false);
    }

    @Override
    public List<Objet> getAllDeleted() {
        return repository.findByIsDeleted(true);
    }

    @Override
    public Objet getById(Long id) throws NotFoundException {
        return repository.findById(id).orElseThrow(() -> new NotFoundException());
    }

    @Override
    public Objet saveObjet(Objet objet) {
        objet.setUuid(generateUuid());
        return repository.save(objet);
    }

    @Override
    public Objet updateObjet(Objet objet) {
        return repository.save(objet);
    }

    @Override
    public Objet deleteTempObjet(Long id) throws NotFoundException {
        Objet objet = repository.findById(id).orElseThrow(() -> new NotFoundException());
        objet.setDeleted(true);
        objet.setDeletedAt(LocalDateTime.now());
        objet = repository.save(objet);
        return objet;
    }

    @Override
    public void deleteObjet(Objet objet) throws Exception {
        try {
        repository.delete(objet);
    } catch (Exception e) {
        throw new Exception("Impossible de supprimer cet objet car il intervient dans plusieurs opérations.");
    }
    }

    @Override
    public Objet getDeletedById(Long id, boolean deleted) throws NotFoundException {
        return repository.findByIdAndIsDeleted(id, deleted).orElseThrow(() -> new NotFoundException());

    }

     private String generateUuid() {
        String code = "obj-" + UUID.randomUUID().toString().substring(0, 5);

        while (repository.findByUuid(code).isPresent()) {
            code = "obj-" + UUID.randomUUID().toString().substring(0, 5);
        }

        return code;
    }

}
