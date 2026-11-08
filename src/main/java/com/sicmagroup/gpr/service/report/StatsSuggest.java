package com.sicmagroup.gpr.service.report;

import java.util.HashMap;
import java.util.List;

import org.springframework.stereotype.Service;

import com.sicmagroup.gpr.api.report.FilterRequest;
import com.sicmagroup.gpr.domain.enumeration.ClaimStatus;
import com.sicmagroup.gpr.repository.ClaimRepository;
import com.sicmagroup.gpr.repository.ObjetRepository;
import com.sicmagroup.gpr.repository.ServicePointRepository;
import com.sicmagroup.gpr.repository.SuggestionRepository;
import com.sicmagroup.gpr.repository.projection.ObjectTotalPerStatusProjection;
import com.sicmagroup.gpr.repository.projection.custom.SuggestTotalPerStatusPro;

import io.micrometer.common.lang.Nullable;

import com.sicmagroup.gpr.repository.CollectionChannelRespository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StatsSuggest {
    private final ClaimRepository claimRepository;
    private final SuggestionRepository suggestionRepository;
    private final CollectionChannelRespository clRepository;
    private final ServicePointRepository spRepository;
    private final ObjetRepository oRepository;

    // Une suggestion n'a pas d'objet : sous un filtre par objet, ses lignes n'ont pas de valeur
    // (null, affiché « non applicable » par l'interface) plutôt qu'un nombre trompeur.
    private static boolean filtreParObjet(@Nullable FilterRequest request) {
        return request != null && request.getObjets() != null && !request.getObjets().isEmpty();
    }

    public HashMap<String, Double> totalSaved(@Nullable FilterRequest request) {
        if (filtreParObjet(request)) {
            HashMap<String, Double> resultat = new HashMap<>();
            resultat.put("Nombre de suggestions Enregistrées", null);
            return resultat;
        }
        Long total = 0L;
        if (request != null) {
            total = suggestionRepository.countSuggestByCriterias(request);
        } else {
            total = suggestionRepository.countByStatusNotAndIsDeletedFalse(ClaimStatus.TEMP_SAVED);
        }
        HashMap<String, Double> resultat = new HashMap<>();
        resultat.put("Nombre de suggestions Enregistrées", total.doubleValue());
        return resultat;
    }

    public HashMap<String, Double> totalSuggest(@Nullable FilterRequest request) {
        HashMap<String, Double> resultat = new HashMap<>();
        if (filtreParObjet(request)) {
            resultat.put("Nombre de suggestion Prises en compte", null);
            resultat.put("Nombre de suggestion Non Prises en compte", null);
            return resultat;
        }
        if (request != null) {
            if (request.getObjets() == null || request.getObjets().isEmpty()) {
                List<SuggestTotalPerStatusPro> listClaimProjection = suggestionRepository
                        .countSuggestByCriteriaAndStatus(request);
                for (SuggestTotalPerStatusPro projection : listClaimProjection) {
                    if (projection.getStatus() == ClaimStatus.TREAT) {
                        if (projection.isAccepted()) {
                            resultat.put("Nombre de suggestion Prises en compte",
                                    Double.valueOf(projection.getTotal()));
                        } else {
                            resultat.put("Nombre de suggestion Non Prises en compte",
                                    Double.valueOf(projection.getTotal()));
                        }
                    }
                }
            }
            if (!resultat.containsKey("Nombre de suggestion Prises en compte")) {
                resultat.put("Nombre de suggestion Prises en compte", 0D);
            }
            if (!resultat.containsKey("Nombre de suggestion Non Prises en compte")) {
                resultat.put("Nombre de suggestion Non Prises en compte", 0D);
            }
        } else {
            List<ObjectTotalPerStatusProjection> listClaimProjection = suggestionRepository.countSuggestPerStatus();
            for (ObjectTotalPerStatusProjection projection : listClaimProjection) {
                if ("TREAT".equals(projection.getStatus())) {
                    if (Boolean.TRUE.equals(projection.getAccepted())) {
                        resultat.put("Nombre de suggestion Prises en compte", Double.valueOf(projection.getTotal()));
                    } else {
                        resultat.put("Nombre de suggestion Non Prises en compte", Double.valueOf(projection.getTotal()));
                    }
                }
            }

            if (!resultat.containsKey("Nombre de suggestion Prises en compte")) {
                resultat.put("Nombre de suggestion Prises en compte", 0D);
            }
            if (!resultat.containsKey("Nombre de suggestion Non Prises en compte")) {
                resultat.put("Nombre de suggestion Non Prises en compte", 0D);
            }
        }

        return resultat;
    }
}
