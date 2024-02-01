package com.sicmagroup.gpr.service.poste;

import java.util.List;

import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;

import com.sicmagroup.gpr.domain.model.Poste;

public interface PosteService {
    public List<Poste> getAll();

    public List<Poste> getAllDeleted();

    public Poste getById(Long id) throws NotFoundException;

    public Poste savePoste(Poste poste);

    public Poste updatePoste(Poste poste);

    public Poste deleteTempPoste(Long id) throws NotFoundException;

    public void deletePoste( Poste poste) throws Exception;

    public Poste getDeletedById(Long id, boolean deleted) throws NotFoundException;
}
