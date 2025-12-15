package com.sicmagroup.gpr.configuration;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);      // nombre de threads de base
        executor.setMaxPoolSize(10);      // nombre max de threads
        executor.setQueueCapacity(500);   // taille de la file d'attente
        executor.setThreadNamePrefix("Async-Mail-");
        executor.initialize();
        return executor;
    }
}

