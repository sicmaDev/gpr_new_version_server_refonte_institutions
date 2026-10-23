package com.sicmagroup.gpr.api.extra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.NoSuchElementException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import com.sicmagroup.gpr.domain.model.User;
import com.sicmagroup.gpr.service.auth.AuthenticationServiceImpl;
import com.sicmagroup.gpr.service.extra.ExtraContentService;

/** Suppression d'un contenu supplémentaire : 404 si introuvable, 403 si ce n'est pas le sien. */
class ExtraContentControllerTest {

    private final ExtraContentService service = mock(ExtraContentService.class);
    private final AuthenticationServiceImpl auth = mock(AuthenticationServiceImpl.class);
    private final ExtraContentController controller = new ExtraContentController(service, auth);

    ExtraContentControllerTest() throws Exception {
        UserDetails principal = mock(UserDetails.class);
        when(principal.getUsername()).thenReturn("agent@exemple.test");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null));
        when(auth.getByEmail(any())).thenReturn(new User());
    }

    @AfterEach
    void clean() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void contenuInexistantDonne404() throws Exception {
        doThrow(new NoSuchElementException()).when(service).deleteExtraContent(any(), any());
        assertThat(controller.deleteExtraContent(1L).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void contenuDUnAutreDonne403() throws Exception {
        doThrow(new Exception("Vous n'êtes pas autorisé à supprimer ce contenu")).when(service).deleteExtraContent(any(), any());
        assertThat(controller.deleteExtraContent(1L).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void suppressionReussieDonne200() {
        assertThat(controller.deleteExtraContent(1L).getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
