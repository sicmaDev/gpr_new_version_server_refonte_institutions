package com.sicmagroup.gpr.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MailServiceMasqueTest {

    @Test
    void masqueLaValeurDuMotDePasse() {
        assertEquals("Email : a@b.c\nMot de passe : ********\nFin",
                MailService.masquerMotDePasse("Email : a@b.c\nMot de passe : Abc123@!\nFin"));
    }

    @Test
    void masqueUnMotDePasseTemporaire() {
        assertEquals("Mot de passe temporaire : ********",
                MailService.masquerMotDePasse("Mot de passe temporaire : Abc123"));
    }

    @Test
    void neTouchePasAuTexteOrdinaire() {
        String texte = "Changez votre mot de passe depuis votre profil.";
        assertEquals(texte, MailService.masquerMotDePasse(texte));
    }
}
