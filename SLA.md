# Suivi des délais (SLA) — GPR

Ce document explique ce que fait le SLA, comment l'activer, les règles appliquées, ce qui a été décidé quand le
cahier des charges laissait un choix, et ce qu'il faut surveiller en production.

## 1. En bref

Le SLA **surveille** les délais ; il ne traite jamais une plainte à la place des humains. Il calcule les échéances,
prévient les responsables, fait remonter un dossier abandonné et trace tout dans l'historique du dossier.

- **Le serveur est la seule source de vérité.** Un seul calcul alimente l'écran de suivi, le menu, les alertes, le
  tableau de bord, les listes et les rapports. Le navigateur n'effectue plus aucun calcul de date.
- **Tout est derrière l'interrupteur `sla.enabled`** (désactivé par défaut). Éteint, GPR se comporte comme avant
  (voir §3 pour les rares exceptions voulues).
- **Le SLA ne peut jamais bloquer un traitement.** Si le SLA plante, l'erreur est notée dans les journaux et l'agent
  continue de travailler.

## 2. Activer, désactiver

Écran **Délais (SLA) → Paramètres** (administrateur) : case « Activer le suivi des délais ».

À l'activation :
1. Les plaintes déjà ouvertes sont **reprises** : un compteur est créé pour chacune, **sans aucune alerte
   rétroactive** (aucun rappel, retard ni remontée pour ce qui est déjà dépassé).
2. Le menu « Suivi des délais » apparaît pour tous, avec le badge des plaintes en retard du périmètre de chacun.
3. L'ancien envoi quotidien de 8 h (`AlertRetardCron`) est arrêté : les rappels viennent désormais du SLA.

Désactiver remet GPR dans son fonctionnement d'avant. Les compteurs restent en base ; si on réactive plus tard, une
nouvelle reprise silencieuse est faite.

## 3. Ce qui change même quand le SLA est éteint

| Changement | Pourquoi |
|---|---|
| Un objet ne peut plus avoir un délai de **0 jour** (message d'erreur). Les objets déjà à 0 ne sont pas modifiés. | 0 jour faisait paraître toute plainte « en retard ». Avec le SLA actif, le délai peut rester **vide** (= délai de la politique). |
| La page Alertes ignore les objets à délai 0, les plaintes supprimées, et ne renvoie jamais le nom d'un dénonciateur. | Mêmes raisons. |
| Quand on choisit une solution dans une session, l'historique note « Solution approuvée ». | Il manquait cet événement. |
| Le statut `WAITING_CUSTOMER` existe dans l'énumération. | Voir §6. |
| Les tableaux de bord ne renvoient plus le nom d'un dénonciateur. | Règle d'identité masquée. |

## 4. Les règles

**Quatre délais** : enregistrement (mesuré, sans limite), prise en charge, résolution (indicateur principal),
clôture (réclamations seulement : après l'approbation de la solution, jusqu'à la mesure de satisfaction).

**Politique** par type de plainte et niveau de risque, configurable. Valeurs par défaut :

| Risque | Prise en charge | Résolution | Clôture | Rappels | Grâce |
|---|---|---|---|---|---|
| GRAVE | 4 h ouvrées | 3 j ouvrés | 2 j ouvrés | 50 % et 75 % | 1 j ouvré |
| MOYEN | 1 j ouvré | 6 j ouvrés | 3 j ouvrés | 50 % et 75 % | 1 j ouvré |
| MINEUR | 2 j ouvrés | 10 j ouvrés | 5 j ouvrés | 50 % et 75 % | 1 j ouvré |

Le délai de l'objet, s'il est renseigné, **prime** sur la résolution ; le délai de prise en charge de l'objet (en
heures) prime sur celui de la politique. Les échéances sont **figées à l'ouverture** : modifier une politique ne change
pas les plaintes en cours. Le chronomètre part de la date de **réception**, pas de la date de saisie.

**Temps ouvré** : jours travaillés, horaires et jours fériés du calendrier (par défaut lundi à vendredi, 8 h à 17 h,
donc un jour ouvré = 9 h). Exemples vérifiés par des tests : lundi 9 h + 3 jours ouvrés = jeudi 9 h ; vendredi 16 h 30 +
4 h = lundi 11 h 30 (mardi si le lundi est férié) ; passage d'une année à l'autre.

