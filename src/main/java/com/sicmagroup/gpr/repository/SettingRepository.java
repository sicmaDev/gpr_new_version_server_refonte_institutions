package com.sicmagroup.gpr.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sicmagroup.gpr.domain.model.Setting;

public interface SettingRepository extends JpaRepository<Setting, Long> {
    Optional<Setting> findByLibelle(String libelle);

    /** Paramètres d'une famille (ex. « sla. ») : évite de relire (et déchiffrer) tous les réglages. */
    java.util.List<Setting> findByLibelleStartingWith(String prefix);
}
