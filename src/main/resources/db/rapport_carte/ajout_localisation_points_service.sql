-- Cartographie du rapport Global : localisation des points de service.
-- Ces colonnes sont aussi créées automatiquement au démarrage (ddl-auto=update, entité ServicePointLocalisation).
-- Script fourni pour les déploiements qui préfèrent appliquer la migration à la main. Rejouable sans risque (MariaDB).
ALTER TABLE gps_service_point
  ADD COLUMN IF NOT EXISTS ville VARCHAR(255) NULL,
  ADD COLUMN IF NOT EXISTS quartier VARCHAR(255) NULL,
  ADD COLUMN IF NOT EXISTS latitude DOUBLE NULL,
  ADD COLUMN IF NOT EXISTS longitude DOUBLE NULL;
