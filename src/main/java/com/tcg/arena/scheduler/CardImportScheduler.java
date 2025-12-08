package com.tcg.arena.scheduler;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CardImportScheduler {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private Job importCardsJob;

    // DISABLED: Cron job disabled - run imports manually only
    // @Scheduled(cron = "0 0 2 * * ?") // Every night at 2 AM
    public void runImportJob() {
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();
            jobLauncher.run(importCardsJob, jobParameters);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}