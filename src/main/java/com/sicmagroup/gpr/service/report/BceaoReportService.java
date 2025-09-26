package com.sicmagroup.gpr.service.report;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.sicmagroup.gpr.api.report.BceaoClaimDetails;
import com.sicmagroup.gpr.api.report.BceaoReport;
import com.sicmagroup.gpr.api.report.BceaoRequest;
import com.sicmagroup.gpr.domain.enumeration.ClaimStatus;
import com.sicmagroup.gpr.domain.enumeration.ClaimType;
import com.sicmagroup.gpr.domain.enumeration.Role;
import com.sicmagroup.gpr.domain.model.Claim;
import com.sicmagroup.gpr.domain.model.Solution;
import com.sicmagroup.gpr.domain.model.Suggestion;
import com.sicmagroup.gpr.domain.model.User;
import com.sicmagroup.gpr.repository.ClaimRepository;
import com.sicmagroup.gpr.repository.SuggestionRepository;
import com.sicmagroup.gpr.repository.UserRepository;
import com.sicmagroup.gpr.utils.Utils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BceaoReportService {
    private final ClaimRepository claimRepository;
    private final SuggestionRepository suggestionRepository;
    private final UserRepository userRepository;

    public BceaoReport generateBceaoReport(BceaoRequest semestre) {
        LocalDateTime start = Utils.convertStrToLocalDateTime(semestre.getStartDate());
        LocalDateTime end = Utils.convertStrToLocalDateTime(semestre.getEndDate());
        LocalDateTime receiptDate;
        LocalDateTime measureDate;
        LocalDateTime supposedFinalTreatmentDate;
        long delaiObj;
        Solution solution;
        BceaoReport bceaoReport = BceaoReport
                .builder()
                .totalClaimTreat(0)
                .totalClaimUnResolve(0)
                .tauxClaimTreat(0D)
                .tauxClaimTreatRespectingTiming(0)
                .totalLigitigateClaimInPeriode(0)
                .totalLitigateClaim(0)
                .periode(Utils.convertLocalDateToString(start)+" au "+Utils.convertLocalDateToString(end))

                .build();

        //get pilote user and contact
        List<User> pilotes = userRepository.findByAdditionalroleIn(Arrays.asList(Role.PILOTE));
        if(pilotes.size() > 0){
                bceaoReport.setPiloteName(pilotes.get(0).getFirstandlastname());
                bceaoReport.setPiloteContact(pilotes.get(0).getTel());
        }
        // total claim
        List<Claim> claims = claimRepository.findByTypeAndStatusNotAndReceiptDateTimeBetween(ClaimType.CLAIM,
                ClaimStatus.TEMP_SAVED, start, end);
        bceaoReport.setTotalClaim(claims.size());
        // total Denun
        List<Claim> denuns = claimRepository.findByTypeAndStatusNotAndReceiptDateTimeBetween(ClaimType.DENUNCIACION,
                ClaimStatus.TEMP_SAVED, start, end);
        bceaoReport.setTotalDenun(denuns.size());
        // total suggest
        List<Suggestion> suggestions = suggestionRepository
                .findByStatusNotAndReceiptDateTimeBetween(ClaimStatus.TEMP_SAVED, start, end);
        bceaoReport.setTotalSuggest(suggestions.size());
        //
        List<ClaimStatus> treatClaimStatus = Arrays.asList(ClaimStatus.TREAT, ClaimStatus.SATISFIED,
                ClaimStatus.UNSATISFIED, ClaimStatus.CLASSED, ClaimStatus.LITIGATION, ClaimStatus.PARTIAL_SATISFIED);
        List<BceaoClaimDetails> claimsReceivedInPeriod = new ArrayList<>();
        List<BceaoClaimDetails> claimsTreatInPeriod = new ArrayList<>();
        List<BceaoClaimDetails> claimsUnResolve = new ArrayList<>();
        List<BceaoClaimDetails> claimsLitigationInPeriod = new ArrayList<>();
        int indexe = 0;
        for (Claim claim : claims) {
            BceaoClaimDetails bceaoClaimDetails = BceaoClaimDetails
                    .builder()
                    .position(indexe++)
                    .product(claim.getProduct().getLibelle())
                    .resume(claim.getContent())

                    .build();
            claimsReceivedInPeriod.add(bceaoClaimDetails);
            if (treatClaimStatus.contains(claim.getStatus())) {
                bceaoReport.setTotalClaimTreat(bceaoReport.getTotalClaimTreat() + 1);
                //
                BceaoClaimDetails bceaoClaimTreatDetails = BceaoClaimDetails
                        .builder()
                        .position(indexe++)
                        .product(claim.getProduct().getLibelle())
                        .resume(claim.getContent())

                        .build();
                claimsTreatInPeriod.add(bceaoClaimTreatDetails);
                // claim treat respection the delay
                receiptDate = claim.getReceiptDateTime();
                solution = claim.getSolutions().get((claim.getSolutions().size() - 1));
                if (solution.getSatisfactionMeasure() != null) {
                    measureDate = solution.getSatisfactionMeasure()
                            .getMeasureDateTime();

                    delaiObj = claim.getObjet().getProcessingTime();
                    supposedFinalTreatmentDate = receiptDate.plusDays(delaiObj);
                    if (!measureDate.isAfter(supposedFinalTreatmentDate)) { // il n'y a pas retard de traitement
                        bceaoReport
                                .setTauxClaimTreatRespectingTiming(bceaoReport.getTauxClaimTreatRespectingTiming() + 1);
                    }
                }
            } else {
                bceaoReport.setTotalClaimUnResolve(bceaoReport.getTotalClaimUnResolve() + 1);
                BceaoClaimDetails bceaoClaimUnResolveDetails = BceaoClaimDetails
                        .builder()
                        .position(indexe++)
                        .product(claim.getProduct().getLibelle())
                        .resume(claim.getContent())

                        .build();
                claimsUnResolve.add(bceaoClaimUnResolveDetails);
            }
            // litigate
            if (claim.getStatus().equals(ClaimStatus.LITIGATION)) {
                bceaoReport.setTotalLigitigateClaimInPeriode(bceaoReport.getTotalLigitigateClaimInPeriode() + 1);
                  BceaoClaimDetails bceaoClaimLitigate = BceaoClaimDetails
                        .builder()
                        .position(indexe++)
                        .product(claim.getProduct().getLibelle())
                        .resume(claim.getContent())

                        .build();
                claimsLitigationInPeriod.add(bceaoClaimLitigate);
            }
        }
        // taux claim treat in delai
        Double tauxClaimTreatRespectingTime = bceaoReport.getTauxClaimTreatRespectingTiming();
        bceaoReport.setTauxClaimTreatRespectingTiming(Utils.parseDouble(Utils.percentCalculator( tauxClaimTreatRespectingTime.longValue(), Long.valueOf(claims.size()))));
        // taux claim treat
        bceaoReport.setTauxClaimTreat(Utils.parseDouble(
                Utils.percentCalculator(Long.valueOf(bceaoReport.getTotalClaimTreat()), Long.valueOf(claims.size()))));
        // taux satisfcation
        List<ClaimStatus> allSatisfaction = Arrays.asList(ClaimStatus.SATISFIED, ClaimStatus.UNSATISFIED,
        ClaimStatus.PARTIAL_SATISFIED,ClaimStatus.CLASSED,ClaimStatus.LITIGATION);
        List<Claim> claims2 = claimRepository.findByTypeAndStatusInAndReceiptDateTimeBetween(ClaimType.CLAIM,
        allSatisfaction, start, end);
        // List<Claim> claimsSatisfied = claimRepository.findByTypeAndStatusIn(ClaimType.CLAIM,
        //         Arrays.asList(ClaimStatus.SATISFIED));
        List<Claim> claimsSatisfied = claimRepository.findByTypeAndStatusInAndReceiptDateTimeBetween(
                ClaimType.CLAIM, Arrays.asList(ClaimStatus.SATISFIED),start, end); 
        bceaoReport.setTauxSatisfaction(Utils.parseDouble(
                Utils.percentCalculator(Long.valueOf(claimsSatisfied.size()), Long.valueOf(claims2.size()))));
        // litigate global
        bceaoReport.setTotalLitigateClaim(
                claimRepository.findByTypeAndStatusIn(ClaimType.CLAIM, Arrays.asList(ClaimStatus.LITIGATION)).size());
        // claimsReceivedInPeriod
        bceaoReport.setClaimsReceivedInPeriod(claimsReceivedInPeriod.toArray(new BceaoClaimDetails[0]));
        // claimsTreatInPeriod
        bceaoReport.setClaimsTreatInPeriod(claimsTreatInPeriod.toArray(new BceaoClaimDetails[0]));
        // claimsUnResolveInPeriod
        bceaoReport.setClaimsUnResolveInPeriod(claimsUnResolve.toArray(new BceaoClaimDetails[0]));
        //claimsLitigateInPeriod
        bceaoReport.setClaimsLitigateInPeriod(claimsLitigationInPeriod.toArray(new BceaoClaimDetails[0]));

        return bceaoReport;
    }

}
