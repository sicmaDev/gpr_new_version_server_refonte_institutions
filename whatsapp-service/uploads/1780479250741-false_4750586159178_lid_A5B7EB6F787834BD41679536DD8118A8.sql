-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Hôte : 127.0.0.1
-- Généré le : jeu. 23 oct. 2025 à 17:59
-- Version du serveur : 10.4.32-MariaDB
-- Version de PHP : 8.2.12

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Base de données : `assilassime_gps`
--

-- --------------------------------------------------------

--
-- Structure de la table `gps_categorie_objet`
--

CREATE TABLE `gps_categorie_objet` (
  `id` bigint(20) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `description` text DEFAULT NULL,
  `libelle` varchar(255) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `gps_chat`
--

CREATE TABLE `gps_chat` (
  `id` bigint(20) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `status` enum('CLOSED','OPEN') DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `created_by_id` bigint(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `gps_chat_vote`
--

CREATE TABLE `gps_chat_vote` (
  `chat_id` bigint(20) NOT NULL,
  `vote_id` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `gps_claim`
--

CREATE TABLE `gps_claim` (
  `id` bigint(20) NOT NULL,
  `address` varchar(255) DEFAULT NULL,
  `affected_anonymous` bit(1) DEFAULT NULL,
  `affected_at` datetime(6) DEFAULT NULL,
  `client_first_and_last_name` varchar(255) DEFAULT NULL,
  `code` varchar(255) DEFAULT NULL,
  `content` text DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `crew` varchar(255) DEFAULT NULL,
  `folder_code` varchar(255) DEFAULT NULL,
  `gender` enum('FEMME','HOMME','NON_DEFINI') DEFAULT NULL,
  `is_transmitted` bit(1) NOT NULL,
  `online_upload_date_time` datetime(6) DEFAULT NULL,
  `receipt_date_time` datetime(6) DEFAULT NULL,
  `status` enum('AFFECTED','CLASSED','DESAPPROUVED','LITIGATION','PARTIAL_SATISFIED','SATISFIED','SAVED','TEMP_SAVED','TO_APPROUVED','TRANSMITTED','TREAT','UNSATISFIED') DEFAULT NULL,
  `tel` varchar(255) DEFAULT NULL,
  `type` enum('CLAIM','DENUNCIACION','SUGGESTION') DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `classed_by_id` bigint(20) DEFAULT NULL,
  `collection_channel_id` bigint(20) DEFAULT NULL,
  `collector_id` bigint(20) DEFAULT NULL,
  `language_id` bigint(20) DEFAULT NULL,
  `objet_id` bigint(20) DEFAULT NULL,
  `product_id` bigint(20) DEFAULT NULL,
  `service_point_id` bigint(20) DEFAULT NULL,
  `session_id` bigint(20) DEFAULT NULL,
  `treat_by_id` bigint(20) DEFAULT NULL,
  `treatment_affected_by_id` bigint(20) DEFAULT NULL,
  `treatment_affected_to_id` bigint(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `gps_claim_audio`
--

CREATE TABLE `gps_claim_audio` (
  `id` bigint(20) NOT NULL,
  `name` text DEFAULT NULL,
  `path` text DEFAULT NULL,
  `size` bigint(20) DEFAULT NULL,
  `claim_id` bigint(20) DEFAULT NULL,
  `suggestion_id` bigint(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `gps_claim_audios`
--

CREATE TABLE `gps_claim_audios` (
  `claim_id` bigint(20) NOT NULL,
  `audios_id` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `gps_claim_external_recourses`
--

CREATE TABLE `gps_claim_external_recourses` (
  `claim_id` bigint(20) NOT NULL,
  `external_recourses_id` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `gps_claim_medias`
--

CREATE TABLE `gps_claim_medias` (
  `claim_id` bigint(20) NOT NULL,
  `medias_id` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `gps_claim_solutions`
--

CREATE TABLE `gps_claim_solutions` (
  `claim_id` bigint(20) NOT NULL,
  `solutions_id` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `gps_collection_channel`
--

CREATE TABLE `gps_collection_channel` (
  `id` bigint(20) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `description` text DEFAULT NULL,
  `is_deleted` tinyint(1) DEFAULT 0,
  `libelle` varchar(255) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `gps_documentation`
--

CREATE TABLE `gps_documentation` (
  `id` bigint(20) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `libelle` text DEFAULT NULL,
  `name` text DEFAULT NULL,
  `path` text DEFAULT NULL,
  `size` bigint(20) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id` bigint(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `gps_existing_solution`
--

CREATE TABLE `gps_existing_solution` (
  `id` bigint(20) NOT NULL,
  `compteur` bigint(20) DEFAULT NULL,
  `content` text DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `objet_id` bigint(20) DEFAULT NULL,
  `solution_id` bigint(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `gps_external_recourse`
--

CREATE TABLE `gps_external_recourse` (
  `id` bigint(20) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `description` text DEFAULT NULL,
  `is_deleted` tinyint(1) DEFAULT 0,
  `libelle` varchar(255) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `gps_external_recourse_claims`
--

CREATE TABLE `gps_external_recourse_claims` (
  `external_recourse_id` bigint(20) NOT NULL,
  `claims_id` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `gps_faq`
--

CREATE TABLE `gps_faq` (
  `id` bigint(20) NOT NULL,
  `answer` text DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `libelle` text DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `gps_language`
--

CREATE TABLE `gps_language` (
  `id` bigint(20) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `description` text DEFAULT NULL,
  `is_deleted` tinyint(1) DEFAULT 0,
  `libelle` varchar(255) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `gps_log`
--

CREATE TABLE `gps_log` (
  `id` bigint(20) NOT NULL,
  `content` text DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `libelle` varchar(255) DEFAULT NULL,
  `target` enum('APP','CLAIM','CONFIG','DENUNCIACION','SUGGESTION') DEFAULT NULL,
  `type` enum('ERROR','INFO','WARN') DEFAULT NULL,
  `user_id` bigint(20) DEFAULT NULL,
  `user_ip_address` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `gps_media`
--

CREATE TABLE `gps_media` (
  `id` bigint(20) NOT NULL,
  `name` text DEFAULT NULL,
  `path` text DEFAULT NULL,
  `size` bigint(20) DEFAULT NULL,
  `claim_id` bigint(20) DEFAULT NULL,
  `suggestion_id` bigint(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `gps_message`
--

CREATE TABLE `gps_message` (
  `id` bigint(20) NOT NULL,
  `content` text DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `is_vote` bit(1) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `chat_id` bigint(20) DEFAULT NULL,
  `sender_id` bigint(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `gps_objet`
--

CREATE TABLE `gps_objet` (
  `id` bigint(20) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `description` text DEFAULT NULL,
  `is_deleted` tinyint(1) DEFAULT 0,
  `libelle` varchar(255) DEFAULT NULL,
  `processing_time` int(11) NOT NULL,
  `risque_level` enum('GRAVE','MINEUR','MOYEN') DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `categorie_id` bigint(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `gps_poste`
--

CREATE TABLE `gps_poste` (
  `id` bigint(20) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `description` text DEFAULT NULL,
  `habilitations` varchar(255) DEFAULT NULL,
  `is_deleted` tinyint(1) DEFAULT 0,
  `libelle` varchar(255) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `gps_product`
--

CREATE TABLE `gps_product` (
  `id` bigint(20) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `description` text DEFAULT NULL,
  `is_deleted` tinyint(1) DEFAULT 0,
  `libelle` varchar(255) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `gps_satisfaction_measure`
--

CREATE TABLE `gps_satisfaction_measure` (
  `id` bigint(20) NOT NULL,
  `commentaire` text DEFAULT NULL,
  `measure_date_time` datetime(6) DEFAULT NULL,
  `status` enum('PARTIAL','SATISFIED','UNSATISFIED') DEFAULT NULL,
  `measurer_id` bigint(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `gps_service_point`
--

CREATE TABLE `gps_service_point` (
  `id` bigint(20) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `description` text DEFAULT NULL,
  `is_deleted` tinyint(1) DEFAULT 0,
  `is_principal_agence` tinyint(1) DEFAULT 0,
  `libelle` varchar(255) DEFAULT NULL,
  `type` enum('AGENCE','DIRECTION','GUICHET') DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `uuid` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `gps_solution`
--

CREATE TABLE `gps_solution` (
  `id` bigint(20) NOT NULL,
  `approuved_at` datetime(6) DEFAULT NULL,
  `commentaire` text DEFAULT NULL,
  `content` text DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `motif_desaprobation` text DEFAULT NULL,
  `status` enum('APPROVED','UNAPPROVED') DEFAULT NULL,
  `un_approuved_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `approuver_id` bigint(20) DEFAULT NULL,
  `author_id` bigint(20) DEFAULT NULL,
  `claim_id` bigint(20) DEFAULT NULL,
  `measure_id` bigint(20) DEFAULT NULL,
  `un_approuver_id` bigint(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `gps_suggestion`
--

CREATE TABLE `gps_suggestion` (
  `id` bigint(20) NOT NULL,
  `accepted` bit(1) NOT NULL,
  `address` varchar(255) DEFAULT NULL,
  `client_first_and_last_name` varchar(255) DEFAULT NULL,
  `code` varchar(255) DEFAULT NULL,
  `commentaire` text DEFAULT NULL,
  `content` text DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `crew` varchar(255) DEFAULT NULL,
  `folder_code` varchar(255) DEFAULT NULL,
  `gender` enum('FEMME','HOMME','NON_DEFINI') DEFAULT NULL,
  `online_upload_date_time` datetime(6) DEFAULT NULL,
  `receipt_date_time` datetime(6) DEFAULT NULL,
  `status` enum('AFFECTED','CLASSED','DESAPPROUVED','LITIGATION','PARTIAL_SATISFIED','SATISFIED','SAVED','TEMP_SAVED','TO_APPROUVED','TRANSMITTED','TREAT','UNSATISFIED') DEFAULT NULL,
  `tel` varchar(255) DEFAULT NULL,
  `treat_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `canal_id` bigint(20) DEFAULT NULL,
  `collecteur_id` bigint(20) DEFAULT NULL,
  `langue_id` bigint(20) DEFAULT NULL,
  `produit_id` bigint(20) DEFAULT NULL,
  `service_indexe_id` bigint(20) DEFAULT NULL,
  `traiteur_id` bigint(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `gps_suggestion_files`
--

CREATE TABLE `gps_suggestion_files` (
  `suggestion_id` bigint(20) NOT NULL,
  `files_id` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `gps_user`
--

CREATE TABLE `gps_user` (
  `id` bigint(20) NOT NULL,
  `additionalrole` enum('DE','MEMBRE_CA','MEMBRE_CGR','MOLDUE','PILOTE','PR_CGR') DEFAULT NULL,
  `code` varchar(255) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `firstandlastname` varchar(255) DEFAULT NULL,
  `habilitation_up` varchar(255) DEFAULT NULL,
  `is_coodonateur` bit(1) NOT NULL,
  `is_deleted` tinyint(1) DEFAULT 0,
  `is_email_receiver` bit(1) NOT NULL,
  `is_ra` bit(1) NOT NULL,
  `password` varchar(255) DEFAULT NULL,
  `tel` varchar(255) DEFAULT NULL,
  `titre` varchar(255) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `poste_id` bigint(20) DEFAULT NULL,
  `service_point_id` bigint(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `gps_user_chats_guest`
--

CREATE TABLE `gps_user_chats_guest` (
  `guests_id` bigint(20) NOT NULL,
  `chats_guest_id` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `gps_user_chats_member`
--

CREATE TABLE `gps_user_chats_member` (
  `members_id` bigint(20) NOT NULL,
  `chats_member_id` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `gps_user_messages`
--

CREATE TABLE `gps_user_messages` (
  `user_id` bigint(20) NOT NULL,
  `messages_id` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `gps_user_vote`
--

CREATE TABLE `gps_user_vote` (
  `id` bigint(20) NOT NULL,
  `vote_type` enum('CONTRE','POUR') DEFAULT NULL,
  `user_id` bigint(20) DEFAULT NULL,
  `vote_id` bigint(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `gps_vote`
--

CREATE TABLE `gps_vote` (
  `id` bigint(20) NOT NULL,
  `commentaire` text DEFAULT NULL,
  `contenu` text DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `is_choosed` bit(1) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `author_id` bigint(20) DEFAULT NULL,
  `chat_id` bigint(20) DEFAULT NULL,
  `message_id` bigint(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Index pour les tables déchargées
--

--
-- Index pour la table `gps_categorie_objet`
--
ALTER TABLE `gps_categorie_objet`
  ADD PRIMARY KEY (`id`);

--
-- Index pour la table `gps_chat`
--
ALTER TABLE `gps_chat`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FKfl3rctgqgnm79dl4s6u576gqm` (`created_by_id`);

--
-- Index pour la table `gps_chat_vote`
--
ALTER TABLE `gps_chat_vote`
  ADD UNIQUE KEY `UK_sq9n04oshpt3q54aa4ucswvsa` (`vote_id`),
  ADD KEY `FKpogbpsm35hbi504d3b62lbjxr` (`chat_id`);

--
-- Index pour la table `gps_claim`
--
ALTER TABLE `gps_claim`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UK_kb9fab3g3y1gw9uytebsmoi8` (`code`),
  ADD UNIQUE KEY `UK_jumlfiqif0kj1b3m14d644se5` (`session_id`),
  ADD KEY `FKn93xcp7kwa20sly5r1eqkkjqx` (`classed_by_id`),
  ADD KEY `FK6ctopjvnp8h7ry9cthclvvdij` (`collection_channel_id`),
  ADD KEY `FK3arrm1ma0yx26mr3gv5qwe9u7` (`collector_id`),
  ADD KEY `FKs2n82eem5rqmb9a7cxfoprn1v` (`language_id`),
  ADD KEY `FK6apeyfseuf1cia6t13e3hlajf` (`objet_id`),
  ADD KEY `FKm1hynwtsi96vnq4iyb015r4do` (`product_id`),
  ADD KEY `FKqv5ianqvift740gs465vryup1` (`service_point_id`),
  ADD KEY `FKnlgv4wtajumyiwmfmccaj1dpw` (`treat_by_id`),
  ADD KEY `FKr7vaovlrph253156o9mjhmtfo` (`treatment_affected_by_id`),
  ADD KEY `FKm872a5h101pgtd1i26hkibw80` (`treatment_affected_to_id`);

--
-- Index pour la table `gps_claim_audio`
--
ALTER TABLE `gps_claim_audio`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FK9mcly4ofmui7r732rvs0mj0s4` (`claim_id`),
  ADD KEY `FK984trdbrl46m5x8ajmc4puwgd` (`suggestion_id`);

--
-- Index pour la table `gps_claim_audios`
--
ALTER TABLE `gps_claim_audios`
  ADD UNIQUE KEY `UK_tkb632ptww29on4lk6wf170uw` (`audios_id`),
  ADD KEY `FKgwfsposx9uele7l6kv206qi8a` (`claim_id`);

--
-- Index pour la table `gps_claim_external_recourses`
--
ALTER TABLE `gps_claim_external_recourses`
  ADD KEY `FK28h72gumci5gk214dt79y4gp0` (`external_recourses_id`),
  ADD KEY `FK5vq03iguj6fnjbf6adsostgk9` (`claim_id`);

--
-- Index pour la table `gps_claim_medias`
--
ALTER TABLE `gps_claim_medias`
  ADD UNIQUE KEY `UK_jii3vfg10n1add81beg4sx5ed` (`medias_id`),
  ADD KEY `FK198vilyml9qqocehp41rti1de` (`claim_id`);

--
-- Index pour la table `gps_claim_solutions`
--
ALTER TABLE `gps_claim_solutions`
  ADD UNIQUE KEY `UK_4xxwu93x4nwu1kebwcvlp84kc` (`solutions_id`),
  ADD KEY `FKt4buuayfs4y0mwah0w2wf17po` (`claim_id`);

--
-- Index pour la table `gps_collection_channel`
--
ALTER TABLE `gps_collection_channel`
  ADD PRIMARY KEY (`id`);

--
-- Index pour la table `gps_documentation`
--
ALTER TABLE `gps_documentation`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FKit3nsxjlotc077j43xqp06qwj` (`user_id`);

--
-- Index pour la table `gps_existing_solution`
--
ALTER TABLE `gps_existing_solution`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UK_eaeep376x5n3w175ox541qbls` (`solution_id`),
  ADD KEY `FKmvhfw8qlkd6rbj1ka83wbdc62` (`objet_id`);

--
-- Index pour la table `gps_external_recourse`
--
ALTER TABLE `gps_external_recourse`
  ADD PRIMARY KEY (`id`);

--
-- Index pour la table `gps_external_recourse_claims`
--
ALTER TABLE `gps_external_recourse_claims`
  ADD KEY `FKe85cmtb9qdgiudxs5hxtyqbqe` (`claims_id`),
  ADD KEY `FKsxeox5k0mfqop44b0fxehx1uw` (`external_recourse_id`);

--
-- Index pour la table `gps_faq`
--
ALTER TABLE `gps_faq`
  ADD PRIMARY KEY (`id`);

--
-- Index pour la table `gps_language`
--
ALTER TABLE `gps_language`
  ADD PRIMARY KEY (`id`);

--
-- Index pour la table `gps_log`
--
ALTER TABLE `gps_log`
  ADD PRIMARY KEY (`id`);

--
-- Index pour la table `gps_media`
--
ALTER TABLE `gps_media`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FK470qiaesjdulqxqrwtl8643cc` (`claim_id`),
  ADD KEY `FKi3wxt1kbsp5s4lk3jtmm1xtcy` (`suggestion_id`);

--
-- Index pour la table `gps_message`
--
ALTER TABLE `gps_message`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FK6os8kti9iqo2go967pk9iqbud` (`chat_id`),
  ADD KEY `FK3siwh1wiqvw3x0puv9tj0sj0q` (`sender_id`);

--
-- Index pour la table `gps_objet`
--
ALTER TABLE `gps_objet`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FKankr29932ojetjr5k8acntfkd` (`categorie_id`);

--
-- Index pour la table `gps_poste`
--
ALTER TABLE `gps_poste`
  ADD PRIMARY KEY (`id`);

--
-- Index pour la table `gps_product`
--
ALTER TABLE `gps_product`
  ADD PRIMARY KEY (`id`);

--
-- Index pour la table `gps_satisfaction_measure`
--
ALTER TABLE `gps_satisfaction_measure`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FK6xdsau1sr3ctqqrb7f9xbvjbt` (`measurer_id`);

--
-- Index pour la table `gps_service_point`
--
ALTER TABLE `gps_service_point`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UK_9rj2ofravlsgju57wde8u7fll` (`uuid`);

--
-- Index pour la table `gps_solution`
--
ALTER TABLE `gps_solution`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UK_pquwqhiwp8w97g5c478qlw6sa` (`measure_id`),
  ADD KEY `FKsfqw6m8rqg1pwbkdef8f4wphf` (`approuver_id`),
  ADD KEY `FK6hvabblext68psgh0v0qugujf` (`author_id`),
  ADD KEY `FK72pmpnnul8g9apcm9m18u6fdy` (`claim_id`),
  ADD KEY `FK8pp9pgxa5es2uym3ubn1kdo7k` (`un_approuver_id`);

--
-- Index pour la table `gps_suggestion`
--
ALTER TABLE `gps_suggestion`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UK_pgvpqjvlup517wi3bc9n5j1mx` (`code`),
  ADD KEY `FKb7p1k7q9sb9rp9a4j4r7a4o2u` (`canal_id`),
  ADD KEY `FK5m8kx8elrvnibtoccoio66qy7` (`collecteur_id`),
  ADD KEY `FKth53o1py7dcmk2lgj6fv8eki7` (`langue_id`),
  ADD KEY `FKtjgd8itodt6x9jcog9cj3r6k0` (`produit_id`),
  ADD KEY `FKeu0qnwonqxivf5bw2oytitoqs` (`service_indexe_id`),
  ADD KEY `FKa4lu9bv1h5vao4lr1d1ies72` (`traiteur_id`);

--
-- Index pour la table `gps_suggestion_files`
--
ALTER TABLE `gps_suggestion_files`
  ADD UNIQUE KEY `UK_pov40j7odlug9f3ejg1tlmkmw` (`files_id`),
  ADD KEY `FKe8obqbk1y7b5utu5hu3jkwvpl` (`suggestion_id`);

--
-- Index pour la table `gps_user`
--
ALTER TABLE `gps_user`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UK_bymik0w5pgo5xavyjswpvvsf1` (`code`),
  ADD UNIQUE KEY `UK_onh7hqry8wo5xxbtr2phevgcf` (`email`),
  ADD UNIQUE KEY `UK_a102t0wjb7hra734j9d6xg772` (`tel`),
  ADD KEY `FKku6qve5caet4k6aadj539fgjq` (`poste_id`),
  ADD KEY `FKp223sikjwd0b1t79qgtdcwo5p` (`service_point_id`);

--
-- Index pour la table `gps_user_chats_guest`
--
ALTER TABLE `gps_user_chats_guest`
  ADD KEY `FKcd0co7bc7vc08gl12fxr88cmh` (`chats_guest_id`),
  ADD KEY `FK9g4ypads8k89jdrdqocq1b6l7` (`guests_id`);

--
-- Index pour la table `gps_user_chats_member`
--
ALTER TABLE `gps_user_chats_member`
  ADD KEY `FKj9ej7k3cxxfwa14623p0ut5d` (`chats_member_id`),
  ADD KEY `FKkcuptpcqsr2xfhyba4rah2rad` (`members_id`);

--
-- Index pour la table `gps_user_messages`
--
ALTER TABLE `gps_user_messages`
  ADD UNIQUE KEY `UK_fu51mf3p044grr1xpjpfdsgc3` (`messages_id`),
  ADD KEY `FKt3xelmhallbsqd6mxjg7p3ut1` (`user_id`);

--
-- Index pour la table `gps_user_vote`
--
ALTER TABLE `gps_user_vote`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FKnn0sl18w5ybgk9r2on0n4i414` (`user_id`),
  ADD KEY `FKiaf685bbswxwc3n47qsplanch` (`vote_id`);

--
-- Index pour la table `gps_vote`
--
ALTER TABLE `gps_vote`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UK_2eyvu9rxhn6q7i8nxa0a94vbd` (`message_id`),
  ADD KEY `FK4yaodpu1ndstmhrlt6tf0ejy4` (`author_id`),
  ADD KEY `FK1oa731syvm55donuhvh2fxvb3` (`chat_id`);

--
-- AUTO_INCREMENT pour les tables déchargées
--

--
-- AUTO_INCREMENT pour la table `gps_categorie_objet`
--
ALTER TABLE `gps_categorie_objet`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `gps_chat`
--
ALTER TABLE `gps_chat`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `gps_claim`
--
ALTER TABLE `gps_claim`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `gps_claim_audio`
--
ALTER TABLE `gps_claim_audio`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `gps_collection_channel`
--
ALTER TABLE `gps_collection_channel`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `gps_documentation`
--
ALTER TABLE `gps_documentation`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `gps_existing_solution`
--
ALTER TABLE `gps_existing_solution`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `gps_external_recourse`
--
ALTER TABLE `gps_external_recourse`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `gps_faq`
--
ALTER TABLE `gps_faq`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `gps_language`
--
ALTER TABLE `gps_language`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `gps_log`
--
ALTER TABLE `gps_log`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `gps_media`
--
ALTER TABLE `gps_media`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `gps_message`
--
ALTER TABLE `gps_message`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `gps_objet`
--
ALTER TABLE `gps_objet`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `gps_poste`
--
ALTER TABLE `gps_poste`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `gps_product`
--
ALTER TABLE `gps_product`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `gps_satisfaction_measure`
--
ALTER TABLE `gps_satisfaction_measure`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `gps_service_point`
--
ALTER TABLE `gps_service_point`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `gps_solution`
--
ALTER TABLE `gps_solution`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `gps_suggestion`
--
ALTER TABLE `gps_suggestion`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `gps_user`
--
ALTER TABLE `gps_user`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `gps_user_vote`
--
ALTER TABLE `gps_user_vote`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `gps_vote`
--
ALTER TABLE `gps_vote`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- Contraintes pour les tables déchargées
--

--
-- Contraintes pour la table `gps_chat`
--
ALTER TABLE `gps_chat`
  ADD CONSTRAINT `FKfl3rctgqgnm79dl4s6u576gqm` FOREIGN KEY (`created_by_id`) REFERENCES `gps_user` (`id`);

--
-- Contraintes pour la table `gps_chat_vote`
--
ALTER TABLE `gps_chat_vote`
  ADD CONSTRAINT `FKiklxmwxur7c3u879icmp5fgs7` FOREIGN KEY (`vote_id`) REFERENCES `gps_vote` (`id`),
  ADD CONSTRAINT `FKpogbpsm35hbi504d3b62lbjxr` FOREIGN KEY (`chat_id`) REFERENCES `gps_chat` (`id`);

--
-- Contraintes pour la table `gps_claim`
--
ALTER TABLE `gps_claim`
  ADD CONSTRAINT `FK3arrm1ma0yx26mr3gv5qwe9u7` FOREIGN KEY (`collector_id`) REFERENCES `gps_user` (`id`),
  ADD CONSTRAINT `FK6apeyfseuf1cia6t13e3hlajf` FOREIGN KEY (`objet_id`) REFERENCES `gps_objet` (`id`),
  ADD CONSTRAINT `FK6ctopjvnp8h7ry9cthclvvdij` FOREIGN KEY (`collection_channel_id`) REFERENCES `gps_collection_channel` (`id`),
  ADD CONSTRAINT `FK8cqr68qw2jck3wk3m2ymyckgb` FOREIGN KEY (`session_id`) REFERENCES `gps_chat` (`id`),
  ADD CONSTRAINT `FKm1hynwtsi96vnq4iyb015r4do` FOREIGN KEY (`product_id`) REFERENCES `gps_product` (`id`),
  ADD CONSTRAINT `FKm872a5h101pgtd1i26hkibw80` FOREIGN KEY (`treatment_affected_to_id`) REFERENCES `gps_user` (`id`),
  ADD CONSTRAINT `FKn93xcp7kwa20sly5r1eqkkjqx` FOREIGN KEY (`classed_by_id`) REFERENCES `gps_user` (`id`),
  ADD CONSTRAINT `FKnlgv4wtajumyiwmfmccaj1dpw` FOREIGN KEY (`treat_by_id`) REFERENCES `gps_user` (`id`),
  ADD CONSTRAINT `FKqv5ianqvift740gs465vryup1` FOREIGN KEY (`service_point_id`) REFERENCES `gps_service_point` (`id`),
  ADD CONSTRAINT `FKr7vaovlrph253156o9mjhmtfo` FOREIGN KEY (`treatment_affected_by_id`) REFERENCES `gps_user` (`id`),
  ADD CONSTRAINT `FKs2n82eem5rqmb9a7cxfoprn1v` FOREIGN KEY (`language_id`) REFERENCES `gps_language` (`id`);

--
-- Contraintes pour la table `gps_claim_audio`
--
ALTER TABLE `gps_claim_audio`
  ADD CONSTRAINT `FK984trdbrl46m5x8ajmc4puwgd` FOREIGN KEY (`suggestion_id`) REFERENCES `gps_suggestion` (`id`),
  ADD CONSTRAINT `FK9mcly4ofmui7r732rvs0mj0s4` FOREIGN KEY (`claim_id`) REFERENCES `gps_claim` (`id`);

--
-- Contraintes pour la table `gps_claim_audios`
--
ALTER TABLE `gps_claim_audios`
  ADD CONSTRAINT `FKgwfsposx9uele7l6kv206qi8a` FOREIGN KEY (`claim_id`) REFERENCES `gps_claim` (`id`),
  ADD CONSTRAINT `FKncaiuevw1451v4jlv8qvwyr9` FOREIGN KEY (`audios_id`) REFERENCES `gps_claim_audio` (`id`);

--
-- Contraintes pour la table `gps_claim_external_recourses`
--
ALTER TABLE `gps_claim_external_recourses`
  ADD CONSTRAINT `FK28h72gumci5gk214dt79y4gp0` FOREIGN KEY (`external_recourses_id`) REFERENCES `gps_external_recourse` (`id`),
  ADD CONSTRAINT `FK5vq03iguj6fnjbf6adsostgk9` FOREIGN KEY (`claim_id`) REFERENCES `gps_claim` (`id`);

--
-- Contraintes pour la table `gps_claim_medias`
--
ALTER TABLE `gps_claim_medias`
  ADD CONSTRAINT `FK198vilyml9qqocehp41rti1de` FOREIGN KEY (`claim_id`) REFERENCES `gps_claim` (`id`),
  ADD CONSTRAINT `FKm3s1kg195d5t72a45oibgnwwm` FOREIGN KEY (`medias_id`) REFERENCES `gps_media` (`id`);

--
-- Contraintes pour la table `gps_claim_solutions`
--
ALTER TABLE `gps_claim_solutions`
  ADD CONSTRAINT `FKpidk0cfw331f5imfwh86r7mv1` FOREIGN KEY (`solutions_id`) REFERENCES `gps_solution` (`id`),
  ADD CONSTRAINT `FKt4buuayfs4y0mwah0w2wf17po` FOREIGN KEY (`claim_id`) REFERENCES `gps_claim` (`id`);

--
-- Contraintes pour la table `gps_documentation`
--
ALTER TABLE `gps_documentation`
  ADD CONSTRAINT `FKit3nsxjlotc077j43xqp06qwj` FOREIGN KEY (`user_id`) REFERENCES `gps_user` (`id`);

--
-- Contraintes pour la table `gps_existing_solution`
--
ALTER TABLE `gps_existing_solution`
  ADD CONSTRAINT `FKf4c9kloswh32ux6n846g90jjo` FOREIGN KEY (`solution_id`) REFERENCES `gps_solution` (`id`),
  ADD CONSTRAINT `FKmvhfw8qlkd6rbj1ka83wbdc62` FOREIGN KEY (`objet_id`) REFERENCES `gps_objet` (`id`);

--
-- Contraintes pour la table `gps_external_recourse_claims`
--
ALTER TABLE `gps_external_recourse_claims`
  ADD CONSTRAINT `FKe85cmtb9qdgiudxs5hxtyqbqe` FOREIGN KEY (`claims_id`) REFERENCES `gps_claim` (`id`),
  ADD CONSTRAINT `FKsxeox5k0mfqop44b0fxehx1uw` FOREIGN KEY (`external_recourse_id`) REFERENCES `gps_external_recourse` (`id`);

--
-- Contraintes pour la table `gps_media`
--
ALTER TABLE `gps_media`
  ADD CONSTRAINT `FK470qiaesjdulqxqrwtl8643cc` FOREIGN KEY (`claim_id`) REFERENCES `gps_claim` (`id`),
  ADD CONSTRAINT `FKi3wxt1kbsp5s4lk3jtmm1xtcy` FOREIGN KEY (`suggestion_id`) REFERENCES `gps_suggestion` (`id`);

--
-- Contraintes pour la table `gps_message`
--
ALTER TABLE `gps_message`
  ADD CONSTRAINT `FK3siwh1wiqvw3x0puv9tj0sj0q` FOREIGN KEY (`sender_id`) REFERENCES `gps_user` (`id`),
  ADD CONSTRAINT `FK6os8kti9iqo2go967pk9iqbud` FOREIGN KEY (`chat_id`) REFERENCES `gps_chat` (`id`);

--
-- Contraintes pour la table `gps_objet`
--
ALTER TABLE `gps_objet`
  ADD CONSTRAINT `FKankr29932ojetjr5k8acntfkd` FOREIGN KEY (`categorie_id`) REFERENCES `gps_categorie_objet` (`id`);

--
-- Contraintes pour la table `gps_satisfaction_measure`
--
ALTER TABLE `gps_satisfaction_measure`
  ADD CONSTRAINT `FK6xdsau1sr3ctqqrb7f9xbvjbt` FOREIGN KEY (`measurer_id`) REFERENCES `gps_user` (`id`);

--
-- Contraintes pour la table `gps_solution`
--
ALTER TABLE `gps_solution`
  ADD CONSTRAINT `FK6hvabblext68psgh0v0qugujf` FOREIGN KEY (`author_id`) REFERENCES `gps_user` (`id`),
  ADD CONSTRAINT `FK72pmpnnul8g9apcm9m18u6fdy` FOREIGN KEY (`claim_id`) REFERENCES `gps_claim` (`id`),
  ADD CONSTRAINT `FK7b0hpo34ko2uplmxdimuhf6x` FOREIGN KEY (`measure_id`) REFERENCES `gps_satisfaction_measure` (`id`),
  ADD CONSTRAINT `FK8pp9pgxa5es2uym3ubn1kdo7k` FOREIGN KEY (`un_approuver_id`) REFERENCES `gps_user` (`id`),
  ADD CONSTRAINT `FKsfqw6m8rqg1pwbkdef8f4wphf` FOREIGN KEY (`approuver_id`) REFERENCES `gps_user` (`id`);

--
-- Contraintes pour la table `gps_suggestion`
--
ALTER TABLE `gps_suggestion`
  ADD CONSTRAINT `FK5m8kx8elrvnibtoccoio66qy7` FOREIGN KEY (`collecteur_id`) REFERENCES `gps_user` (`id`),
  ADD CONSTRAINT `FKa4lu9bv1h5vao4lr1d1ies72` FOREIGN KEY (`traiteur_id`) REFERENCES `gps_user` (`id`),
  ADD CONSTRAINT `FKb7p1k7q9sb9rp9a4j4r7a4o2u` FOREIGN KEY (`canal_id`) REFERENCES `gps_collection_channel` (`id`),
  ADD CONSTRAINT `FKeu0qnwonqxivf5bw2oytitoqs` FOREIGN KEY (`service_indexe_id`) REFERENCES `gps_service_point` (`id`),
  ADD CONSTRAINT `FKth53o1py7dcmk2lgj6fv8eki7` FOREIGN KEY (`langue_id`) REFERENCES `gps_language` (`id`),
  ADD CONSTRAINT `FKtjgd8itodt6x9jcog9cj3r6k0` FOREIGN KEY (`produit_id`) REFERENCES `gps_product` (`id`);

--
-- Contraintes pour la table `gps_suggestion_files`
--
ALTER TABLE `gps_suggestion_files`
  ADD CONSTRAINT `FK9j3v7ayx306owfl0iwx7n30nt` FOREIGN KEY (`files_id`) REFERENCES `gps_media` (`id`),
  ADD CONSTRAINT `FKe8obqbk1y7b5utu5hu3jkwvpl` FOREIGN KEY (`suggestion_id`) REFERENCES `gps_suggestion` (`id`);

--
-- Contraintes pour la table `gps_user`
--
ALTER TABLE `gps_user`
  ADD CONSTRAINT `FKku6qve5caet4k6aadj539fgjq` FOREIGN KEY (`poste_id`) REFERENCES `gps_poste` (`id`),
  ADD CONSTRAINT `FKp223sikjwd0b1t79qgtdcwo5p` FOREIGN KEY (`service_point_id`) REFERENCES `gps_service_point` (`id`);

--
-- Contraintes pour la table `gps_user_chats_guest`
--
ALTER TABLE `gps_user_chats_guest`
  ADD CONSTRAINT `FK9g4ypads8k89jdrdqocq1b6l7` FOREIGN KEY (`guests_id`) REFERENCES `gps_user` (`id`),
  ADD CONSTRAINT `FKcd0co7bc7vc08gl12fxr88cmh` FOREIGN KEY (`chats_guest_id`) REFERENCES `gps_chat` (`id`);

--
-- Contraintes pour la table `gps_user_chats_member`
--
ALTER TABLE `gps_user_chats_member`
  ADD CONSTRAINT `FKj9ej7k3cxxfwa14623p0ut5d` FOREIGN KEY (`chats_member_id`) REFERENCES `gps_chat` (`id`),
  ADD CONSTRAINT `FKkcuptpcqsr2xfhyba4rah2rad` FOREIGN KEY (`members_id`) REFERENCES `gps_user` (`id`);

--
-- Contraintes pour la table `gps_user_messages`
--
ALTER TABLE `gps_user_messages`
  ADD CONSTRAINT `FK18qv1bshicb1fs8l4wd6sh9wf` FOREIGN KEY (`messages_id`) REFERENCES `gps_message` (`id`),
  ADD CONSTRAINT `FKt3xelmhallbsqd6mxjg7p3ut1` FOREIGN KEY (`user_id`) REFERENCES `gps_user` (`id`);

--
-- Contraintes pour la table `gps_user_vote`
--
ALTER TABLE `gps_user_vote`
  ADD CONSTRAINT `FKiaf685bbswxwc3n47qsplanch` FOREIGN KEY (`vote_id`) REFERENCES `gps_vote` (`id`),
  ADD CONSTRAINT `FKnn0sl18w5ybgk9r2on0n4i414` FOREIGN KEY (`user_id`) REFERENCES `gps_user` (`id`);

--
-- Contraintes pour la table `gps_vote`
--
ALTER TABLE `gps_vote`
  ADD CONSTRAINT `FK1oa731syvm55donuhvh2fxvb3` FOREIGN KEY (`chat_id`) REFERENCES `gps_chat` (`id`),
  ADD CONSTRAINT `FK4yaodpu1ndstmhrlt6tf0ejy4` FOREIGN KEY (`author_id`) REFERENCES `gps_user` (`id`),
  ADD CONSTRAINT `FKmoq8c4wjhfd25h6dxyhefns4n` FOREIGN KEY (`message_id`) REFERENCES `gps_message` (`id`);
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
