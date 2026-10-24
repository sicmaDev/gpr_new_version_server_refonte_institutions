package com.sicmagroup.gpr.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Mails de compte : identifiants présents, style du système, valeurs échappées. */
class AccountMailsTest {

    @Test
    void leMailDeCreationContientLesIdentifiantsEtLeStyleDuSysteme() {
        String html = AccountMails.creation("Banque Test", "Awa Koffi", "awa@exemple.test", "Ab3$xyzK9!");

        assertThat(html).contains("Création de votre compte - Banque Test", "Awa Koffi", "awa@exemple.test", "Ab3$xyzK9!")
                .contains("#004080", "#f0f8ff", "Cet email a été généré automatiquement");
        assertThat(AccountMails.subjectCreation("Banque Test")).isEqualTo("Création de votre compte - Banque Test");
    }

    @Test
    void leMailDeReinitialisationLeDit() {
        String html = AccountMails.reset("Banque Test", "Awa", "awa@exemple.test", "Zz9!aaaaBB");
        assertThat(html).contains("Réinitialisation de votre mot de passe - Banque Test", "réinitialisé", "Zz9!aaaaBB");
    }

    @Test
    void lesValeursInsereesSontEchappees() {
        String html = AccountMails.creation("A&B <Co>", "<script>x</script>", "a@b.test", "p<w>&");
        assertThat(html).doesNotContain("<script>").contains("A&amp;B &lt;Co&gt;", "p&lt;w&gt;&amp;");
    }
}
