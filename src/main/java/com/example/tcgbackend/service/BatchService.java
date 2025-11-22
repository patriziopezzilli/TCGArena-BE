package com.example.tcgbackend.service;

import com.example.tcgbackend.model.TCGType;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BatchService {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private Job importCardsJob;

    public void triggerBatchImport(TCGType tcgType, int startIndex, int endIndex) throws Exception {
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .addString("tcgType", tcgType.name())
                .addLong("startIndex", (long) startIndex)
                .addLong("endIndex", (long) endIndex)
                .toJobParameters();

        jobLauncher.run(importCardsJob, jobParameters);
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