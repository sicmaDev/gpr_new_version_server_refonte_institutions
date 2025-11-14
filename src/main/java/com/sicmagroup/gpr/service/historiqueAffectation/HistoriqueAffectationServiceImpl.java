package com.sicmagroup.gpr.service.historiqueAffectation;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sicmagroup.gpr.api.claim.AffectTreatmentRequest;
import com.sicmagroup.gpr.api.claim.HistoriqueAffectationResponse;
import com.sicmagroup.gpr.domain.model.Claim;
import com.sicmagroup.gpr.domain.model.HistoriqueAffectation;
import com.sicmagroup.gpr.domain.model.User;
import com.sicmagroup.gpr.repository.ClaimRepository;
import com.sicmagroup.gpr.repository.HistoriqueAffectationRepository;
import com.sicmagroup.gpr.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class HistoriqueAffectationServiceImpl implements HistoriqueAffectationService {
    
    private final HistoriqueAffectationRepository repository;
    private final ClaimRepository claimRepository;
    private final UserRepository userRepository;
    @Override
    public HistoriqueAffectation storeHistorique(AffectTreatmentRequest affectTreatmentRequest) throws Exception {
        try {
            Claim claim = claimRepository.findById(affectTreatmentRequest.getClaimId()).orElseThrow();
            User user = userRepository.findById(affectTreatmentRequest.getAffectToId()).orElseThrow();
            User userAffector = userRepository.findById(affectTreatmentRequest.getAffectorId()).orElseThrow();
            HistoriqueAffectation currentAffectation = getCurrentAffectation(affectTreatmentRequest.getClaimId());
            
            if (currentAffectation != null) {
                currentAffectation.setDateFinAffectation(LocalDateTime.now());
                currentAffectation.setUpdatedAt(LocalDateTime.now());
                repository.save(currentAffectation);
            }
     
        // Créer la nouvelle affectation
        HistoriqueAffectation nouvelleAffectation = HistoriqueAffectation.builder()
            .reclamationId(affectTreatmentRequest.getClaimId())
            .codePlainte(claim.getCodeClient())
            .typePlainte(claim.getType())
            .nomAgent(user.getFirstandlastname() )
            .emailAgent(user.getEmail())
            .telephoneAgent(user.getTel())
            .dateAffectation(LocalDateTime.now())
            .delaiJours(affectTreatmentRequest.getDelai().intValue())
            .contentMail(affectTreatmentRequest.getMessage())
            .mailEnvoye(false)
            .smsEnvoye(false)
            .affecteurId(affectTreatmentRequest.getAffectorId())
            .nomAffecteur(userAffector.getFirstandlastname())
            .emailAffecteur(userAffector.getEmail())
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
            
        return repository.save(nouvelleAffectation);
           } catch (Exception e) {
             throw e;
        }
    }
    
    @Override
    public List<HistoriqueAffectationResponse> getHistoriqueByClaimId(Long claimId) {
        List<HistoriqueAffectation> historique = repository.findByReclamationIdOrderByDateAffectationDesc(claimId);
        return historique.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }
    
    @Override
    public HistoriqueAffectation getCurrentAffectation(Long claimId) {
        List<HistoriqueAffectation> affectations = repository.findByReclamationIdOrderByDateAffectationDesc(claimId);
        return affectations.stream()
            .filter(h -> h.getDateFinAffectation() == null)
            .findFirst()
            .orElse(null);
    }
    
    @Override
    public List<HistoriqueAffectation> getHistoriqueLate() {
        return repository.findRetardAffectations(LocalDateTime.now());
    }
    
    @Override
    public List<HistoriqueAffectation> getHistoriqueLate(int joursAvant) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endDate = now.plusDays(joursAvant);
        return repository.findSoonToRetardAffectations(now, endDate);
    }
    
    @Override
    public void marquerCommeNotifie(Long historiqueId) {
        try {
            HistoriqueAffectation affectation = repository.findById(historiqueId)
                .orElseThrow(() -> new NotFoundException());
            affectation.setLastNotification(LocalDateTime.now());
            affectation.setUpdatedAt(LocalDateTime.now());
            repository.save(affectation);
        } catch (NotFoundException e) {
            // Log l'erreur mais ne pas faire planter
        }
    }
    
    @Override
    public long retardDays(HistoriqueAffectation affectation) {
        if (affectation.getDateFinAffectation() != null) {
            return 0; 
        }
        
        LocalDateTime dateLimite = affectation.getDateAffectation().plusDays(affectation.getDelaiJours());
        LocalDateTime now = LocalDateTime.now();
        
        if (now.isAfter(dateLimite)) {
            return ChronoUnit.DAYS.between(dateLimite, now);
        }
        return 0;
    }
    
    @Override
    public long restantsDay(HistoriqueAffectation affectation) {
        if (affectation.getDateFinAffectation() != null) {
            return 0; 
        }
        
        LocalDateTime dateLimite = affectation.getDateAffectation().plusDays(affectation.getDelaiJours());
        LocalDateTime now = LocalDateTime.now();
        
        if (now.isBefore(dateLimite)) {
            return ChronoUnit.DAYS.between(now, dateLimite);
        }
        return 0;
    }
    
    @Override
    public boolean isLate(HistoriqueAffectation affectation) {
        return retardDays(affectation) > 0;
    }
    
    private HistoriqueAffectationResponse convertToResponse(HistoriqueAffectation affectation) {
        return HistoriqueAffectationResponse.builder()
            .id(affectation.getId())
            .claimId(affectation.getReclamationId())
            .codePlainte(affectation.getCodePlainte())
            .typePlainte(affectation.getTypePlainte())
            .nomAgent(affectation.getNomAgent())
            .emailAgent(affectation.getEmailAgent())
            .telephoneAgent(affectation.getTelephoneAgent())
            .dateAffectation(affectation.getDateAffectation())
            .delaiJours(affectation.getDelaiJours())
            .dateFinAffectation(affectation.getDateFinAffectation())
            .mailEnvoye(affectation.getMailEnvoye())
            .smsEnvoye(affectation.getSmsEnvoye())
            .lastNotification(affectation.getLastNotification())
            .emailAffecteur(affectation.getEmailAffecteur())
            .nomAffecteur(affectation.getNomAffecteur())
            .joursRetard(retardDays(affectation))
            .joursRestants(restantsDay(affectation))
            .estEnRetard(isLate(affectation))
            .estActif(affectation.getDateFinAffectation() == null)
            .dateLimite(affectation.getDateAffectation().plusDays(affectation.getDelaiJours()))
            .createdAt(affectation.getCreatedAt())
            .build();
    }
}