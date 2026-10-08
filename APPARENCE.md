# Menu Apparence : le verrouiller pour une institution

Par défaut, le menu **Apparence** (couleurs et logo) est visible. SICMA peut le verrouiller (lecture seule) pour une institution
qui n'a pas pris cette option, avec une seule commande dans la base de données. Aucun bouton ni code secret
dans l'application : seul celui qui a accès à la base peut le faire.

## Dans quel ordre, et où

1. **Au départ** : ne rien lancer. Le menu Apparence est visible.
2. **Pendant la configuration de l'institution** : régler les couleurs et le logo avec le menu Apparence.
3. **Une fois la configuration terminée** : lancer la commande « Masquer » ci-dessous. Après cela, les couleurs
   et le logo ne sont plus modifiables depuis l'application.

Les commandes se lancent **dans la base de données de l'institution** (MariaDB), pas dans le terminal du
serveur : avec phpMyAdmin (onglet « SQL ») ou avec le client `mysql`. Vérifier que c'est bien la base de la
bonne institution avant de valider.

Pour tester en local : lancer « Masquer » sur la base locale, se reconnecter (la page Apparence est en lecture seule), puis lancer
« Réafficher » pour le récupérer.

## Masquer (après la configuration de l'institution)

```sql
INSERT INTO gps_setting (libelle, value, created_at, updated_at)
VALUES ('app-appearance-visible', 'false', NOW(), NOW())
ON DUPLICATE KEY UPDATE value = 'false', updated_at = NOW();
```

## Réafficher

```sql
INSERT INTO gps_setting (libelle, value, created_at, updated_at)
VALUES ('app-appearance-visible', 'true', NOW(), NOW())
ON DUPLICATE KEY UPDATE value = 'true', updated_at = NOW();
```

(Supprimer la ligne `app-appearance-visible` revient au même : sans ligne, le menu est visible.)

## Ce que ça change

- Le menu **Apparence** reste visible, mais la page passe en **lecture seule** : tout est grisé et un bandeau
  indique « Option non incluse dans votre abonnement ». L'institution voit ce que fait la fonction, sans pouvoir agir.
- Le serveur refuse aussi tout changement des couleurs et du logo, même par un appel direct.
- Les couleurs et le logo déjà enregistrés restent appliqués.
- L'institution ne peut pas remettre l'option : le réglage `app-appearance-visible` est refusé
  par les appels de réglages de l'application.
- Les personnes déjà connectées voient la page en lecture seule à leur prochaine connexion (le serveur, lui, refuse
  tout de suite).

Ne jamais lancer ces commandes sur une base sans en avoir validé le choix avec le responsable de l'institution.
