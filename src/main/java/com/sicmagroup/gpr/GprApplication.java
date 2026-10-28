package com.sicmagroup.gpr;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.sicmagroup.gpr.service.media.FileStorageProperties;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableConfigurationProperties({
	FileStorageProperties.class
})
public class GprApplication {

	/** Fuseau utilisé quand l'institution n'en précise pas (variable GPR_TIMEZONE ou propriété gpr.timezone). */
	public static final String DEFAULT_TIMEZONE = "Africa/Porto-Novo";

	// Fixe le fuseau horaire par défaut de la JVM avant tout démarrage du contexte Spring, quel que soit le mode de
	// lancement (IDE, mvnw, Docker). L'URL JDBC utilise le même réglage (serverTimezone=${GPR_TIMEZONE}) : on aligne
	// LocalDateTime.now() (relances planifiées, suivi des délais) sur le même fuseau.
	static {
		TimeZone.setDefault(TimeZone.getTimeZone(resolveTimezone()));
	}

	/** GPR_TIMEZONE (variable d'environnement), sinon gpr.timezone (-D), sinon Porto-Novo ; un identifiant inconnu est refusé. */
	public static String resolveTimezone() {
		String zone = System.getenv("GPR_TIMEZONE");
		if (zone == null || zone.isBlank()) zone = System.getProperty("gpr.timezone");
		if (zone == null || zone.isBlank()) return DEFAULT_TIMEZONE;
		try {
			return ZoneId.of(zone.trim()).getId();
		} catch (DateTimeException e) {
			throw new IllegalStateException("GPR_TIMEZONE invalide : « " + zone + " » (attendu : identifiant IANA, ex. Africa/Abidjan)", e);
		}
	}

	public static void main(String[] args) {
		SpringApplication.run(GprApplication.class, args);
	}

}
