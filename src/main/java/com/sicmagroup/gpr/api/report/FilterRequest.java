package com.sicmagroup.gpr.api.report;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.sicmagroup.gpr.domain.enumeration.ClaimStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FilterRequest {
    private List<Long> objets;
    private Long year;
    private List<Long> products;
    private List<Long> savedBy;
    private List<Long> servicePoints;
    private String receiveStart;
    private String receiveEnd;
    private List<ClaimStatus> etats;
    private List<Long> canals;

    // Copie indépendante du filtre (listes comprises) : un calcul qui ajuste sa copie
    // (ex. setEtats) ne change plus le filtre utilisé par les calculs suivants.
    public static FilterRequest copieDe(FilterRequest r) {
        if (r == null) {
            return null;
        }
        return FilterRequest.builder()
                .objets(copie(r.getObjets()))
                .year(r.getYear())
                .products(copie(r.getProducts()))
                .savedBy(copie(r.getSavedBy()))
                .servicePoints(copie(r.getServicePoints()))
                .receiveStart(r.getReceiveStart())
                .receiveEnd(r.getReceiveEnd())
                .etats(copie(r.getEtats()))
                .canals(copie(r.getCanals()))
                .build();
    }

    private static <T> List<T> copie(List<T> liste) {
        return liste == null ? null : new ArrayList<>(liste);
    }

}
