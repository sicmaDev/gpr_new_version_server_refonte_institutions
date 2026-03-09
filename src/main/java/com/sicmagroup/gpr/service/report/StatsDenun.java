package com.sicmagroup.gpr.service.report;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.springframework.stereotype.Service;

import com.sicmagroup.gpr.api.report.FilterRequest;
import com.sicmagroup.gpr.domain.enumeration.ClaimStatus;
import com.sicmagroup.gpr.domain.enumeration.ClaimType;
import com.sicmagroup.gpr.domain.model.Claim;
import com.sicmagroup.gpr.repository.ClaimRepository;
import com.sicmagroup.gpr.repository.ObjetRepository;
import com.sicmagroup.gpr.repository.ServicePointRepository;
import com.sicmagroup.gpr.repository.SuggestionRepository;
import com.sicmagroup.gpr.repository.projection.ClaimPerObjLevelProjection;
import com.sicmagroup.gpr.repository.projection.custom.ClaimPerObjLevelPro;

import io.micrometer.common.lang.Nullable;

import com.sicmagroup.gpr.repository.CollectionChannelRespository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StatsDenun {

    private final ClaimRepository claimRepository;
    private final SuggestionRepository suggestionRepository;
    private final CollectionChannelRespository clRepository;
    private final ServicePointRepository spRepository;
    private final ObjetRepository oRepository;

    public HashMap<String, Double> totalSavedClaim(@Nullable FilterRequest request) {
        Long total = 0L;
        if (request != null) {
            total = claimRepository.countClaimByCriterias(request, ClaimType.DENUNCIACION);
        } else {
            total = claimRepository.countByTypeAndIsDeletedFalseAndStatusNot(ClaimType.DENUNCIACION, ClaimStatus.TEMP_SAVED);
        }

        HashMap<String, Double> resultat = new HashMap<>();
        resultat.put("Nombre de dénonciations Enregistrées", total.doubleValue());
        return resultat;
    }

    public HashMap<String, Double> totalByGravity(@Nullable FilterRequest request) {
        HashMap<String, Double> resultat = new HashMap<>();
        String key = "";
        if (request != null) {
            List<ClaimPerObjLevelPro> allResult = claimRepository.countClaimByCriteriaAndByObjLevel(request,
                    ClaimType.DENUNCIACION);
            for (ClaimPerObjLevelPro claimPerObjLevelPro : allResult) {
                switch (claimPerObjLevelPro.getObjNiveau().name()) {
                    case "GRAVE":
                        key = "Nombre de dénonciations à niveau de gravité élevé enregistrées";
                        break;
                    case "MOYEN":
                        key = "Nombre de dénonciations à niveau de gravité moyen enregistrées";
                        break;
                    case "MINEUR":
                        key = "Nombre de dénonciations à niveau de gravité mineur enregistrées";
                        break;
                }
                resultat.put(key, claimPerObjLevelPro.getTotal().doubleValue());
            }

        } else {
            List<ClaimPerObjLevelProjection> allResult = claimRepository.countClaimPerObjLevel(ClaimType.DENUNCIACION);
            // for (ClaimPerObjLevelProjection projection : allResult) {
            //     switch (projection.getObjNiveau()) {
            //         case "GRAVE":
            //             key = "Nombre de dénonciations à niveau de gravité élevé enregistrées";
            //             break;
            //         case "MOYEN":
            //             key = "Nombre de dénonciations à niveau de gravité moyen enregistrées";
            //             break;
            //         case "MINEUR":
            //             key = "Nombre de dénonciations à niveau de gravité mineur enregistrées";
            //             break;

            //     }
            //     resultat.put(key, projection.getTotal().doubleValue());
            // }

           
            System.out.println("Nombre total de réclamations: " + allResult.size());

            // // Itérer sur les résultats
            for (ClaimPerObjLevelProjection projection : allResult) {
                System.out.println("Niveau: " + projection.getObjNiveau() + ", Total: " + projection.getTotal());

                // Définir la clé pour le niveau de gravité
              
                switch (projection.getObjNiveau()) {
                    case "GRAVE":
                        key = "Nombre de dénonciations à niveau de gravité élevé enregistrées";
                        break;
                    case "MOYEN":
                        key = "Nombre de dénonciations à niveau de gravité moyen enregistrées";
                        break;
                    case "MINEUR":
                        key = "Nombre de dénonciations à niveau de gravité mineur enregistrées";
                        break;

                    default:
                        key = "Niveau de gravité inconnu";
                        System.err.println("Niveau de gravité inconnu: " + projection.getObjNiveau());
                        continue; // Passer à l'élément suivant
                }

                // Accumuler les totaux pour chaque clé
                resultat.put(key, resultat.getOrDefault(key, 0.0) + projection.getTotal().doubleValue());
            }

           
        }

        if (!resultat.containsKey("Nombre de dénonciations à niveau de gravité élevé enregistrées")) {
            resultat.put("Nombre de dénonciations à niveau de gravité élevé enregistrées", 0D);
        }
        if (!resultat.containsKey("Nombre de dénonciations à niveau de gravité moyen enregistrées")) {
            resultat.put("Nombre de dénonciations à niveau de gravité moyen enregistrées", 0D);
        }
        if (!resultat.containsKey("Nombre de dénonciations à niveau de gravité mineur enregistrées")) {
            resultat.put("Nombre de dénonciations à niveau de gravité mineur enregistrées", 0D);
        }

        return resultat;
    }

    public HashMap<String, Double> totalUnTreat(@Nullable FilterRequest request) {
        HashMap<String, Double> resultat = new HashMap<>();
        int total = 0;
        List<ClaimStatus> status = Arrays.asList(ClaimStatus.TREAT, ClaimStatus.TEMP_SAVED);
        if (request != null) {
            if (request.getEtats() != null && request.getEtats().isEmpty()) {
                request.setEtats(status);
                total = claimRepository.countClaimByCriteriaAndStatusNot(request, ClaimType.DENUNCIACION).size();
            } else if (request.getEtats() != null && !request.getEtats().isEmpty()) {
                List<ClaimStatus> finalStatus = new ArrayList<>();
                for (ClaimStatus cStatus : request.getEtats()) {
                    if (status.contains(cStatus)) {
                        finalStatus.add(cStatus);
                    }
                }
                if (finalStatus.isEmpty()) {
                    resultat.put("Nombre de dénonciations non traitées", 0D);
                } else {
                    request.setEtats(finalStatus);
                    total = claimRepository.countClaimByCriteriaAndStatusNot(request, ClaimType.DENUNCIACION).size();
                }
            }

        } else {
            total = (claimRepository.findByTypeAndStatusNotIn(ClaimType.DENUNCIACION, status).size());
        }
        resultat.put("Nombre de dénonciations non traitées", Double.parseDouble("" + total));
        return resultat;
    }

    public HashMap<String, Double> totalUnTreatByGravity(@Nullable FilterRequest request) {
        HashMap<String, Double> resultat = new HashMap<>();
        List<ClaimStatus> status = Arrays.asList(ClaimStatus.TREAT, ClaimStatus.TEMP_SAVED);
        resultat.put("Nombre de dénonciations à niveau de gravité Grave non traitées", 0D);
        resultat.put("Nombre de dénonciations à niveau de gravité Moyen non traitées", 0D);
        resultat.put("Nombre de dénonciations à niveau de gravité Faible non traitées", 0D);
        List<Claim> claimsByStatusNot = new ArrayList<>();
        if (request != null) {
            if (request.getEtats() != null && request.getEtats().isEmpty()) {
                request.setEtats(status);
                claimsByStatusNot = claimRepository.countClaimByCriteriaAndStatusNot(request,
                        ClaimType.DENUNCIACION);
            } else if (request.getEtats() != null && !request.getEtats().isEmpty()) {
                List<ClaimStatus> finalStatus = new ArrayList<>();
                for (ClaimStatus cStatus : request.getEtats()) {
                    if (status.contains(cStatus)) {
                        finalStatus.add(cStatus);
                    }
                }
                if (!finalStatus.isEmpty()) {
                    request.setEtats(finalStatus);
                    claimsByStatusNot = claimRepository.countClaimByCriteriaAndStatusNot(request,
                            ClaimType.DENUNCIACION);
                }
            }

        } else {
            claimsByStatusNot = claimRepository.findByTypeAndStatusNotIn(ClaimType.DENUNCIACION, status);

        }
        Double oldVal = 0D;
        for (Claim claim : claimsByStatusNot) {
            switch (claim.getObjet().getRisqueLevel()) {
                case GRAVE:
                    oldVal = resultat.get("Nombre de dénonciations à niveau de gravité Grave non traitées");
                    resultat.replace("Nombre de dénonciations à niveau de gravité Grave non traitées", oldVal + 1);
                    break;
                case MOYEN:
                    oldVal = resultat.get("Nombre de dénonciations à niveau de gravité Moyen non traitées");
                    resultat.replace("Nombre de dénonciations à niveau de gravité Moyen non traitées", oldVal + 1);
                    break;
                case MINEUR:
                    oldVal = resultat.get("Nombre de dénonciations à niveau de gravité Moyen non traitées");
                    resultat.replace("Nombre de dénonciations à niveau de gravité Faible non traitées", oldVal + 1);
                    break;
            }
            oldVal = 0D;
        }
        return resultat;
    }

    public HashMap<String, Double> totalTreat(@Nullable FilterRequest request) {
        HashMap<String, Double> resultat = new HashMap<>();
        List<ClaimStatus> status = Arrays.asList(ClaimStatus.TREAT);
        long total = 0;
        if (request != null) {
            if (request.getEtats() != null && request.getEtats().isEmpty()) {
                request.setEtats(status);
                total = claimRepository.countClaimByCriterias(request, ClaimType.DENUNCIACION);
            } else if (request.getEtats() != null && !request.getEtats().isEmpty()) {
                List<ClaimStatus> finalStatus = new ArrayList<>();
                for (ClaimStatus cStatus : request.getEtats()) {
                    if (status.contains(cStatus)) {
                        finalStatus.add(cStatus);
                    }
                }
                if (finalStatus.isEmpty()) {
                    resultat.put("Nombre de dénonciations traitées", 0D);
                } else {
                    request.setEtats(finalStatus);
                    total = claimRepository.countClaimByCriterias(request, ClaimType.DENUNCIACION);
                }
            }
        } else {
           total = claimRepository.findByTypeAndIsDeletedFalseAndStatusIn(ClaimType.DENUNCIACION, status).size();
        }
        resultat.put("Nombre de dénonciations traitées", Double.parseDouble("" + total));
        return resultat;
    }

    public HashMap<String, Double> totalTreatByGravity(@Nullable FilterRequest request) {
        HashMap<String, Double> resultat = new HashMap<>();
        List<ClaimStatus> status = Arrays.asList(ClaimStatus.TREAT);
        resultat.put("Nombre de dénonciations à niveau de gravité Grave traitées", 0D);
        resultat.put("Nombre de dénonciations à niveau de gravité Moyen traitées", 0D);
        resultat.put("Nombre de dénonciations à niveau de gravité Mineur traitées", 0D);
        List<Claim> claimsByStatus = new ArrayList<>();
        if (request != null) {
            if (request.getEtats() != null && request.getEtats().isEmpty()) {
                request.setEtats(status);
                claimsByStatus = claimRepository.countClaimByCriteriaAndStatusIn(request,
                        ClaimType.DENUNCIACION);
            } else if (request.getEtats() != null && !request.getEtats().isEmpty()) {
                List<ClaimStatus> finalStatus = new ArrayList<>();
                for (ClaimStatus cStatus : request.getEtats()) {
                    if (status.contains(cStatus)) {
                        finalStatus.add(cStatus);
                    }
                }
                if (!finalStatus.isEmpty()) {
                    request.setEtats(finalStatus);
                    claimsByStatus = claimRepository.countClaimByCriteriaAndStatusIn(request,
                            ClaimType.DENUNCIACION);
                }
            }

        } else {
            claimsByStatus = claimRepository.findByTypeAndIsDeletedFalseAndStatusIn(ClaimType.DENUNCIACION, status);
        }
        Double oldVal = 0D;
        for (Claim claim : claimsByStatus) {
            switch (claim.getObjet().getRisqueLevel()) {
                case GRAVE:
                    oldVal = resultat.get("Nombre de dénonciations à niveau de gravité Grave traitées");
                    resultat.replace("Nombre de dénonciations à niveau de gravité Grave traitées", oldVal + 1);
                    break;
                case MOYEN:
                    oldVal = resultat.get("Nombre de dénonciations à niveau de gravité Moyen traitées");
                    resultat.replace("Nombre de dénonciations à niveau de gravité Moyen traitées", oldVal + 1);
                    break;
                case MINEUR:
                    oldVal = resultat.get("Nombre de dénonciations à niveau de gravité Mineur traitées");
                    resultat.replace("Nombre de dénonciations à niveau de gravité Mineur traitées", oldVal + 1);
                    break;
            }
            oldVal = 0D;
        }
        return resultat;
    }

    public HashMap<String, Double> totalTreatByRespectTiming(@Nullable FilterRequest request) {
        HashMap<String, Double> resultat = new HashMap<>();
        resultat.put("Nombre de dénonciations traitées dans le délai", 0D);
        resultat.put("Nombre de dénonciations à niveau de gravité Mineur traitées dans le délai", 0D);
        resultat.put("Nombre de dénonciations à niveau de gravité Moyen traitées dans le délai", 0D);
        resultat.put("Nombre de dénonciations à niveau de gravité Grave traitées dans le délai", 0D);
        List<ClaimStatus> status = Arrays.asList(ClaimStatus.TREAT);
        List<Claim> claimsTreat = new ArrayList<>();
        if (request != null) {
            if (request.getEtats() != null && request.getEtats().isEmpty()) {
                request.setEtats(status);
                claimsTreat = claimRepository.countClaimByCriteriaAndStatusIn(request, ClaimType.DENUNCIACION);
            } else if (request.getEtats() != null && !request.getEtats().isEmpty()) {
                List<ClaimStatus> finalStatus = new ArrayList<>();
                for (ClaimStatus cStatus : request.getEtats()) {
                    if (status.contains(cStatus)) {
                        finalStatus.add(cStatus);
                    }
                }
                if (!finalStatus.isEmpty()) {
                    request.setEtats(finalStatus);
                    claimsTreat = claimRepository.countClaimByCriteriaAndStatusIn(request, ClaimType.DENUNCIACION);
                }
            }
        } else {
            claimsTreat = claimRepository.findByTypeAndIsDeletedFalseAndStatusIn(ClaimType.DENUNCIACION, status);

        }
        LocalDateTime receiptDate;
        LocalDateTime measureDate;
        LocalDateTime supposedFinalTreatmentDate;
        Double oldVal = 0D;
        Double oldVal2 = 0D;
        long delaiObj;
        for (Claim claim : claimsTreat) {
            receiptDate = claim.getReceiptDateTime();
            measureDate = claim.getSolutions().get((claim.getSolutions().size() - 1)).getCreatedAt();

            delaiObj = claim.getObjet().getProcessingTime();
            supposedFinalTreatmentDate = receiptDate.plusDays(delaiObj);
            if (!measureDate.isAfter(supposedFinalTreatmentDate)) { // il n'y a retard de traitement
                oldVal2++;
                switch (claim.getObjet().getRisqueLevel()) {
                    case GRAVE:
                        oldVal = resultat
                                .get("Nombre de dénonciations à niveau de gravité Grave traitées dans le délai");
                        resultat.replace("Nombre de dénonciations à niveau de gravité Grave traitées dans le délai",
                                oldVal + 1);
                        break;
                    case MOYEN:
                        oldVal = resultat
                                .get("Nombre de dénonciations à niveau de gravité Moyen traitées dans le délai");
                        resultat.replace("Nombre de dénonciations à niveau de gravité Moyen traitées dans le délai",
                                oldVal + 1);
                        break;
                    case MINEUR:
                        oldVal = resultat
                                .get("Nombre de dénonciations à niveau de gravité Mineur traitées dans le délai");
                        resultat.replace("Nombre de dénonciations à niveau de gravité Mineur traitées dans le délai",
                                oldVal + 1);
                        break;
                }
                oldVal = 0D;
            }
        }
        resultat.replace("Nombre de dénonciations traitées dans le délai",
                oldVal2);
        return resultat;
    }

}
