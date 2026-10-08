package com.sicmagroup.gpr.api.config;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import com.sicmagroup.gpr.service.jwt.JwtServiceImpl;

/**
 * SLA lot 1 : un objet ne peut plus avoir un délai de traitement inférieur à 1 jour.
 * Les requêtes sont refusées avant tout accès à la base : rien n'est créé ni modifié.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ObjetDelaiTest {

    private static final String ADMIN = "admin.test@gpr.local";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtServiceImpl jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    private final UserDetails admin = User.withUsername(ADMIN).password("x").authorities("H12", "DE").build();

    @BeforeEach
    void setUp() {
        when(userDetailsService.loadUserByUsername(anyString())).thenReturn(admin);
    }

    private String bearer() {
        return "Bearer " + jwtService.generateToken(admin);
    }

    @Test
    void ajoutAvecDelaiZeroEstRefuse() throws Exception {
        mockMvc.perform(post("/api/v1/config/objet/add").header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"libelle\":\"objet-test-delai\",\"categorie\":1,\"processingTime\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("au moins 1 jour")));
    }

    @Test
    void ajoutSansDelaiEstRefuse() throws Exception {
        // processingTime absent => 0
        mockMvc.perform(post("/api/v1/config/objet/add").header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"libelle\":\"objet-test-delai\",\"categorie\":1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void modificationAvecDelaiNegatifEstRefusee() throws Exception {
        mockMvc.perform(put("/api/v1/config/objet/1/update").header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":1,\"libelle\":\"objet-test-delai\",\"categorie\":1,\"processingTime\":-3}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("au moins 1 jour")));
    }
}
