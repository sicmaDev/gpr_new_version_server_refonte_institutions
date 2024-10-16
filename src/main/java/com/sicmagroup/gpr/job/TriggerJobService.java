package com.sicmagroup.gpr.job;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TriggerJobService {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private Job notificationJob;

    public void triggerNotificationJob(List<String> usersToContact, String message) {
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("usersToContact", String.join(",", usersToContact))
                .addString("message", message)
                .toJobParameters();

        try {
            jobLauncher.run(notificationJob, jobParameters);
        } catch (JobExecutionException e) {
            e.printStackTrace();
        }
    }
}

