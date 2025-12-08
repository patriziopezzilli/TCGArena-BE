package com.tcg.arena.batch;

import com.tcg.arena.model.TCGType;
import com.tcg.arena.service.JustTCGApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Spring Batch configuration for JustTCG API import
 * Uses Tasklet approach for simple API-to-DB import
 */
@Configuration
public class JustTCGBatchConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(JustTCGBatchConfiguration.class);

    @Autowired
    private JustTCGApiClient justTCGApiClient;

    @Bean
    public Job justTCGImportJob(JobRepository jobRepository,
            @Qualifier("justTCGImportStep") Step justTCGImportStep) {
        return new JobBuilder("justTCGImportJob", jobRepository)
                .start(justTCGImportStep)
                .build();
    }

    @Bean
    @Qualifier("justTCGImportStep")
    public Step justTCGImportStep(JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            @Qualifier("justTCGImportTasklet") Tasklet tasklet) {
        return new StepBuilder("justTCGImportStep", jobRepository)
                .tasklet(tasklet, transactionManager)
                .build();
    }

    @Bean
    @StepScope
    @Qualifier("justTCGImportTasklet")
    public Tasklet justTCGImportTasklet(
            @Value("#{jobParameters['tcgType']}") String tcgTypeParam) {

        return (contribution, chunkContext) -> {
            logger.info("Starting JustTCG import tasklet with tcgType: {}", tcgTypeParam);

            TCGType tcgType;
            try {
                tcgType = TCGType.valueOf(tcgTypeParam);
            } catch (Exception e) {
                logger.error("Invalid TCG type: {}", tcgTypeParam);
                return RepeatStatus.FINISHED;
            }

            if (!justTCGApiClient.isTCGSupported(tcgType)) {
                logger.warn("TCG type {} is not supported by JustTCG API", tcgType);
                return RepeatStatus.FINISHED;
            }

            try {
                Integer importedCount = justTCGApiClient.importCardsForTCG(tcgType).block();
                logger.info("JustTCG import completed. Imported {} cards for {}",
                        importedCount, tcgType.getDisplayName());
            } catch (Exception e) {
                logger.error("Error during JustTCG import: {}", e.getMessage(), e);
            }

            return RepeatStatus.FINISHED;
        };
    }
}
