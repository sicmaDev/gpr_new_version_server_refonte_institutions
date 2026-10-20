package com.sicmagroup.gpr;

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

	// Fixe le fuseau horaire par défaut de la JVM avant tout démarrage du contexte Spring, quel que soit le mode de
	// lancement (IDE, mvnw, Docker). L'URL JDBC déclare déjà serverTimezone=Africa/Porto-Novo : on aligne
	// LocalDateTime.now() (relances planifiées, suivi des délais) sur le même fuseau.
	static {
		TimeZone.setDefault(TimeZone.getTimeZone("Africa/Porto-Novo"));
	}

	public static void main(String[] args) {
		SpringApplication.run(GprApplication.class, args);
	}

}
