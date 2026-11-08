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

// Chaque calcul travaille sur sa propre copie du filtre (FilterRequest.copieDe) : le filtre partagé
// par les tableaux récapitulatifs n'est jamais modifié.
@Service
@RequiredArgsConstructor
public class StatsClaim {
    private final com.sicmagroup.gpr.sla.service.EcheanceObjet echeanceObjet;

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
                // Une ligne par objet : on additionne les objets d'un même niveau de gravité.
                resultat.put(key, resultat.getOrDefault(key, 0.0) + claimPerObjLevelPro.getTotal().doubleValue());
            }

        } else {
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
        request = FilterRequest.copieDe(request);
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
                List<ClaimStatus> etatsNonTraites = new ArrayList<>();
                for (ClaimStatus cStatus : request.getEtats()) {
                    if (!status.contains(cStatus)) {
                        etatsNonTraites.add(cStatus);
                    }
                }
                if (etatsNonTraites.isEmpty()) {
                    resultat.put("Nombre de réclamations non traitées", 0D);
                } else {
                    request.setEtats(etatsNonTraites);
                    total = claimRepository.countClaimByCriteriaAndStatusIn(request, ClaimType.CLAIM).size();
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
        request = FilterRequest.copieDe(request);
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
                List<ClaimStatus> etatsNonTraites = new ArrayList<>();
                for (ClaimStatus cStatus : request.getEtats()) {
                    if (!status.contains(cStatus)) {
                        etatsNonTraites.add(cStatus);
                    }
                }
                if (!etatsNonTraites.isEmpty()) {
                    request.setEtats(etatsNonTraites);
                    claimsByStatusNot = claimRepository.countClaimByCriteriaAndStatusIn(request,
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
        request = FilterRequest.copieDe(request);
        HashMap<String, Double> resultat = new HashMap<>();
        List<ClaimStatus> status = Arrays.asList(ClaimStatus.TREAT, ClaimStatus.SATISFIED, ClaimStatus.UNSATISFIED,
                ClaimStatus.CLASSED, ClaimStatus.LITIGATION, ClaimStatus.PARTIAL_SATISFIED);
        long total = 0;
        if (request != null) {
            if ((request.getEtats() != null && request.getEtats().isEmpty()) || request.getEtats() == null) {
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
        request = FilterRequest.copieDe(request);
        HashMap<String, Double> resultat = new HashMap<>();
        int total = 0;
        List<ClaimStatus> status = Arrays.asList(ClaimStatus.TREAT, ClaimStatus.SATISFIED, ClaimStatus.UNSATISFIED,
                ClaimStatus.CLASSED, ClaimStatus.LITIGATION, ClaimStatus.PARTIAL_SATISFIED);
        resultat.put("Nombre de réclamations à niveau de gravité Mineur traitées", 0D);
        resultat.put("Nombre de réclamations à niveau de gravité Moyen traitées", 0D);
        resultat.put("Nombre de réclamations à niveau de gravité Grave traitées", 0D);
       
       
        List<Claim> claimsByStatus = new ArrayList<>();
        if (request != null) {
            if ((request.getEtats() != null && request.getEtats().isEmpty()) || request.getEtats() == null) {
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
            if (claim.getObjet() == null) {
                continue;
            }
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
        request = FilterRequest.copieDe(request);
        HashMap<String, Double> resultat = new HashMap<>();
        resultat.put("Nombre de réclamations traitées dans le délai", 0D);
        resultat.put("Nombre de réclamations à niveau de gravité Mineur traitées dans le délai", 0D);
        resultat.put("Nombre de réclamations à niveau de gravité Moyen traitées dans le délai", 0D);
        resultat.put("Nombre de réclamations à niveau de gravité Grave traitées dans le délai", 0D);
        List<ClaimStatus> status = Arrays.asList(ClaimStatus.TREAT, ClaimStatus.SATISFIED, ClaimStatus.UNSATISFIED,
                ClaimStatus.CLASSED, ClaimStatus.LITIGATION, ClaimStatus.PARTIAL_SATISFIED);
        List<Claim> claimsTreat = new ArrayList<>();
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
        LocalDateTime receiptDate;
        LocalDateTime measureDate;
        LocalDateTime supposedFinalTreatmentDate;
        Double oldVal = 0D;
        Double oldVal2 = 0D;
        long delaiObj;
        for (Claim claim : claimsTreat) {
            receiptDate = claim.getReceiptDateTime();
            measureDate = dateTraitement(claim);
            if (measureDate != null && claim.getObjet() != null && receiptDate != null) {
                delaiObj = claim.getObjet().getProcessingTime();
                supposedFinalTreatmentDate = echeanceObjet.echeance(receiptDate, delaiObj);
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
        resultat.replace("Nombre de réclamations traitées dans le délai",
                oldVal2);
        return resultat;
    }

    private static LocalDateTime dateTraitement(Claim claim) {
        if (claim == null || claim.getSolutions() == null || claim.getSolutions().isEmpty()) {
            return null;
        }
        Solution solution = claim.getSolutions().get(claim.getSolutions().size() - 1);
        if (solution == null) {
            return null;
        }
        if (solution.getSatisfactionMeasure() != null && solution.getSatisfactionMeasure().getMeasureDateTime() != null) {
            return solution.getSatisfactionMeasure().getMeasureDateTime();
        }
        return solution.getCreatedAt();
    }

    public HashMap<String, Double> totalReclamantSatisfait(@Nullable FilterRequest request) {
        request = FilterRequest.copieDe(request);
        HashMap<String, Double> resultat = new HashMap<>();
        resultat.put("Nombre de réclamations à réclamants satisfaits", 0D);

        List<ClaimStatus> status = Arrays.asList(ClaimStatus.SATISFIED);
        List<Claim> claimsTreat = new ArrayList<>();
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

        resultat.replace("Nombre de réclamations à réclamants satisfaits",
                Double.valueOf(claimsTreat.size()));
        return resultat;
    }

    public HashMap<String, Double> tauxSatisfaction(@Nullable FilterRequest request) {
        request = FilterRequest.copieDe(request);
        List<ClaimStatus> etatsChoisis = request == null ? null : request.getEtats();
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
        List<Claim> allClaims = new ArrayList<>();
        if (request != null) {
            // Dénominateur : les réclamations clôturées du même filtre (agence, période, objet...),
            // pas toutes celles de l'institution.
            List<ClaimStatus> statusDenominateur = new ArrayList<>(allSatisfaction);
            if (etatsChoisis != null && !etatsChoisis.isEmpty()) {
                statusDenominateur.retainAll(etatsChoisis);
            }
            if (!statusDenominateur.isEmpty()) {
                FilterRequest filtreDenominateur = FilterRequest.copieDe(request);
                filtreDenominateur.setEtats(statusDenominateur);
                allClaims = claimRepository.countClaimByCriteriaAndStatusIn(filtreDenominateur, ClaimType.CLAIM);
            }
        } else {
            allClaims = claimRepository.findByTypeAndIsDeletedFalseAndStatusIn(ClaimType.CLAIM, allSatisfaction);
        }
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
    
        List<Claim> filteredClaims = (request != null)
                ? reclamationsDuFiltre(request)
                : claimRepository.findByTypeAndIsDeletedFalseAndStatusNot(ClaimType.CLAIM, ClaimStatus.TEMP_SAVED);
        FilterRequest sansEtats = FilterRequest.copieDe(request);
        if (sansEtats != null) {
            sansEtats.setEtats(null);
        }
        List<Claim> allClaims = (sansEtats != null && request.getEtats() != null && !request.getEtats().isEmpty())
                ? reclamationsDuFiltre(sansEtats)
                : filteredClaims;

        long nombreReclamationsTraitees = filteredClaims.stream()
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

    // Réclamations du filtre, brouillons exclus : avec un filtre par état, celles de ces états, sinon toutes.
    // (countClaimByCriteriaAndStatusNot exclurait les états choisis et ramènerait les brouillons, sans objet.)
    private List<Claim> reclamationsDuFiltre(FilterRequest request) {
        FilterRequest copie = FilterRequest.copieDe(request);
        if (copie.getEtats() != null && !copie.getEtats().isEmpty()) {
            List<ClaimStatus> etats = new ArrayList<>(copie.getEtats());
            etats.remove(ClaimStatus.TEMP_SAVED);
            if (etats.isEmpty()) {
                return new ArrayList<>();
            }
            copie.setEtats(etats);
            return claimRepository.countClaimByCriteriaAndStatusIn(copie, ClaimType.CLAIM);
        }
        return claimRepository.countClaimByCriteriaAndStatusNot(copie, ClaimType.CLAIM);
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
                ? reclamationsDuFiltre(request)
                : claimRepository.findByTypeAndIsDeletedFalseAndStatusNot(ClaimType.CLAIM, ClaimStatus.TEMP_SAVED);
        List<Claim> allExpiredClaims = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // Filtrage des réclamations échues
        for (Claim claim : allClaims) {
            if (claim.getObjet() == null || claim.getReceiptDateTime() == null) {
                continue;
            }
            LocalDateTime supposedFinalTreatmentDate = echeanceObjet.echeance(claim.getReceiptDateTime(), claim.getObjet().getProcessingTime());
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
    
        try {
            // Récupération des réclamations selon le filtre
            allClaims = (request != null)
                    ? reclamationsDuFiltre(request)
                    : claimRepository.findByTypeAndIsDeletedFalseAndStatusNot(ClaimType.CLAIM, ClaimStatus.TEMP_SAVED);

            List<Claim> claimsTraitees = new ArrayList<>();
            for (Claim claim : allClaims) {
                if (statusTraites.contains(claim.getStatus())) {
                    claimsTraitees.add(claim);
                }
            }

            // Compter le nombre de réclamations traitées dans le délai
            long nombreReclamationsDansDelai = claimsTraitees.stream()
                    .filter(claim -> {
                        if (claim.getObjet() == null || claim.getReceiptDateTime() == null) {
                            return false;
                        }
                        LocalDateTime supposedFinalTreatmentDate = echeanceObjet.echeance(claim.getReceiptDateTime(), claim.getObjet().getProcessingTime());
                        LocalDateTime measureDate = dateTraitement(claim);
                        return measureDate != null && !measureDate.isAfter(supposedFinalTreatmentDate);
                    })
                    .count();
    
            // Calculer le pourcentage des réclamations traitées dans le délai parmi les réclamations traitées
            if (!claimsTraitees.isEmpty()) {
                double pourcentage = (double) nombreReclamationsDansDelai / claimsTraitees.size() * 100;
                pourcentage = Math.round(pourcentage * 100.0) / 100.0;
    
                resultat.replace("Pourcentage des réclamations traitées dans le délai (%)", pourcentage);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    
        return resultat;
    }
}
