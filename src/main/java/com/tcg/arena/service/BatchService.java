package com.tcg.arena.service;

import com.tcg.arena.model.TCGType;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class BatchService {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    @Qualifier("tcgImportJob")
    private Job tcgImportJob;

    /**
     * Trigger TCG API import for a specific TCG type
     */
    public void triggerTCGImport(TCGType tcgType) throws Exception {
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .addString("tcgType", tcgType.name())
                .toJobParameters();

        jobLauncher.run(tcgImportJob, jobParameters);
    }
}