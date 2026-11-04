package com.sicmagroup.gpr.service.existingSolution;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.sicmagroup.gpr.api.config.existingSolution.AddExistingSolutionRequest;
import com.sicmagroup.gpr.api.config.existingSolution.UpdateExistingSolutionRequest;
import com.sicmagroup.gpr.domain.model.ExistingSolution;
import com.sicmagroup.gpr.domain.model.Objet;
import com.sicmagroup.gpr.repository.ExistingSolutionRepository;
import com.sicmagroup.gpr.repository.ObjetRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExistingSolutionServiceImpl implements ExistingSolutionService {

    private final ExistingSolutionRepository repository;
    private final ObjetRepository objetRepository;

    @Override
    public List<ExistingSolution> getAll() {
        return repository.findAll();
    }

    // Une même solution (sans tenir compte des majuscules ni des espaces en trop) ne peut exister
    // qu'une fois pour un objet : sinon elle s'affiche en double au traitement. idExclu : la solution modifiée.
    private void verifierDoublon(Objet objet, String contenu, Long idExclu) throws Exception {
        String cle = normaliser(contenu);
        boolean doublon = repository.findByObjet(objet).stream()
                .anyMatch(s -> !s.getId().equals(idExclu) && normaliser(s.getContent()).equals(cle));
        if (doublon) {
            throw new Exception("Cette solution existe déjà pour l'objet « " + objet.getLibelle() + " ».");
        }
    }

    private static String normaliser(String texte) {
        return texte == null ? "" : texte.trim().replaceAll("\\s+", " ").toLowerCase();
    }

    @Override
    public ExistingSolution saveOne(AddExistingSolutionRequest request) throws Exception {
        Objet objet = objetRepository.findById(request.getObjet()).orElseThrow(
                () -> new Exception("L'objet sélectionné n'existe pas. Réessayez ou contactez le support."));
        verifierDoublon(objet, request.getContent(), null);
        ExistingSolution existingSolution = ExistingSolution
                .builder()
                .content(request.getContent())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .objet(objet)
                .build();
        existingSolution = repository.save(existingSolution);

        return existingSolution;
    }

    @Override
    public ExistingSolution updateOne(UpdateExistingSolutionRequest request) throws Exception {
        ExistingSolution existingSolution = repository.findById(request.getId())
                .orElseThrow(() -> new Exception("La solution sélectionnée n'existe pas. Réessayez ou contactez le support."));
        Objet objet = objetRepository.findById(request.getObjet()).orElseThrow(
                () -> new Exception("L'objet sélectionné n'existe pas. Réessayez ou contactez le support."));
        verifierDoublon(objet, request.getContent(), existingSolution.getId());
        existingSolution.setContent(request.getContent());
        existingSolution.setObjet(objet);
                existingSolution.setUpdatedAt(LocalDateTime.now());
        existingSolution = repository.save(existingSolution);
        return existingSolution;
    }

    @Override
    public void removeOne(Long id) throws Exception {
        if(id != null){
             try {
            repository.deleteById(id);
        } catch (Exception e) {
            throw new Exception("Erreur, la solution a déjà été utilisée pour un ou plusieurs traitement.");
        }
        } else {
            throw new Exception("Invalide identifier");
        }
       
        
    }

    @Override
    public List<ExistingSolution> getAllByObjet(Long id) throws Exception {
        Objet objet = objetRepository.findById(id).orElseThrow(() -> new Exception("Objet introuvable"));

        List<ExistingSolution> rExistingSolutions = repository.findByObjet(objet);
        return rExistingSolutions;
    }

}
