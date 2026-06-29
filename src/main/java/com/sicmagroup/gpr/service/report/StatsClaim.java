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
import com.sicmagroup.gpr.domain.model.Objet;
import com.sicmagroup.gpr.domain.model.Solution;
import com.sicmagroup.gpr.repository.ClaimRepository;
import com.sicmagroup.gpr.repository.projection.ClaimPerObjLevelProjection;
import com.sicmagroup.gpr.repository.projection.custom.ClaimPerObjLevelPro;
import com.sicmagroup.gpr.utils.Utils;

import io.micrometer.common.lang.Nullable;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StatsClaim {

    private final ClaimRepository claimRepository;

    public HashMap<String, Double> totalSavedClaim(@Nullable FilterRequest request) {
        Long total = 0L;
        if (request != null) {
            total = claimRepository.countClaimByCriterias(request, ClaimType.CLAIM);
        } else {

            total = claimRepository.countByTypeAndIsDeletedFalseAndStatusNot(ClaimType.CLAIM, ClaimStatus.TEMP_SAVED);
        }

        HashMap<String, Double> resultat = new HashMap<>();
        resultat.put("Nombre de réclamations Enregistrées", total.doubleValue());
        return resultat;
    }

    public HashMap<String, Double> totalByGravity(@Nullable FilterRequest request) {
        HashMap<String, Double> resultat = new HashMap<>();
        String key = "";
        if (request != null) {
            List<ClaimPerObjLevelPro> allResult = claimRepository.countClaimByCriteriaAndByObjLevel(request,
                    ClaimType.CLAIM);
            for (ClaimPerObjLevelPro claimPerObjLevelPro : allResult) {
                switch (claimPerObjLevelPro.getObjNiveau().name()) {
                    case "MINEUR":
                        key = "Nombre de réclamations à niveau de gravité Mineur enregistrées";
                        break;
                    case "MOYEN":
                        key = "Nombre de réclamations à niveau de gravité Moyen enregistrées";
                        break;
                    case "GRAVE":
                        key = "Nombre de réclamations à niveau de gravité Grave enregistrées";
                        break;
                    
                    
                }
                resultat.put(key, claimPerObjLevelPro.getTotal().doubleValue());
            }

        } else {
            // List<ClaimPerObjLevelProjection> allResult = claimRepository.countClaimPerObjLevel(ClaimType.CLAIM);
            // // System.out.println("Niveau: " allResult.length());
            // for (ClaimPerObjLevelProjection projection : allResult) {
            //     System.out.println("Niveau: " + projection.getObjNiveau() + ", Total: " + projection.getTotal());
            //     switch (projection.getObjNiveau()) {
            //         case "GRAVE":
            //             key = "Nombre de réclamations à niveau de gravité Grave enregistrées";
            //             break;
            //         case "MOYEN":
            //             key = "Nombre de réclamations à niveau de gravité Moyen enregistrées";
            //             break;
            //         case "MINEUR":
            //             key = "Nombre de réclamations à niveau de gravité Mineur enregistrées";
            //             break;

            //     }
            //     resultat.put(key, projection.getTotal().doubleValue());
            // }

            // Initialiser le Map pour accumuler les totaux


            // Récupérer les données depuis claimRepository
            List<ClaimPerObjLevelProjection> allResult = claimRepository.countClaimPerObjLevel(ClaimType.CLAIM);

            System.out.println("Nombre total de réclamations: " + allResult.size());

            // Itérer sur les résultats
            for (ClaimPerObjLevelProjection projection : allResult) {
                System.out.println("Niveau: " + projection.getObjNiveau() + ", Total: " + projection.getTotal());

                // Définir la clé pour le niveau de gravité
              
                switch (projection.getObjNiveau()) {
                    case "MINEUR":
                        key = "Nombre de réclamations à niveau de gravité Mineur enregistrées";
                        break;
                    case "MOYEN":
                        key = "Nombre de réclamations à niveau de gravité Moyen enregistrées";
                        break;
                    case "GRAVE":
                        key = "Nombre de réclamations à niveau de gravité Grave enregistrées";
                        break;
                    
                   
                    default:
                        key = "Niveau de gravité inconnu";
                        System.err.println("Niveau de gravité inconnu: " + projection.getObjNiveau());
                        continue; // Passer à l'élément suivant
                }

                // Accumuler les totaux pour chaque clé
                resultat.put(key, resultat.getOrDefault(key, 0.0) + projection.getTotal().doubleValue());
            }

            // Afficher le contenu du résultat pour vérification
            // for (Map.Entry<String, Double> entry : resultat.entrySet()) {
            //     System.out.println(entry.getKey() + ": " + entry.getValue());
            // }

        }

        if (!resultat.containsKey("Nombre de réclamations à niveau de gravité Mineur enregistrées")) {
            resultat.put("Nombre de réclamations à niveau de gravité Mineur enregistrées", 0D);
        }
        if (!resultat.containsKey("Nombre de réclamations à niveau de gravité Moyen enregistrées")) {
            resultat.put("Nombre de réclamations à niveau de gravité Moyen enregistrées", 0D);
        }
        if (!resultat.containsKey("Nombre de réclamations à niveau de gravité Grave enregistrées")) {
            resultat.put("Nombre de réclamations à niveau de gravité Grave enregistrées", 0D);
        }
        
        return resultat;
    }

    public HashMap<String, Double> totalUnTreat(@Nullable FilterRequest request) {
        HashMap<String, Double> resultat = new HashMap<>();
        int total = 0;
        List<ClaimStatus> status = Arrays.asList(ClaimStatus.TREAT, ClaimStatus.SATISFIED, ClaimStatus.UNSATISFIED,
                ClaimStatus.CLASSED, ClaimStatus.LITIGATION, ClaimStatus.TEMP_SAVED, ClaimStatus.PARTIAL_SATISFIED);
        if (request != null) {
            if ((request.getEtats() != null && request.getEtats().isEmpty()) || request.getEtats() == null) {
                request.setEtats(status);
                total = claimRepository.countClaimByCriteriaAndStatusNot(request, ClaimType.CLAIM).size();
                System.out.println("total1 " + total);
            } else if (request.getEtats() != null && !request.getEtats().isEmpty()) {
                List<ClaimStatus> finalStatus = new ArrayList<>();
                for (ClaimStatus cStatus : request.getEtats()) {
                    if (status.contains(cStatus)) {
                        finalStatus.add(cStatus);
                    }
                }
                if (finalStatus.isEmpty()) {
                    resultat.put("Nombre de réclamations non traitées", 0D);
                } else {
                    request.setEtats(finalStatus);
                    if (!finalStatus.contains(ClaimStatus.TEMP_SAVED)) {
                        finalStatus.add(ClaimStatus.TEMP_SAVED);
                    }
                    total = claimRepository.countClaimByCriteriaAndStatusNot(request, ClaimType.CLAIM).size();
                    System.out.println("total2 " + total);
                }

            }

        } else {
            total = (claimRepository.findByTypeAndStatusNotIn(ClaimType.CLAIM, status).size());

            System.out.println("total3 " + total);
        }
        resultat.put("Nombre de réclamations non traitées", Double.parseDouble("" + total));
        return resultat;
    }

    public HashMap<String, Double> totalUnTreatByGravity(@Nullable FilterRequest request) {
        HashMap<String, Double> resultat = new HashMap<>();
        int total = 0;
        List<ClaimStatus> status = Arrays.asList(ClaimStatus.TREAT, ClaimStatus.SATISFIED, ClaimStatus.UNSATISFIED,
                ClaimStatus.CLASSED, ClaimStatus.LITIGATION, ClaimStatus.TEMP_SAVED, ClaimStatus.PARTIAL_SATISFIED);
        resultat.put("Nombre de réclamations à niveau de gravité Grave non traitées", 0D);
        resultat.put("Nombre de réclamations à niveau de gravité Moyen non traitées", 0D);
        resultat.put("Nombre de réclamations à niveau de gravité Mineur non traitées", 0D);
        List<Claim> claimsByStatusNot = new ArrayList<>();
        if (request != null) {
            if ((request.getEtats() != null && request.getEtats().isEmpty()) || request.getEtats() == null) {
                request.setEtats(status);
                claimsByStatusNot = claimRepository.countClaimByCriteriaAndStatusNot(request,
                        ClaimType.CLAIM);
            } else if (request.getEtats() != null && !request.getEtats().isEmpty()) {
                List<ClaimStatus> finalStatus = new ArrayList<>();
                for (ClaimStatus cStatus : request.getEtats()) {
                    if (status.contains(cStatus)) {
                        finalStatus.add(cStatus);
                    }
                }
                if (!finalStatus.isEmpty()) {
                    if (!finalStatus.contains(ClaimStatus.TEMP_SAVED)) {
                        finalStatus.add(ClaimStatus.TEMP_SAVED);
                    }
                    request.setEtats(finalStatus);

                    claimsByStatusNot = claimRepository.countClaimByCriteriaAndStatusNot(request,
                            ClaimType.CLAIM);
                }
            }

        } else {
            claimsByStatusNot = claimRepository.findByTypeAndStatusNotIn(ClaimType.CLAIM, status);

        }
        Double oldVal = 0D;
        for (Claim claim : claimsByStatusNot) {
            Objet obj = claim.getObjet();
            if (obj != null) {
                switch (obj.getRisqueLevel()) {
                    case GRAVE:
                        oldVal = resultat.get("Nombre de réclamations à niveau de gravité Grave non traitées");
                        resultat.replace("Nombre de réclamations à niveau de gravité Grave non traitées", oldVal + 1);
                        break;
                    case MOYEN:
                        oldVal = resultat.get("Nombre de réclamations à niveau de gravité Moyen non traitées");
                        resultat.replace("Nombre de réclamations à niveau de gravité Moyen non traitées", oldVal + 1);
                        break;
                    case MINEUR:
                        oldVal = resultat.get("Nombre de réclamations à niveau de gravité Mineur non traitées");
                        resultat.replace("Nombre de réclamations à niveau de gravité Mineur non traitées", oldVal + 1);
                        break;
                }
                oldVal = 0D;
            }

        }
        return resultat;
    }

    public HashMap<String, Double> totalTreat(@Nullable FilterRequest request) {
        HashMap<String, Double> resultat = new HashMap<>();
        List<ClaimStatus> status = Arrays.asList(ClaimStatus.TREAT, ClaimStatus.SATISFIED, ClaimStatus.UNSATISFIED,
                ClaimStatus.CLASSED, ClaimStatus.LITIGATION, ClaimStatus.PARTIAL_SATISFIED);
        long total = 0;
        if (request != null) {
            if (request.getEtats() != null && request.getEtats().isEmpty()) {
                request.setEtats(status);
                total = claimRepository.countClaimByCriterias(request, ClaimType.CLAIM);
            } else if (request.getEtats() != null && !request.getEtats().isEmpty()) {
                List<ClaimStatus> finalStatus = new ArrayList<>();
                for (ClaimStatus cStatus : request.getEtats()) {
                    if (status.contains(cStatus)) {
                        finalStatus.add(cStatus);
                    }
                }
                if (finalStatus.isEmpty()) {
                    resultat.put("Nombre de réclamations traitées", 0D);
                } else {
                    request.setEtats(finalStatus);
                    total = claimRepository.countClaimByCriterias(request, ClaimType.CLAIM);
                }
            }
        } else {
            total = claimRepository.findByTypeAndIsDeletedFalseAndStatusIn(ClaimType.CLAIM, status).size();
        }
        resultat.put("Nombre de réclamations traitées", Double.parseDouble("" + total));
        return resultat;
    }

    public HashMap<String, Double> totalTreatByGravity(@Nullable FilterRequest request) {
        HashMap<String, Double> resultat = new HashMap<>();
        int total = 0;
        List<ClaimStatus> status = Arrays.asList(ClaimStatus.TREAT, ClaimStatus.SATISFIED, ClaimStatus.UNSATISFIED,
                ClaimStatus.CLASSED, ClaimStatus.LITIGATION, ClaimStatus.PARTIAL_SATISFIED);
        resultat.put("Nombre de réclamations à niveau de gravité Mineur traitées", 0D);
        resultat.put("Nombre de réclamations à niveau de gravité Moyen traitées", 0D);
        resultat.put("Nombre de réclamations à niveau de gravité Grave traitées", 0D);
       
       
        List<Claim> claimsByStatus = new ArrayList<>();
        if (request != null) {
            if (request.getEtats() != null && request.getEtats().isEmpty()) {
                request.setEtats(status);
                claimsByStatus = claimRepository.countClaimByCriteriaAndStatusIn(request,
                        ClaimType.CLAIM);
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
                            ClaimType.CLAIM);
                }
            }

        } else {
            claimsByStatus = claimRepository.findByTypeAndIsDeletedFalseAndStatusIn(ClaimType.CLAIM, status);
        }
        Double oldVal = 0D;
        for (Claim claim : claimsByStatus) {
            switch (claim.getObjet().getRisqueLevel()) {
                case GRAVE:
                    oldVal = resultat.get("Nombre de réclamations à niveau de gravité Grave traitées");
                    resultat.replace("Nombre de réclamations à niveau de gravité Grave traitées", oldVal + 1);
                    break;
                case MOYEN:
                    oldVal = resultat.get("Nombre de réclamations à niveau de gravité Moyen traitées");
                    resultat.replace("Nombre de réclamations à niveau de gravité Moyen traitées", oldVal + 1);
                    break;
                case MINEUR:
                    oldVal = resultat.get("Nombre de réclamations à niveau de gravité Mineur traitées");
                    resultat.replace("Nombre de réclamations à niveau de gravité Mineur traitées", oldVal + 1);
                    break;
            }
            oldVal = 0D;
        }
        return resultat;
    }

    public HashMap<String, Double> totalTreatByRespectTiming(@Nullable FilterRequest request) {
        HashMap<String, Double> resultat = new HashMap<>();
        resultat.put("Nombre de réclamations traitées dans le délai", 0D);
        resultat.put("Nombre de réclamations à niveau de gravité Mineur traitées dans le délai", 0D);
        resultat.put("Nombre de réclamations à niveau de gravité Moyen traitées dans le délai", 0D);
        resultat.put("Nombre de réclamations à niveau de gravité Grave traitées dans le délai", 0D);
        List<ClaimStatus> status = Arrays.asList(ClaimStatus.TREAT, ClaimStatus.SATISFIED, ClaimStatus.UNSATISFIED,
                ClaimStatus.CLASSED, ClaimStatus.LITIGATION, ClaimStatus.PARTIAL_SATISFIED);
        List<Claim> claimsTreat = new ArrayList<>();
        if (request != null) {
            if (request.getEtats() != null && request.getEtats().isEmpty()) {
                request.setEtats(status);
                claimsTreat = claimRepository.countClaimByCriteriaAndStatusIn(request, ClaimType.CLAIM);
            } else if (request.getEtats() != null && !request.getEtats().isEmpty()) {
                List<ClaimStatus> finalStatus = new ArrayList<>();
                for (ClaimStatus cStatus : request.getEtats()) {
                    if (status.contains(cStatus)) {
                        finalStatus.add(cStatus);
                    }
                }
                if (!finalStatus.isEmpty()) {
                    request.setEtats(finalStatus);
                    claimsTreat = claimRepository.countClaimByCriteriaAndStatusIn(request, ClaimType.CLAIM);
                }
            }
        } else {
            claimsTreat = claimRepository.findByTypeAndIsDeletedFalseAndStatusIn(ClaimType.CLAIM, status);

        }
        LocalDateTime receiptDate;
        LocalDateTime measureDate;
        LocalDateTime supposedFinalTreatmentDate;
        Double oldVal = 0D;
        Double oldVal2 = 0D;
        long delaiObj;
        Solution solution;
        for (Claim claim : claimsTreat) {
            receiptDate = claim.getReceiptDateTime();
            if (claim.getSolutions().size() > 0) {
                solution = claim.getSolutions().get((claim.getSolutions().size() - 1));
                if (solution.getSatisfactionMeasure() != null) {
                    measureDate = solution.getSatisfactionMeasure()
                            .getMeasureDateTime();

                    delaiObj = claim.getObjet().getProcessingTime();
                    supposedFinalTreatmentDate = receiptDate.plusDays(delaiObj);
                    if (!measureDate.isAfter(supposedFinalTreatmentDate)) { // il n'y a pas retard de traitement
                        oldVal2++;
                        switch (claim.getObjet().getRisqueLevel()) {
                            case GRAVE:
                                oldVal = resultat
                                        .get("Nombre de réclamations à niveau de gravité Grave traitées dans le délai");
                                resultat.replace(
                                        "Nombre de réclamations à niveau de gravité Grave traitées dans le délai",
                                        oldVal + 1);
                                break;
                            case MOYEN:
                                oldVal = resultat
                                        .get("Nombre de réclamations à niveau de gravité Moyen traitées dans le délai");
                                resultat.replace(
                                        "Nombre de réclamations à niveau de gravité Moyen traitées dans le délai",
                                        oldVal + 1);
                                break;
                            case MINEUR:
                                oldVal = resultat
                                        .get("Nombre de réclamations à niveau de gravité Mineur traitées dans le délai");
                                resultat.replace(
                                        "Nombre de réclamations à niveau de gravité Mineur traitées dans le délai",
                                        oldVal + 1);
                                break;
                        }
                        oldVal = 0D;
                    }

                }

            }
        }
        resultat.replace("Nombre de réclamations traitées dans le délai",
                oldVal2);
        return resultat;
    }

    // HashMap<String, Double> totalSolutionAndByGravity(@Nullable FilterRequest
    // request) {
    // HashMap<String, Double> resultat = new HashMap<>();
    // resultat.put("Nombre de réponses aux réclamations", 0D);
    // resultat.put("Nombre de réponses aux réclamations à niveau de gravité
    // Mineur", 0D);
    // resultat.put("Nombre de réponses aux réclamations à niveau de gravité Moyen",
    // 0D);
    // resultat.put("Nombre de réponses aux réclamations à niveau de gravité Grave",
    // 0D);
    // List<ClaimStatus> status = Arrays.asList(ClaimStatus.TREAT,
    // ClaimStatus.SATISFIED, ClaimStatus.UNSATISFIED,
    // ClaimStatus.CLASSED, ClaimStatus.LITIGATION);
    // List<Claim> claimsTreat = new ArrayList<>();
    // if (request != null) {
    // if (request.getEtats() != null && request.getEtats().isEmpty()) {
    // request.setEtats(status);
    // claimsTreat = claimRepository.countClaimByCriteriaAndStatusIn(request,
    // ClaimType.CLAIM);
    // } else {
    // List<ClaimStatus> finalStatus = new ArrayList<>();
    // for (ClaimStatus cStatus : request.getEtats()) {
    // if (status.contains(cStatus)) {
    // finalStatus.add(cStatus);
    // }
    // }
    // if (!finalStatus.isEmpty()) {
    // request.setEtats(finalStatus);
    // claimsTreat = claimRepository.countClaimByCriteriaAndStatusIn(request,
    // ClaimType.CLAIM);
    // }
    // }
    // } else {
    // claimsTreat = claimRepository.findByTypeAndStatusIn(ClaimType.CLAIM, status);

    // }

    // Double oldVal = 0D;
    // Double oldVal2 = 0D;
    // for (Claim claim : claimsTreat) {
    // if (!claim.getSolutions().isEmpty()) {
    // for (Solution solution : claim.getSolutions()) {
    // if (solution.getSatisfactionMeasure() != null
    // || solution.getStatus().equals(SolutionStatus.APPROVED)) {
    // oldVal2++;
    // switch (claim.getObjet().getRisqueLevel()) {
    // case GRAVE:
    // oldVal = resultat
    // .get("Nombre de réponses aux réclamations à niveau de gravité Grave");
    // resultat.replace("Nombre de réponses aux réclamations à niveau de gravité
    // Grave",
    // oldVal + 1);
    // break;
    // case MOYEN:
    // oldVal = resultat
    // .get("Nombre de réponses aux réclamations à niveau de gravité Moyen");
    // resultat.replace("Nombre de réponses aux réclamations à niveau de gravité
    // Moyen",
    // oldVal + 1);
    // break;
    // case MINEUR:
    // oldVal = resultat
    // .get("Nombre de réponses aux réclamations à niveau de gravité Mineur");
    // resultat.replace("Nombre de réponses aux réclamations à niveau de gravité
    // Mineur",
    // oldVal + 1);
    // break;
    // }
    // oldVal = 0D;
    // }

    // }
    // }
    // }
    // resultat.replace("Nombre de réponses aux réclamations",
    // oldVal2);
    // return resultat;
    // }

    public HashMap<String, Double> totalReclamantSatisfait(@Nullable FilterRequest request) {
        HashMap<String, Double> resultat = new HashMap<>();
        resultat.put("Nombre de réclamations à réclamants satisfaits", 0D);

        List<ClaimStatus> status = Arrays.asList(ClaimStatus.SATISFIED);
        List<Claim> claimsTreat = new ArrayList<>();
        if (request != null) {
            if (request.getEtats() != null && request.getEtats().isEmpty()) {
                request.setEtats(status);
                claimsTreat = claimRepository.countClaimByCriteriaAndStatusIn(request, ClaimType.CLAIM);
            } else if (request.getEtats() != null && !request.getEtats().isEmpty()) {
                List<ClaimStatus> finalStatus = new ArrayList<>();
                for (ClaimStatus cStatus : request.getEtats()) {
                    if (status.contains(cStatus)) {
                        finalStatus.add(cStatus);
                    }
                }
                if (!finalStatus.isEmpty()) {
                    request.setEtats(finalStatus);
                    claimsTreat = claimRepository.countClaimByCriteriaAndStatusIn(request, ClaimType.CLAIM);
                }
            }
        } else {
            claimsTreat = claimRepository.findByTypeAndIsDeletedFalseAndStatusIn(ClaimType.CLAIM, status);
        }

        resultat.replace("Nombre de réclamations à réclamants satisfaits",
                Double.valueOf(claimsTreat.size()));
        return resultat;
    }

    public HashMap<String, Double> tauxSatisfaction(@Nullable FilterRequest request) {
        HashMap<String, Double> resultat = new HashMap<>();
        List<ClaimStatus> status = Arrays.asList(ClaimStatus.SATISFIED);
        List<Claim> claimsTreat = new ArrayList<>();
        resultat.put("Taux de satisfaction(%)", 0D);
        if (request != null) {
            if ((request.getEtats() != null && request.getEtats().isEmpty()) || request.getEtats() == null) {
                request.setEtats(status);
                claimsTreat = claimRepository.countClaimByCriteriaAndStatusIn(request, ClaimType.CLAIM);
            } else if (request.getEtats() != null && !request.getEtats().isEmpty()) {
                List<ClaimStatus> finalStatus = new ArrayList<>();
                for (ClaimStatus cStatus : request.getEtats()) {
                    if (status.contains(cStatus)) {
                        finalStatus.add(cStatus);
                    }
                }
                if (!finalStatus.isEmpty()) {
                    request.setEtats(finalStatus);
                    claimsTreat = claimRepository.countClaimByCriteriaAndStatusIn(request, ClaimType.CLAIM);
                }
            }
        } else {
            claimsTreat = claimRepository.findByTypeAndIsDeletedFalseAndStatusIn(ClaimType.CLAIM, status);
        }

        List<ClaimStatus> allSatisfaction = Arrays.asList(ClaimStatus.SATISFIED, ClaimStatus.UNSATISFIED,
                ClaimStatus.PARTIAL_SATISFIED,ClaimStatus.CLASSED,ClaimStatus.LITIGATION);
        List<Claim> allClaims = claimRepository.findByTypeAndIsDeletedFalseAndStatusIn(ClaimType.CLAIM, allSatisfaction);
        // System.out.println("Taux");
        resultat.replace("Taux de satisfaction(%)",
                Utils.parseDouble(
                        Utils.percentCalculator(Long.valueOf(claimsTreat.size()), Long.valueOf(allClaims.size()))));
        return resultat;
    }

    public HashMap<String, Double> pourcentageReclamationsTraitees(@Nullable FilterRequest request) {
        HashMap<String, Double> resultat = new HashMap<>();
        resultat.put("Pourcentage réclamations traitées (%)", 0D);  // Initialisation avec 0%
    
        // Liste des statuts correspondant aux réclamations traitées
        List<ClaimStatus> statusTraites = Arrays.asList(
                ClaimStatus.TREAT,
                ClaimStatus.SATISFIED,
                ClaimStatus.UNSATISFIED,
                ClaimStatus.PARTIAL_SATISFIED,
                ClaimStatus.CLASSED,
                ClaimStatus.LITIGATION
        );
    
        // Récupération du nombre total de réclamations
        List<Claim> allClaims = new ArrayList<>();
        if (request != null) {
            // Si un filtre est fourni, on récupère les réclamations en fonction de ce filtre
            // allClaims = claimRepository.countClaimByCriteria(request);
        } else {
            // Si aucun filtre n'est fourni, on récupère toutes les réclamations
            allClaims = claimRepository.findByTypeAndIsDeletedFalseAndStatusNot(ClaimType.CLAIM, ClaimStatus.TEMP_SAVED);
        }
    
        // Récupération du nombre total de réclamations traitées
        long nombreReclamationsTraitees = allClaims.stream()
                .filter(claim -> statusTraites.contains(claim.getStatus()))
                .count();
    
        // Calcul du pourcentage de réclamations traitées
        if (allClaims.size() > 0) {
            double pourcentage = (double) nombreReclamationsTraitees / allClaims.size() * 100;
            // Arrondi à deux chiffres après la virgule
            pourcentage = Math.round(pourcentage * 100.0) / 100.0;
            // Mise à jour du HashMap avec le pourcentage calculé
            resultat.replace("Pourcentage réclamations traitées (%)", pourcentage);
        }
    
        return resultat;
    }

    public HashMap<String, Double> pourcentageReelReclamationsTraitees(@Nullable FilterRequest request) {
        HashMap<String, Double> resultat = new HashMap<>();
        resultat.put("Pourcentage réel réclamations traitées (%)", 0D);  // Initialisation avec 0%
    
        // Liste des statuts correspondant aux réclamations traitées
        List<ClaimStatus> statusTraites = Arrays.asList(
                ClaimStatus.TREAT,
                ClaimStatus.SATISFIED,
                ClaimStatus.PARTIAL_SATISFIED,
                ClaimStatus.UNSATISFIED,
                ClaimStatus.CLASSED,
                ClaimStatus.LITIGATION
        );
    
        // Récupération des réclamations selon le filtre
        List<Claim> allClaims = (request != null)
                ? claimRepository.countClaimByCriteriaAndStatusNot(request, ClaimType.CLAIM)
                : claimRepository.findByTypeAndIsDeletedFalseAndStatusNot(ClaimType.CLAIM, ClaimStatus.TEMP_SAVED);
        List<Claim> allExpiredClaims = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // Filtrage des réclamations échues
        for (Claim claim : allClaims) {
            LocalDateTime supposedFinalTreatmentDate = claim.getReceiptDateTime().plusDays(claim.getObjet().getProcessingTime());
            if (now.isAfter(supposedFinalTreatmentDate)) {
                allExpiredClaims.add(claim);
            }
        }
    
        // Compter le nombre total de réclamations traitées parmi celles échues
        long nombreReclamationsTraitees = allExpiredClaims.stream()
                .filter(claim -> statusTraites.contains(claim.getStatus()))
                .count();
    
        // Calculer le pourcentage réel des réclamations traitées
        if (!allExpiredClaims.isEmpty()) {
            double pourcentage = (double) nombreReclamationsTraitees / allExpiredClaims.size() * 100;
            // Arrondi à deux chiffres après la virgule
            pourcentage = Math.round(pourcentage * 100.0) / 100.0;
    
            // Mise à jour du HashMap avec le pourcentage calculé
            resultat.replace("Pourcentage réel réclamations traitées (%)", pourcentage);
        }
    
        return resultat;
    }
    
    public HashMap<String, Double> pourcentageReclamationsTraiteesDansDelai(@Nullable FilterRequest request) {
        HashMap<String, Double> resultat = new HashMap<>();
        resultat.put("Pourcentage des réclamations traitées dans le délai (%)", 0D);
    
        List<ClaimStatus> statusTraites = Arrays.asList(
                ClaimStatus.TREAT,
                ClaimStatus.SATISFIED,
                ClaimStatus.PARTIAL_SATISFIED,
                ClaimStatus.UNSATISFIED,
                ClaimStatus.CLASSED,
                ClaimStatus.LITIGATION
        );
    
        List<Claim> allClaims;
        List<Claim> allExpiredClaims = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
    
        try {
            // Récupération des réclamations selon le filtre
            allClaims = (request != null)
                    ? claimRepository.countClaimByCriteriaAndStatusNot(request, ClaimType.CLAIM)
                    : claimRepository.findByTypeAndIsDeletedFalseAndStatusNot(ClaimType.CLAIM, ClaimStatus.TEMP_SAVED);
    
            // Filtrage des réclamations échues
            for (Claim claim : allClaims) {
                LocalDateTime supposedFinalTreatmentDate = claim.getReceiptDateTime().plusDays(claim.getObjet().getProcessingTime());
                if (now.isAfter(supposedFinalTreatmentDate)) {
                    allExpiredClaims.add(claim);
                }
            }
    
            // Compter le nombre de réclamations traitées dans le délai
            long nombreReclamationsDansDelai = allClaims.stream()
                    .filter(claim -> {
                        LocalDateTime supposedFinalTreatmentDate = claim.getReceiptDateTime().plusDays(claim.getObjet().getProcessingTime());
                        List<Solution> solutions = claim.getSolutions();
                        LocalDateTime measureDate = (solutions != null && !solutions.isEmpty()) 
                                ? solutions.get(solutions.size() - 1).getSatisfactionMeasure().getMeasureDateTime()
                                : null;
    
                        return measureDate != null 
                                && statusTraites.contains(claim.getStatus())
                                && !measureDate.isAfter(supposedFinalTreatmentDate);
                    })
                    .count();
    
            // Calculer le pourcentage des réclamations traitées dans le délai
            if (!allExpiredClaims.isEmpty()) {
                double pourcentage = (double) nombreReclamationsDansDelai / allExpiredClaims.size() * 100;
                pourcentage = Math.round(pourcentage * 100.0) / 100.0;
    
                resultat.replace("Pourcentage des réclamations traitées dans le délai (%)", pourcentage);
            }
        } catch (Exception e) {
            e.printStackTrace(); // Affiche l'erreur dans la console
            // Vous pouvez également enregistrer l'erreur dans un fichier ou une base de données pour l'analyse
        }
    
        return resultat;
    }
    
    

    // HashMap<String, Double> tauxReponse(@Nullable FilterRequest request) {
    // HashMap<String, Double> resultat = new HashMap<>();
    // List<ClaimStatus> status = Arrays.asList(ClaimStatus.TREAT,
    // ClaimStatus.SATISFIED, ClaimStatus.UNSATISFIED,
    // ClaimStatus.CLASSED, ClaimStatus.LITIGATION);
    // List<Claim> claimsTreat = new ArrayList<>();
    // resultat.put("Taux de réponses aux réclamations", 0D);
    // resultat.put("Taux de réponses aux réclamations à niveau de gravité Mineur",
    // 0D);
    // resultat.put("Taux de réponses aux réclamations à niveau de gravité Moyen",
    // 0D);
    // resultat.put("Taux de réponses aux réclamations à niveau de gravité Grave",
    // 0D);
    // if (request != null) {
    // if (request.getEtats() != null && request.getEtats().isEmpty()) {
    // request.setEtats(status);
    // claimsTreat = claimRepository.countClaimByCriteriaAndStatusIn(request,
    // ClaimType.CLAIM);
    // } else {
    // List<ClaimStatus> finalStatus = new ArrayList<>();
    // for (ClaimStatus cStatus : request.getEtats()) {
    // if (status.contains(cStatus)) {
    // finalStatus.add(cStatus);
    // }
    // }
    // if (!finalStatus.isEmpty()) {
    // request.setEtats(finalStatus);
    // claimsTreat = claimRepository.countClaimByCriteriaAndStatusIn(request,
    // ClaimType.CLAIM);
    // }
    // }
    // } else {
    // claimsTreat = claimRepository.findByTypeAndStatusIn(ClaimType.CLAIM, status);

    // }
    // Double oldVal = 0D;
    // Double oldVal2 = 0D;
    // for (Claim claim : claimsTreat) {
    // if (!claim.getSolutions().isEmpty()) {
    // for (Solution solution : claim.getSolutions()) {
    // if (solution.getSatisfactionMeasure() != null
    // || solution.getStatus().equals(SolutionStatus.APPROVED)) {
    // oldVal2++;
    // switch (claim.getObjet().getRisqueLevel()) {
    // case GRAVE:
    // oldVal = resultat
    // .get("Taux de réponses aux réclamations à niveau de gravité Grave");
    // resultat.replace("Taux de réponses aux réclamations à niveau de gravité
    // Grave",
    // oldVal + 1);
    // break;
    // case MOYEN:
    // oldVal = resultat
    // .get("Taux de réponses aux réclamations à niveau de gravité Moyen");
    // resultat.replace("Taux de réponses aux réclamations à niveau de gravité
    // Moyen",
    // oldVal + 1);
    // break;
    // case MINEUR:
    // oldVal = resultat
    // .get("Taux de réponses aux réclamations à niveau de gravité Mineur");
    // resultat.replace("Taux de réponses aux réclamations à niveau de gravité
    // Mineur",
    // oldVal + 1);
    // break;
    // }
    // oldVal = 0D;
    // }

    // }
    // }
    // }
    // resultat.replace("Taux de réponses aux réclamations",
    // oldVal2);
    // List<Claim> allClaims =
    // claimRepository.findByTypeAndStatusNot(ClaimType.CLAIM,
    // ClaimStatus.TEMP_SAVED);
    // resultat.put("Taux de réponses aux réclamations", 0D);
    // resultat.replace("Taux de réponses aux réclamations à niveau de gravité
    // Mineur", 0D);
    // resultat.replace("Taux de réponses aux réclamations à niveau de gravité
    // Moyen", 0D);
    // resultat.replace("Taux de réponses aux réclamations à niveau de gravité
    // Grave", 0D);
    // return resultat;
    // }

}
