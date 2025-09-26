package com.sicmagroup.gpr.service.historiqueAffectation;

import java.time.LocalDateTime;
import java.util.List;

import com.sicmagroup.gpr.api.claim.AffectTreatmentRequest;
import com.sicmagroup.gpr.api.claim.HistoriqueAffectationResponse;
import com.sicmagroup.gpr.domain.model.HistoriqueAffectation;

public interface HistoriqueAffectationService {
    
   
    HistoriqueAffectation storeHistorique(AffectTreatmentRequest affectTreatmentRequest) throws Exception;
    List<HistoriqueAffectationResponse> getHistoriqueByClaimId(Long claimId);
    HistoriqueAffectation getCurrentAffectation(Long claimId);
    List<HistoriqueAffectation> getHistoriqueLate();
    List<HistoriqueAffectation> getHistoriqueLate(int joursAvant);
    
    void marquerCommeNotifie(Long historiqueId);
    long retardDays(HistoriqueAffectation affectation);
    long restantsDay(HistoriqueAffectation affectation);
    boolean isLate(HistoriqueAffectation affectation);
}