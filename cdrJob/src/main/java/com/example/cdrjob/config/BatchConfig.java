package com.example.cdrjob.config;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.support.DatabaseType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
@EnableBatchProcessing
public class BatchConfig {

    private final PlatformTransactionManager transactionManager;

    public BatchConfig(@Qualifier("buildTransactionManager") PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    @Bean
    public JobRepository jobRepository(@Qualifier("buildDataSource") DataSource dataSource){
        JobRepositoryFactoryBean factory = new JobRepositoryFactoryBean();
        factory.setDataSource(dataSource);
        factory.setTransactionManager(transactionManager);
        factory.setDatabaseType(DatabaseType.MYSQL.getProductName());
        try {
            factory.afterPropertiesSet();
        } catch (Exception e) {
            // 处理异常或重新抛出运行时异常
            throw new RuntimeException("Failed to create JobRepository", e);
        }
        try {
            return factory.getObject();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Bean
    public Job ruleExecutionJob(Step ruleExecutionStep, JobRepository jobRepository ) {
        return new JobBuilder("ruleExecutionJob", jobRepository)
                .start(ruleExecutionStep)
                .build();
    }

    @Bean
    public Step ruleExecutionStep(Tasklet tasklet,  JobRepository jobRepository) {
        return new StepBuilder("ruleExecutionStep", jobRepository)
                .tasklet(tasklet, transactionManager)
                .build();
    }
}