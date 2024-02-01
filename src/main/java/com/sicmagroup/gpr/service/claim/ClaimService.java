package com.sicmagroup.gpr.service.claim;

import java.util.List;

import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.web.multipart.MultipartFile;

import com.sicmagroup.gpr.api.claim.ProposedSolutionRequest;
import com.sicmagroup.gpr.api.claim.SaveRequest;
import com.sicmagroup.gpr.api.denunciation.DenunRequest;
import com.sicmagroup.gpr.api.denunciation.SaveDenunRequest;
import com.sicmagroup.gpr.domain.dto.AlertDto;
import com.sicmagroup.gpr.domain.enumeration.ClaimStatus;
import com.sicmagroup.gpr.domain.enumeration.ClaimType;
import com.sicmagroup.gpr.domain.enumeration.SatisfactionStatus;
import com.sicmagroup.gpr.domain.model.Claim;
import com.sicmagroup.gpr.domain.model.ExternalRecourse;
import com.sicmagroup.gpr.domain.model.Solution;
import com.sicmagroup.gpr.domain.model.User;

public interface ClaimService {

        public List<Claim> getAll(ClaimType type);

        public Claim getById(Long id) throws NotFoundException;

        public Claim saveClaim(SaveRequest claimToSave, ClaimType type) throws Exception;

        public Claim saveClaim(SaveDenunRequest claimToSave, ClaimType type) throws Exception;

        public Claim saveTempClaim(SaveRequest claimToSave, ClaimType type) throws Exception;

        public Claim affectTreatmentToUser(Claim claim, User affectedTo, User affectedBy, Boolean anonymous, String remoteAddress)
                        throws Exception;

        public Claim treatClaim(Claim claim, User treator, ProposedSolutionRequest request) throws Exception;

        public Claim measureClaim(Claim claim, Solution solution, User measurer, SatisfactionStatus status, String commentaire);

        public Claim unApprouvedSolution(Claim claim, Solution solution, User unApprouver, String commentaire);

        public Claim approuvedSolution(Claim claim, Solution solution, User approuver);

        public Claim classedClaim(Claim claim, User classer);

        public Claim unClassedClaim(Claim claim, User classer);

        public Claim litigateClaim(Claim claim, User litigator, List<ExternalRecourse> externalRecourses);

        public List<Claim> getClaimByStatus(ClaimType type, ClaimStatus status);

        public List<Claim> getAllNotTempSave(ClaimType type);

        public List<Claim> getAllByTypeStatusCollector(ClaimType type, ClaimStatus status, User collector);

        public List<Claim> getAllByTypeAndStatusIn(ClaimType type, List<ClaimStatus> statusList);

        public List<Claim> getAllByTypeAndCollectorAndStatusOrTreatmentAffectedToAndStatusIn(ClaimType type,
                        ClaimStatus status, User affectedTo, List<ClaimStatus> statusList);

        public List<Claim> getAllWithLatestApprouvedSolutionByTypeAndStatusIn(ClaimType type,
                        List<ClaimStatus> statusList);

        public List<Claim> getAllWithLatestSolutionByTypeAndStatusOrTypeAndTreatmentAffectedToAndStatusIn(
                        ClaimType type,
                        ClaimStatus status, User affectedTo, List<ClaimStatus> statusList);

        public List<Claim> getAllWithApprovedSolutionByTypeAndStatus(ClaimType type, List<ClaimStatus> statusList);

        public List<Claim> getAllByTypeAndStatusNotIn(ClaimType type, List<ClaimStatus> status);

        public List<AlertDto> getAllAlertDtosByType(ClaimType type);

        public Claim getByCode(String code) throws Exception;

        public void saveClaimOffline(SaveRequest claimToSave, ClaimType type) throws Exception;

        public void saveTempClaimOffline(SaveRequest claimToSave, ClaimType type) throws Exception;

        public void saveDenunOffline(SaveDenunRequest claimToSave, ClaimType type) throws Exception;

        public void saveTempDenunOffline(SaveDenunRequest claimToSave, ClaimType type) throws Exception;

        public Long countClaims();

        public Claim transmitClaim(Claim claim) throws Exception;

        public List<Claim> getClaimsWhenUserIsInGuestChat(User user, List<Claim> excludeClaims);

        public List<Claim> getClaimsWhenUserIsInGuestChatSuper(User user, List<Claim> excludeClaims);

}
