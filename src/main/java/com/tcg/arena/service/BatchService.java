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
    private Job importCardsJob;

    @Autowired
    @Qualifier("justTCGImportJob")
    private Job justTCGImportJob;

    public void triggerBatchImport(TCGType tcgType, int startIndex, int endIndex) throws Exception {
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .addString("tcgType", tcgType.name())
                .addLong("startIndex", (long) startIndex)
                .addLong("endIndex", (long) endIndex)
                .toJobParameters();

        jobLauncher.run(importCardsJob, jobParameters);
    }

    /**
     * Trigger JustTCG API import for a specific TCG type
     */
    public void triggerJustTCGImport(TCGType tcgType) throws Exception {
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .addString("tcgType", tcgType.name())
                .toJobParameters();

        jobLauncher.run(justTCGImportJob, jobParameters);
    }

    // Backward compatibility
    public void triggerBatchImport(TCGType tcgType, int startIndex) throws Exception {
        triggerBatchImport(tcgType, startIndex, -99);
    }

    // Backward compatibility
    public void triggerBatchImport(TCGType tcgType) throws Exception {
        triggerBatchImport(tcgType, -99, -99);
    }
}