# Outils de test en direct

Ces scripts servent à tester l'application devant l'utilisatrice, dans une fenêtre Edge visible, avec un bandeau
« TEST AUTOMATIQUE » qui annonce chaque action. Ils ne contiennent ni mot de passe ni clé.

## Démarrer

1. Lancer le serveur (port 8080) sur la base de test `gpr_sicma_verif` et l'interface (port 3000).
2. Lancer le pilote du navigateur, depuis ce dossier :
   - `GPR_JWT_SECRET` lu dans les variables d'environnement Windows (portée Utilisateur), jamais affiché ;
   - `WS_MODULE` = chemin du module `ws` de l'interface (`.../node_modules/ws`) ;
   - `node pilote.js` : ouvre Edge (port 9333) et écoute les commandes sur http://127.0.0.1:9400.
3. Se connecter : l'utilisatrice se connecte elle-même dans la fenêtre Edge (le mot de passe de test n'est jamais
   écrit dans un fichier).

## Commandes

- `curl -X POST http://127.0.0.1:9400/eval --data-binary @fichier.js` : exécute un script dans la page.
  Toujours envoyer un fichier écrit en UTF-8 (`--data-binary`), sinon les accents sont abîmés.
- `/shot?nom=x` : capture de la page ; `/journal` : erreurs réseau et console.

## Aides (à charger dans la page, aucune action au chargement)

- `visuel.js` : bandeau d'annonce, surlignage orange, clics et saisies visibles, lecture des messages.
- `simple_visuel.js` : `__ajouterV`, `__modifierV`, `__supprimerV` pour les pages intitulé + description.
- `objet_aide.js`, `sol_aide.js`, `sol_modif.js`, `u_sel.js`, `m_aide.js`, `m_test.js` : pages Objets, Solutions,
  Utilisateurs, Email.
- `premier_plan.ps1` : remet la fenêtre de test à gauche, au premier plan ; `etat_fenetre.ps1` : dit si elle est réduite
  (fenêtre réduite = navigateur en veille, les scripts s'arrêtent).
- `ouvrir-pbi.ps1` + `capture.ps1` : ouvre un export Power BI dans Power BI Desktop et capture l'écran
  (uniquement avec l'accord de l'utilisatrice).
- `plan_doublon.py` : ajoute une ligne au plan de test Excel.
