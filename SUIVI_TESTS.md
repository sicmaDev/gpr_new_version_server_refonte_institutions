# Suivi de la campagne de tests en direct

Mis à jour le 04/11/2026, avant le passage sur un autre PC.
Projets : ce dépôt (serveur) et `sicmaDev-sicmaDev-gpr_new_version_client_refonte_institutions` (interface), tous deux sur main.

## 1. Où on s'est arrêté (à reprendre dans cet ordre)

1. **Export Power BI : pages vides à l'ouverture** (priorité). Le fichier `.pbit` est complet (6 pages, 92 visuels,
   7 tables de données remplies) et Power BI Desktop 2.159 lit bien les données, mais il affiche un rapport vide
   « Sans titre » au lieu des pages, sans message. Une partie de la description des pages (`Report/Layout`) doit être
   refusée. Piste : générer le fichier page par page (ou visuel par visuel) pour trouver l'élément refusé.
   Code : `service/report/powerbi/*`, `api/report/ExportPowerBiController.java`. Ouvrir avec `outils-tests/ouvrir-pbi.ps1`
   (avec l'accord de l'utilisatrice) ; le chargement prend 3 à 5 minutes sur un PC lent.
2. **Doublons avec majuscules et espaces en trop** (même défaut sur plusieurs pages), page par page :
   - Modalités de dépôt, Catégories, Langues : corrigés et revérifiés (plan lignes 94-97, 112, 113).
   - **Objets** : correction faite (ObjetController), **pas encore revérifiée à l'écran**. Dans la base de test, à
     supprimer à l'écran : « TEST Doublon Objet » et le doublon « ␣␣test␣␣␣DOUBLON objet␣␣ » (créé avant correction).
     Puis refaire l'essai (ajout et modification) et noter la ligne 114 du plan (`outils-tests/plan_doublon.py`).
   - **Recours externes** : à faire (ExternalRecourseController, même correction `Utils.nettoyerLibelle`).
3. Suite du module Configuration : SMS, Archive des envois, Fréquences alertes, API, Recours Externes, Notifications,
   RSD supprimées, puis Ressources, Backup et surveillance, Synchronisation.
4. Autres modules : Réclamations, Dénonciations, Suggestions (enregistrement, affectation, traitement, clôture),
   Rapports, Alertes, Plaintes WhatsApp, Aide.

## 2. Déjà testé (détail dans le plan Excel)
Tableaux de bord ; Configuration : Institution, Apparence, Points de services, Postes, Produits, Catégories, Objets,
Solutions, Langues, Modalités de dépôt, Compte Utilisateurs, Email. Dernière ligne remplie du plan : **113**.

## 3. Points en attente d'une décision de l'utilisatrice
- Fautes d'accord dans les messages (« Langue ajouté », « Solution modifié », « Catégorie d'objet supprimé »…).
- Licence : les comptes désactivés occupent une place (16 / 16 atteint vite). Voulu ou non ?
- Test d'envoi d'email réel : demander une adresse de test à l'utilisatrice (les adresses @example.com sont refusées par
  le serveur mail Hostinger, ce qui prouve seulement que connexion et mot de passe fonctionnent).
- Lectures de mesure de satisfaction sans protection dans `ReportServiceImpl.java` (lignes ~3523, 3674, 3766, 4010, 4189) :
  même risque de plantage que celui corrigé dans `StatsClaim.java`, à vérifier.
- Plus anciens : renommer l'entrée de menu « Ressources » (en double), mise à jour de libphonenumber-js (accord requis),
  confirmer la règle sur les agents.
- Remarques notées non corrigées : « 1 élément prêt à enregistrer » sur un poste sans habilitation ; données bizarres
  de la copie (Agence B de type DIRECTION, « PRODUIT A » avec espace final, objet « TEST GRAVE » à 0 jour).

## 4. Installation sur un nouveau PC
- `git pull` des deux dépôts ; base MariaDB : importer `gpr_sicma_verif.sql` (copié à la main, **jamais dans Git** :
  copie de vraies données) dans une base `gpr_sicma_verif`.
- Recréer les variables d'environnement Windows (portée Utilisateur) : `GPR_JWT_SECRET`, `GPR_ENCRYPTION_KEY`,
  `GPR_HMAC_KEY`, `WHATGPR_INTERNAL_SECRET` (mêmes valeurs qu'avant, sinon les données chiffrées de la copie sont illisibles).
- Licence : le fichier `data.txt` est propre à chaque poste (jamais dans Git).
- Outils de test : `outils-tests/LISEZMOI.md`.

## 5. Constats techniques utiles
- Fenêtre de test réduite = navigateur en veille, tous les scripts s'arrêtent : la garder visible.
- Recharger la page peut déconnecter : l'utilisatrice doit alors se reconnecter elle-même.
- Après `mvnw -o -q compile`, le serveur redémarre seul (devtools) ; attendre « Started GprApplication » dans le journal.
- Doublons : l'index unique de la base ignore les majuscules et les espaces de fin, pas les espaces de début ni les
  espaces en double ; d'où `Utils.nettoyerLibelle` côté serveur.
