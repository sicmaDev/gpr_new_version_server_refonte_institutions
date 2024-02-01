package com.sicmagroup.gpr.service.categorieObjet;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.sicmagroup.gpr.api.config.categorieObjet.CategorieObjetRequest;
import com.sicmagroup.gpr.api.config.categorieObjet.UpdateCategorieObjetRequest;
import com.sicmagroup.gpr.domain.model.CategorieObjet;
import com.sicmagroup.gpr.repository.CategorieObjetRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CategorieObjetServiceImpl implements CategorieObjetService {

    private final CategorieObjetRepository repository;

    @Override
    public List<CategorieObjet> getAll() {
        return repository.findAll();
    }

    @Override
    public CategorieObjet saveOne(CategorieObjetRequest request) {
        CategorieObjet categorieObjet = CategorieObjet
                .builder()
                .libelle(request.getLibelle())
                .description(request.getDescription())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return repository.save(categorieObjet);

    }

    @Override
    public CategorieObjet updateOne(UpdateCategorieObjetRequest request) throws Exception {
        CategorieObjet categorieObjet = repository.findById(request.getId())
                .orElseThrow(() -> new Exception("La categorie à mettre à jour est introuvable"));
        categorieObjet.setLibelle(request.getLibelle());
        categorieObjet.setDescription(request.getDescription());
        categorieObjet.setUpdatedAt(LocalDateTime.now());
        return repository.save(categorieObjet);
    }

    @Override
    public void removeOne(Long id) throws Exception {
        CategorieObjet categorieObjet = repository.findById(id)
                .orElseThrow(() -> new Exception("La categorie à supprimer est introuvable"));
        try {
            repository.delete(categorieObjet);
        } catch (Exception e) {
            throw new Exception("La catégorie est déjà liée à des objets de réclamation, impossible de la supprimé.");
        }
    }

    @Override
    public CategorieObjet getOneById(Long id) throws Exception {
        return repository.findById(id).orElseThrow(() -> new Exception("La categorie d'objet est introuvable"));
    }

}
