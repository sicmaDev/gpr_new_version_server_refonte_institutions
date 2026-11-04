# Consignes de travail (refonte GPR)

**Au démarrage : lire `SUIVI_TESTS.md` (même dossier) et reprendre là où le travail s'est arrêté.**

## Façon de travailler
- Toujours répondre en **français simple, sans jargon** technique.
- Travailler sur la branche **main** (pas de branche dédiée, sauf demande explicite).
- Avancer **progressivement** : une page / une correction à la fois, vérifiée à l'écran, puis notée dans le plan de test.
- Avant chaque page à tester : **demander quoi tester**, puis faire les tests **dans la fenêtre Edge visible** (bandeau
  « TEST AUTOMATIQUE »), voir `outils-tests/LISEZMOI.md`.
- Remplir `Plan_de_test_GPR_Refonte_vierge.xlsx` : une ligne par cas, avec les **erreurs constatées et corrigées**.
- Ne pas piloter l'écran (captures, clics, fenêtres au premier plan) sans l'accord de l'utilisatrice.

## Interdits et précautions
- **Pas de push** sans demande explicite. Ne jamais commiter `maquettes/`. Demander avant d'ajouter une dépendance.
- **Historique Git** (fichiers sensibles déjà poussés) : l'utilisatrice s'en occupe, ne pas y toucher.
- **Secrets** (`GPR_JWT_SECRET`, `GPR_ENCRYPTION_KEY`, `GPR_HMAC_KEY`, `WHATGPR_INTERNAL_SECRET`) : lus seulement dans les
  variables d'environnement Windows (portée Utilisateur), jamais affichés ni écrits dans un fichier.
- **Mot de passe de test** : jamais écrit dans un fichier ; c'est l'utilisatrice qui se connecte dans la fenêtre de test.
- Tests sur la **copie `gpr_sicma_verif`** uniquement. Tout réglage modifié pour un test est sauvegardé avant puis remis
  à l'identique. Adresses email de test en `@example.com` (n'arrivent chez personne).
- À la toute fin du chantier : résumé complet, en français simple, du volet chiffrement.
