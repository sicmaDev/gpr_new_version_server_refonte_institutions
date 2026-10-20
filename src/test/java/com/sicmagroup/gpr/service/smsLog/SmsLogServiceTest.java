package com.sicmagroup.gpr.service.smsLog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.sicmagroup.gpr.domain.dto.SmsLogDto;
import com.sicmagroup.gpr.domain.enumeration.SmsStatus;
import com.sicmagroup.gpr.domain.enumeration.SmsType;
import com.sicmagroup.gpr.domain.model.Claim;
import com.sicmagroup.gpr.domain.model.SmsLog;
import com.sicmagroup.gpr.domain.model.Suggestion;
import com.sicmagroup.gpr.repository.ClaimRepository;
import com.sicmagroup.gpr.repository.SmsLogRepository;
import com.sicmagroup.gpr.repository.SuggestionRepository;

/** Archive des SMS : chaque ligne reçoit le code client de la plainte ou de la suggestion liée. */
class SmsLogServiceTest {

    private final SmsLogRepository smsRepo = mock(SmsLogRepository.class);
    private final ClaimRepository claimRepo = mock(ClaimRepository.class);
    private final SuggestionRepository suggestionRepo = mock(SuggestionRepository.class);
    private final SmsLogServiceImpl service = new SmsLogServiceImpl(smsRepo, claimRepo, suggestionRepo);

    private SmsLog sms(long id, Long claimId, Long suggestId) {
        return SmsLog.builder().id(id).type(SmsType.EXTERNE).claimId(claimId).suggestId(suggestId)
                .numeroDestinataire("0022900000000").content("Bonjour").status(SmsStatus.ENVOYE).build();
    }

    @Test
    void chaqueSmsAffichLeCodeClientDeSaPlainte() {
        Claim claim = new Claim();
        claim.setId(5L);
        claim.setCode("uuid-long-5555");
        claim.setCodeClient("REC-ab12");
        Suggestion sug = new Suggestion();
        sug.setId(9L);
        sug.setCode("uuid-sug");
        sug.setCodeClient("SUG-cd34");
        when(smsRepo.findByIsDeletedFalseOrderByCreatedAtDesc()).thenReturn(List.of(sms(1, 5L, null), sms(2, null, 9L), sms(3, null, null)));
        when(claimRepo.findAllById(java.util.Set.of(5L))).thenReturn(List.of(claim));
        when(suggestionRepo.findAllById(java.util.Set.of(9L))).thenReturn(List.of(sug));

        List<SmsLogDto> res = service.getAll();

        assertThat(res).hasSize(3);
        assertThat(res.get(0).getClaimCode()).isEqualTo("REC-ab12");
        assertThat(res.get(1).getSuggestCode()).isEqualTo("SUG-cd34");
        assertThat(res.get(2).getClaimCode()).isNull();
    }

    @Test
    void sansCodeClientOnRetombeSurLeCodeInterne() {
        Claim claim = new Claim();
        claim.setId(5L);
        claim.setCode("den-uuid-1");
        when(smsRepo.findByIsDeletedFalseOrderByCreatedAtDesc()).thenReturn(List.of(sms(1, 5L, null)));
        when(claimRepo.findAllById(java.util.Set.of(5L))).thenReturn(List.of(claim));

        assertThat(service.getAll().get(0).getClaimCode()).isEqualTo("den-uuid-1");
    }
}
