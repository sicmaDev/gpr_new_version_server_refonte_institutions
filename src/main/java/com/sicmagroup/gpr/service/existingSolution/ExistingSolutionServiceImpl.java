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

    @Override
    public ExistingSolution saveOne(AddExistingSolutionRequest request) throws Exception {
        ExistingSolution existingSolution = ExistingSolution
                .builder()
                .content(request.getContent())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .objet(objetRepository.findById(request.getObjet()).orElseThrow(
                        () -> new Exception("L'objet sélectionné n'existe pas. Réessayez ou contactez le support.")))
                        
                .build();
        existingSolution = repository.save(existingSolution);

        return existingSolution;
    }

    @Override
    public ExistingSolution updateOne(UpdateExistingSolutionRequest request) throws Exception {
        ExistingSolution existingSolution = repository.findById(request.getId())
                .orElseThrow(() -> new Exception("La solution sélectionnée n'existe pas. Réessayez ou contactez le support."));
        existingSolution.setContent(request.getContent());
        existingSolution.setObjet(objetRepository.findById(request.getObjet()).orElseThrow(
                () -> new Exception("L'objet sélectionné n'existe pas. Réessayez ou contactez le support.")));
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