**Échéance réglementaire** : 30 jours calendaires depuis la réception (circulaire n° 002-2020/CB/C), alerte 5 jours
avant, jamais mise en pause. Passé la limite, la plainte est marquée définitivement « Hors délai réglementaire ».

**Pause « en attente du client »** : le délai interne s'arrête ; à la reprise, les échéances internes reculent de la
durée de la pause ; l'échéance réglementaire ne bouge pas.

**Rappels, alertes, remontée** :
1. Rappels à 50 % puis 75 % : à l'agent affecté, **avec le RA de l'agence en copie** (au RA seul si non affectée).
   Regroupés en un récapitulatif quotidien (réglable).
2. Dépassement : alerte immédiate (e-mail et SMS) à l'agent et au RA. Le dossier reste chez l'agent.
3. Sans **aucune action humaine** pendant le délai de grâce, le dossier est **transmis** au niveau supérieur, un
   niveau à la fois : agent → RA de l'agence → direction de rattachement → Pilote → DE. Chaque niveau a son délai de
   grâce ; le niveau suivant reçoit le dossier, le niveau d'après est en copie. Niveaux absents sautés.
4. Action humaine = affectation/réaffectation, solution proposée, ouverture d'une session, mise en attente du client,
   justification du retard. La simple consultation ne compte pas.
5. Le chronomètre du client n'est **jamais** remis à zéro par une remontée. Après le DE : plus de transmission,
   récapitulatif quotidien au Pilote et au DE.

**Dénonciations** : mêmes délais, rappels, copie au RA et remontée, mais **pas de délai de clôture** (TREAT vaut
clôture) et **identité du dénonciateur toujours masquée** (écrans, e-mails, SMS, exports : seulement le code interne,
l'objet, l'agence et l'échéance).

**Suggestions** : un seul délai de réponse, alerte au RA, sans escalade.

**Réclamations traitées non mesurées** : délai de clôture avec rappels ; tentatives de contact enregistrées ; après
3 tentatives sans succès (réglable) en 7 jours, le Pilote peut clôturer « client injoignable » (ni satisfait ni
insatisfait).

**Message d'attente au client** (réclamations seulement) : `NONE` (défaut), `MANUAL` (bouton proposé à l'agent) ou `AUTO`.

## 5. Données créées

Tables (créées automatiquement au démarrage) : `gps_claim_sla` (un compteur par plainte et par cycle),
`gps_sla_event` (registre des alertes, clé de déduplication unique), `gps_sla_policy`, `gps_business_calendar`,
`gps_holiday`, `gps_sla_breach_reason`, `gps_contact_attempt`. Paramètres `sla.*` dans `gps_setting`. Colonne
`takeover_hours` sur `gps_objet`. Aucune de ces tables ne contient d'identité de client (le commentaire de justification
et celui des tentatives de contact sont chiffrés comme le reste).

Au premier démarrage, la configuration par défaut (7 politiques, calendrier, motifs) est créée si elle n'existe pas.

## 6. Décisions prises (là où le cahier des charges laissait un choix) et écarts

- **Branche** : travail sur `main`, comme demandé en cours de projet (le prompt parlait de `feature/sla`).
- **Statut `WAITING_CUSTOMER`** : il existe dans l'énumération, mais la pause est suivie **dans le compteur SLA**
  sans changer le statut de la plainte. Raison : les listes d'agents filtrent par statut (affectée, désapprouvée...) ;
  changer le statut ferait **disparaître** la plainte de la liste de l'agent qui doit la reprendre.
- **Échéance réglementaire** : elle s'arrête quand la solution est **approuvée** (le serveur ne sait pas détecter l'envoi
  de la réponse au client). L'envoi au client reste visible dans l'historique.
- **Plainte classée ou en contentieux** : « suspendue », hors indicateurs.
- **« Client injoignable »** : la clôture est faite **dans le SLA** ; le statut de la plainte reste `TREAT`, donc elle
  reste visible dans la liste de mesure.
- **« Direction »** = le RA du point de service parent (`direction_id`). Les rôles **RR et DG n'existent pas** dans le
  modèle de données (seuls DE, PILOTE, MOLDUE) : ils ne sont pas gérés. Une hiérarchie en boucle ne bloque pas la remontée.
