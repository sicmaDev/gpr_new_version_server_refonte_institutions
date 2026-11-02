package com.sicmagroup.gpr.service.report.carte;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.sicmagroup.gpr.domain.enumeration.Role;
import com.sicmagroup.gpr.domain.model.Poste;
import com.sicmagroup.gpr.domain.model.ServicePointLocalisation;
import com.sicmagroup.gpr.domain.model.User;
import com.sicmagroup.gpr.service.report.PerimetreRapport;

/**
 * Carte du rapport Global : règles de saisie de la localisation et périmètre « élevé » (PerimetreRapport, partagé avec le rapport).
 * Sans base de données.
 */
class CarteRapportTest {

    private static ServicePointLocalisation loc(Double lat, Double lon, String ville) {
        return ServicePointLocalisation.builder().latitude(lat).longitude(lon).ville(ville).build();
    }

    private static User user(String habilitations, Role role) {
        Poste poste = new Poste();
        poste.setHabilitations(habilitations);
        User u = new User();
        u.setPoste(poste);
        u.setAdditionalrole(role);
        return u;
    }

    @Test
    void localisationValideOuVide() {
        assertThat(LocalisationRegles.verifier(loc(6.37, 2.46, "Cotonou"))).isNull();
        assertThat(LocalisationRegles.verifier(loc(null, null, null))).isNull();
    }

    @Test
    void latitudeEtLongitudeVontEnsemble() {
        assertThat(LocalisationRegles.verifier(loc(6.37, null, null))).contains("ensemble");
        assertThat(LocalisationRegles.verifier(loc(null, 2.46, null))).contains("ensemble");
    }

    @Test
    void plagesRespectees() {
        assertThat(LocalisationRegles.verifier(loc(95.0, 2.0, null))).contains("latitude");
        assertThat(LocalisationRegles.verifier(loc(6.0, 200.0, null))).contains("longitude");
        assertThat(LocalisationRegles.verifier(loc(-90.0, -180.0, null))).isNull();
    }

    @Test
    void villeTropLongueRefusee() {
        assertThat(LocalisationRegles.verifier(loc(null, null, "x".repeat(121)))).contains("120");
    }

    @Test
    void eleveCommeLeRapport() {
        PerimetreRapport p = new PerimetreRapport(null, null);
        assertThat(p.estEleve(user("H1,H11", Role.PILOTE))).isTrue();
        assertThat(p.estEleve(user("H1,H11", Role.DE))).isTrue();
        assertThat(p.estEleve(user("H1, H6 ,H11", Role.MOLDUE))).isTrue();
        assertThat(p.estEleve(user("H1,H11,H14", Role.MOLDUE))).isFalse();
        assertThat(p.estEleve(user("H16,H11", Role.MOLDUE))).isFalse();
    }
}
