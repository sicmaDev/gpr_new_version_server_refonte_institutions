package com.sicmagroup.gpr.service.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.sicmagroup.gpr.domain.model.User;
import com.sicmagroup.gpr.repository.UserRepository;
import com.sicmagroup.gpr.service.MailService;
import com.sicmagroup.gpr.service.log.LogServiceImpl;
import com.sicmagroup.gpr.service.setting.SettingServiceImpl;

import jakarta.servlet.http.HttpServletRequest;

/** Mot de passe temporaire d'un compte : réinitialisation, mail, lecture par l'administrateur, effacement. */
class MotDePasseTemporaireTest {

    private final UserRepository users = mock(UserRepository.class);
    private final MailService mail = mock(MailService.class);
    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    private AuthenticationServiceImpl service() throws Exception {
        AuthenticationServiceImpl s = mock(AuthenticationServiceImpl.class, CALLS_REAL_METHODS);
        SettingServiceImpl settings = mock(SettingServiceImpl.class);
        when(settings.getbySlug(any())).thenThrow(new IllegalStateException("absent"));
        ReflectionTestUtils.setField(s, "userRepository", users);
        ReflectionTestUtils.setField(s, "mailService", mail);
        ReflectionTestUtils.setField(s, "passwordEncoder", encoder);
        ReflectionTestUtils.setField(s, "settingServiceImpl", settings);
        ReflectionTestUtils.setField(s, "logServiceImpl", mock(LogServiceImpl.class));
        ReflectionTestUtils.setField(s, "httpServletRequest", mock(HttpServletRequest.class));
        return s;
    }

    private User user() {
        User u = new User();
        u.setId(5L);
        u.setFirstandlastname("Awa Koffi");
        u.setEmail("awa@exemple.test");
        u.setPassword(encoder.encode("ancien"));
        return u;
    }

    @Test
    void laReinitialisationGenereEnregistreEtEnvoieParMail() throws Exception {
        User u = user();
        when(users.findById(5L)).thenReturn(Optional.of(u));

        String nouveau = service().resetPassword(5L);

        assertThat(nouveau).hasSize(10);
        assertThat(encoder.matches(nouveau, u.getPassword())).isTrue();
        assertThat(u.getTemporaryPassword()).isEqualTo(nouveau);
        ArgumentCaptor<String> html = ArgumentCaptor.forClass(String.class);
        verify(mail).sendMail(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(), html.capture(), (String) any());
        assertThat(html.getValue()).contains(nouveau, "awa@exemple.test");
    }

    @Test
    void l_administrateurLitLeMotDePasseTemporaireTantQuIlExiste() throws Exception {
        User u = user();
        u.setTemporaryPassword("Tmp-123");
        when(users.findById(5L)).thenReturn(Optional.of(u));

        assertThat(service().getTemporaryPassword(5L)).isEqualTo("Tmp-123");
    }

    @Test
    void unMotDePasseChoisiParLaPersonneEffaceLaCopie() throws Exception {
        User u = user();
        u.setTemporaryPassword("Tmp-123");
        u.setPassword(encoder.encode("Tmp-123"));
        when(users.findById(anyLong())).thenReturn(Optional.of(u));
        com.sicmagroup.gpr.api.auth.UpdatePwdRequest req = new com.sicmagroup.gpr.api.auth.UpdatePwdRequest();
        req.setId(5L);
        req.setOldPassword("Tmp-123");
        req.setNewPassword("MonPropre#2026");

        service().updateAccountPwdUser(req);

        assertThat(u.getTemporaryPassword()).isNull();
        assertThat(service().getTemporaryPassword(5L)).isNull();
    }
}
