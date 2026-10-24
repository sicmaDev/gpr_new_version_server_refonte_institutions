package com.sicmagroup.gpr.utils;

/**
 * Mails de compte (création, réinitialisation), au même style que les autres mails du système :
 * carte blanche sur fond gris, titre bleu, encadré des identifiants, mention d'envoi automatique.
 * Toutes les valeurs insérées sont échappées.
 */
public final class AccountMails {

    private AccountMails() {
    }

    public static String subjectCreation(String platform) {
        return "Création de votre compte - " + platform;
    }

    public static String subjectReset(String platform) {
        return "Réinitialisation de votre mot de passe - " + platform;
    }

    public static String creation(String platform, String name, String email, String password) {
        return build(platform, "Création de votre compte",
                "Votre compte a été créé avec succès sur la plateforme de gestion des plaintes et réclamations (<strong>"
                        + escape(platform) + "</strong>).",
                name, email, password);
    }

    public static String reset(String platform, String name, String email, String password) {
        return build(platform, "Réinitialisation de votre mot de passe",
                "Votre mot de passe a été <strong>réinitialisé</strong> sur la plateforme de gestion des plaintes et réclamations (<strong>"
                        + escape(platform) + "</strong>).",
                name, email, password);
    }

    private static String build(String platform, String title, String intro, String name, String email, String password) {
        return """
                <html>
                    <body style="font-family: Arial, sans-serif; background-color: #f7f7f7; padding: 20px;">
                        <div style="max-width: 600px; margin: auto; background: white; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); padding: 20px;">
                            <h2 style="color: #004080; text-align: center;">%s - %s</h2>

                            <p>Bonjour <strong>%s</strong>,</p>
                            <p>%s</p>

                            <div style="margin-top: 20px; background-color: #f0f8ff; border-left: 4px solid #004080; padding: 10px 15px;">
                                <p style="margin: 0;"><strong>Vos informations de connexion :</strong></p>
                                <p style="margin: 5px 0;"><strong>Email :</strong> %s</p>
                                <p style="margin: 5px 0;"><strong>Mot de passe :</strong> <span style="font-family: monospace; font-size: 15px;">%s</span></p>
                            </div>

                            <p style="margin-top: 20px;">
                                Nous vous recommandons de modifier ce mot de passe depuis votre profil après votre première connexion.
                            </p>

                            <p style="margin-top: 25px; color: #cc0000; font-size: 0.9em;">
                                Ce message contient des informations sensibles. Ne le partagez avec personne.
                            </p>

                            <p style="margin-top: 30px;">Cordialement,<br>L'équipe %s</p>

                            <p style="font-size: 12px; color: gray; text-align: center; margin-top: 30px;">
                                Cet email a été généré automatiquement. Merci de ne pas y répondre.
                            </p>
                        </div>
                    </body>
                </html>
                """.formatted(escape(title), escape(platform), escape(name), intro, escape(email), escape(password), escape(platform));
    }

    static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
