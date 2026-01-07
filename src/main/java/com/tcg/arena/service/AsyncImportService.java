package com.tcg.arena.service;

import com.tcg.arena.model.ImportJob;
import com.tcg.arena.model.ImportProgress;
import com.tcg.arena.model.JobStatus;
import com.tcg.arena.model.TCGType;
import com.tcg.arena.repository.ImportProgressRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AsyncImportService {

    private static final Logger logger = LoggerFactory.getLogger(AsyncImportService.class);

    private final Map<String, ImportJob> jobs = new ConcurrentHashMap<>();

    @Autowired
    private BatchService batchService;

    @Autowired
    private ImportProgressRepository importProgressRepository;

    /**
     * Starts an async import job for TCG API
     */
    public ImportJob triggerTCGImportAsync(TCGType tcgType) {
        // Create job record
        ImportJob job = new ImportJob(tcgType);
        jobs.put(job.getId(), job);

        // Execute asynchronously
        runTCGImport(job);

        return job;
    }

    @Async
    protected void runTCGImport(ImportJob job) {
        job.setStatus(JobStatus.RUNNING);
        job.setMessage("Starting import via TCG API...");

        try {
            logger.info("Starting Async TCG Import for Job ID: {}", job.getId());
            batchService.triggerTCGImport(job.getTcgType());

            job.setStatus(JobStatus.COMPLETED);
            job.setMessage("Import completed successfully!");
            job.setProcessedItems(job.getTotalItems()); // Ensure 100% at end
            job.setProgressPercent(100);

        } catch (Exception e) {
            logger.error("Async JustTCG Import failed for Job ID: {}", job.getId(), e);
            job.setStatus(JobStatus.FAILED);
            job.setMessage("Import failed: " + e.getMessage());
        }
    }

    /**
     * Retrieves current job status, syncing with DB progress if running
     */
    public ImportJob getJobStatus(String jobId) {
        ImportJob job = jobs.get(jobId);
        if (job == null) {
            return null;
        }

        // If running, poll DB for real-time progress
        if (job.getStatus() == JobStatus.RUNNING) {
            Optional<ImportProgress> progressOpt = importProgressRepository.findByTcgType(job.getTcgType());

            progressOpt.ifPresent(progress -> {
                // Map ImportProgress to ImportJob fields
                // Note: ImportProgress tracks pages/offsets, not necessarily a clean 0-100%
                // unless we know total
                if (progress.getTotalPagesKnown() != null && progress.getTotalPagesKnown() > 0) {
                    job.setTotalItems(progress.getTotalPagesKnown());
                    job.setProcessedItems(progress.getLastProcessedPage());

                    // message update
                    job.setMessage("Processing page " + progress.getLastProcessedPage() + " of "
                            + progress.getTotalPagesKnown());
                } else {
                    // fallback if total unknown
                    job.setProcessedItems(progress.getLastProcessedPage());
                    job.setMessage("Processing page " + progress.getLastProcessedPage() + "...");
                }
            });
        }

        return job;
    }
}
