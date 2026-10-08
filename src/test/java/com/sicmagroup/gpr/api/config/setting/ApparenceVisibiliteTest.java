package com.sicmagroup.gpr.api.config.setting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.sicmagroup.gpr.domain.dto.ApiResponseDto;
import com.sicmagroup.gpr.service.setting.SettingServiceImpl;
import com.sicmagroup.gpr.utils.Constante;

@ExtendWith(MockitoExtension.class)
class ApparenceVisibiliteTest {

    @Mock
    SettingServiceImpl service;
    @InjectMocks
    SettingController controller;

    @Test
    void lesCouleursNePeuventPlusEtreChangeesQuandLApparenceEstMasquee() throws Exception {
        when(service.isAppearanceVisible()).thenReturn(false);

        ResponseEntity<ApiResponseDto> reponse = controller.configAppearance(new AppearanceRequest("#111", "#222", null));

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(service, never()).update(any());
        verify(service, never()).save(any());
    }

    @Test
    void lesAppelsGeneriquesNePeuventNiToucherLeReglageReserveNiLesCouleursMasquees() throws Exception {
        when(service.isAppearanceVisible()).thenReturn(false);

        assertThat(controller.config(AddSettingRequest.builder().libelle(Constante.APPEARANCE_VISIBLE_SLUG).value("true").build())
                .getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(controller.configUpdate(Constante.APPEARANCE_SLUG,
                UpdateSettingRequest.builder().libelle(Constante.APPEARANCE_SLUG).value("{}").build())
                .getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(service, never()).save(any());
        verify(service, never()).update(any());
    }

    @Test
    void leReglageDeVisibiliteResteInterditAuxAppelsGeneriquesMemeQuandLApparenceEstVisible() throws Exception {
        assertThat(controller.config(AddSettingRequest.builder().libelle(Constante.APPEARANCE_VISIBLE_SLUG).value("true").build())
                .getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(service, never()).save(any());
    }
}
