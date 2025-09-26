package com.sicmagroup.gpr;

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

	public static void main(String[] args) {
		SpringApplication.run(GprApplication.class, args);
	}

}
