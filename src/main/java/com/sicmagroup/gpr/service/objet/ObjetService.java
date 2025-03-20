package com.sicmagroup.gpr.service.objet;

import java.util.List;

import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.transaction.annotation.Transactional;

import com.sicmagroup.gpr.domain.model.Objet;

public interface ObjetService {

    public List<Objet> getAll();

    public List<Objet> getAllDeleted();

    public Objet getById(Long id) throws NotFoundException;

    public Objet saveObjet(Objet objet);

    public Objet updateObjet(Objet objet);

    public Objet deleteTempObjet(Long id) throws NotFoundException;

    public void deleteObjet(Objet objet) throws Exception ; 

    public Objet getDeletedById(Long id, boolean deleted) throws NotFoundException;
}

