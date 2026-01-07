package com.tcg.arena.batch;

import com.tcg.arena.model.TCGType;
import com.tcg.arena.service.ImportStatsCollector;
import com.tcg.arena.service.TCGApiClient;
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
 * Spring Batch configuration for TCG API import
 * Uses Tasklet approach for simple API-to-DB import
 */
@Configuration
public class TCGBatchConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(TCGBatchConfiguration.class);

    @Autowired
    private TCGApiClient tcgApiClient;
    
    @Autowired
    private ImportStatsCollector statsCollector;

    @Bean
    public Job tcgImportJob(JobRepository jobRepository,
            @Qualifier("tcgImportStep") Step tcgImportStep) {
        return new JobBuilder("tcgImportJob", jobRepository)
                .start(tcgImportStep)
                .build();
    }

    @Bean
    @Qualifier("tcgImportStep")
    public Step tcgImportStep(JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            @Qualifier("tcgImportTasklet") Tasklet tasklet) {
        return new StepBuilder("tcgImportStep", jobRepository)
                .tasklet(tasklet, transactionManager)
                .build();
    }

    @Bean
    @StepScope
    @Qualifier("tcgImportTasklet")
    public Tasklet tcgImportTasklet(
            @Value("#{jobParameters['tcgType']}") String tcgTypeParam) {

        return (contribution, chunkContext) -> {
            logger.info("Starting TCG import tasklet with tcgType: {}", tcgTypeParam);

            TCGType tcgType;
            try {
                tcgType = TCGType.valueOf(tcgTypeParam);
            } catch (Exception e) {
                logger.error("Invalid TCG type: {}", tcgTypeParam);
                return RepeatStatus.FINISHED;
            }

            if (!tcgApiClient.isTCGSupported(tcgType)) {
                logger.warn("TCG type {} is not supported by TCG API", tcgType);
                statsCollector.recordImportFailure(tcgType, "TCG not supported by TCG API");
                return RepeatStatus.FINISHED;
            }

            // Record import start
            statsCollector.recordImportStart(tcgType);

            try {
                logger.info("Starting reactive import for {}", tcgType.getDisplayName());
                Integer importedCount = tcgApiClient.importCardsForTCG(tcgType)
                        .timeout(java.time.Duration.ofHours(4)) // 4 hour timeout
                        .block();
                
                int imported = (importedCount != null) ? importedCount : 0;
                logger.info("TCG import completed. Imported {} cards for {}",
                        imported, tcgType.getDisplayName());
                
                // Record success
                statsCollector.recordImportSuccess(tcgType, imported, imported, 0);
            } catch (Exception e) {
                logger.error("Error during TCG import: {}", e.getMessage(), e);
                statsCollector.recordImportFailure(tcgType, e.getMessage());
            }

            return RepeatStatus.FINISHED;
        };
    }
}