- **Une base par institution** : le code n'a aucune notion d'`institution_id` (les colonnes de ce nom visibles dans
  `gps_setting` sont des restes).
- **Sessions** (invités) : non gérées dans le périmètre de visibilité (les listes existantes ne les gèrent pas non plus).
- **Exports** : tableaux exportables en **CSV** (s'ouvre dans Excel) et impression/PDF par le navigateur. Pas de
  PDF/Word générés par le serveur.
- **Ancien `AlertRetardCron`** : arrêté quand le SLA est actif. Il était de toute façon **inopérant** sur MariaDB (la
  fonction `DATEADD` n'existe pas) et avalait l'erreur ; je ne l'ai pas « réparé » pour ne pas faire partir d'un coup des
  e-mails qui ne partaient jamais.

## 7. Performance (mesurée) et ce que j'ai corrigé

Mesure sur une base de test de **50 000 plaintes** fictives (`gpr_sicma_charge`, jamais la base réelle), sur un portable
i5-6300U à 100 % de charge avec le MariaDB de XAMPP réglé par défaut (cache de 16 Mo) : un environnement environ cinq
fois plus lent qu'un serveur. Premières mesures : résumé du Pilote **19 s**, liste **11 s**. Corrections apportées :
colonnes recopiées dans le compteur (plus de jointure avec la table des plaintes), un seul calcul groupé pour le résumé,
mémoire de 60 s (vidée à chaque changement), totaux des listes lus dans le résumé (plus de second comptage), recherche
par code en deux temps, lectures légères (colonnes utiles seulement), index retiré qui ralentissait les totaux.
Les résultats finaux sont dans le §9.

## 8. À surveiller en production (AlmaLinux)

- **Fuseau horaire du serveur** : toutes les dates sont locales au serveur. Il doit être réglé sur `Africa/Porto-Novo`.
- **Une seule instance** de GPR fait tourner la tâche de contrôle. Si plusieurs instances tournent, les alertes restent
  envoyées **une seule fois** (clé de déduplication unique en base), mais il vaut mieux n'en avoir qu'une.
- **Réglages e-mail et SMS** : sans eux, les notifications ne partent pas (le SLA le note dans les journaux et continue).
  C'est déjà le cas aujourd'hui sur la base de développement, qui n'a ni réglage e-mail ni réglage SMS.
- **MariaDB** : prévoir un cache InnoDB (`innodb_buffer_pool_size`) d'au moins 256 Mo.
- Le journal Hibernate (`show-sql`) du poste de développement a atteint plus de 400 Mo : à désactiver en production.

## 9. Résultats des mesures

Base de test de 50 000 plaintes et 50 000 compteurs, portable i5-6300U saturé, cache MariaDB de 16 Mo, **mesures à
froid** (mémoire du résumé vidée avant chaque mesure). « Avant » = première version, « Après » = version livrée.

| Opération | Avant | Après |
|---|---|---|
| Résumé du Pilote (cartes, badge) | 19 s | **1,6 s** |
| Résumé d'un RA / d'un agent | 10 à 17 s | **0,7 à 1 s** |
| Badge du menu | 2,4 s | **0,8 à 1,6 s** (puis lu en mémoire 60 s) |
| Liste « en retard », page 1 | 11,7 s | **3,2 s** |
| Liste, page 100 | 8,1 s | **6,3 s** |
| Recherche par code | 13,5 s | **7 s** |
| Page Alertes (toutes les réclamations en retard) | 2,6 s | 2,6 s |
| Contrôle : trouver les compteurs à regarder | 15 ms | **13 ms** |
| Rattrapage des plaintes sans compteur (toutes les 15 min) | 1,3 s | 2,9 s |
| Traitement d'un compteur (rappel, alerte, remontée) | | environ 0,4 s |

Lecture : sur un serveur normal, ces chiffres seraient plusieurs fois plus petits. Le coût réel d'un écran est de
quelques secondes sur cette machine, une seule lecture groupée, et le badge ne relit pas la base à chaque fois.
**Limite connue** : le rapport sur une très longue période est lent ; il est donc limité à **400 jours**.
Le traitement des compteurs (rappels et remontées) est lent par dossier (chargement complet de la plainte) mais
n'est fait que pour les compteurs arrivés à échéance (quelques dizaines par passage en régime normal).
