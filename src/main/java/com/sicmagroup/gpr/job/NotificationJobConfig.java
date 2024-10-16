package com.sicmagroup.gpr.job;

// import org.springframework.batch.core.Job;
// import org.springframework.batch.core.JobParametersBuilder;
// import org.springframework.batch.core.Step;
// import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
// import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;

// @Configuration
// public class NotificationJobConfig {

//     @Bean
//     public Job notificationJob(JobBuilderFactory jobBuilderFactory, Step notificationStep) {
//         return jobBuilderFactory.get("notificationJob")
//                 .start(notificationStep)  // Ce job exécute un seul Step qui est "notificationStep"
//                 .build();
//     }

//     @Bean
//     public Step notificationStep(StepBuilderFactory stepBuilderFactory, NotificationTasklet notificationTasklet) {
//         return stepBuilderFactory.get("notificationStep")
//                 .tasklet(notificationTasklet)  // Ce Step exécute la tâche d'envoi des notifications
//                 .build();
//     }
// }


import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableBatchProcessing
public class NotificationJobConfig {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final NotificationTasklet notificationTasklet;

    public NotificationJobConfig(JobBuilderFactory jobBuilderFactory,
                                  StepBuilderFactory stepBuilderFactory,
                                  NotificationTasklet notificationTasklet) {
        this.jobBuilderFactory = jobBuilderFactory;
        this.stepBuilderFactory = stepBuilderFactory;
        this.notificationTasklet = notificationTasklet;
    }

    @Bean
    public Job notificationJob() {
        return jobBuilderFactory.get("notificationJob")
                .start(notificationStep())
                .build();
    }

    @Bean
    public Step notificationStep() {
        return stepBuilderFactory.get("notificationStep")
                .tasklet(notificationTasklet)
                .build();
    }
}

