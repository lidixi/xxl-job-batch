package com.example.cdrjob.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class DataSourceConfig {

    @Bean(name = "buildDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.build")
    public DataSource buildDataSource() {
        return org.springframework.boot.jdbc.DataSourceBuilder.create().build();
    }

    @Bean(name = "cdrBaseDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.cdr-base")
    public DataSource cdrBaseDataSource() {
        return org.springframework.boot.jdbc.DataSourceBuilder.create().build();
    }

    @Bean(name = "cdrAuditDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.cdr-audit")
    public DataSource cdrAuditDataSource() {
        return org.springframework.boot.jdbc.DataSourceBuilder.create().build();
    }

    @Bean(name = "logDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.log")
    public DataSource logDataSource() {
        return org.springframework.boot.jdbc.DataSourceBuilder.create().build();
    }

    @Bean
    public AbstractRoutingDataSource dataSource(
            @Qualifier("buildDataSource") DataSource buildDataSource,
            @Qualifier("cdrBaseDataSource") DataSource cdrBaseDataSource,
            @Qualifier("cdrAuditDataSource") DataSource cdrAuditDataSource,
            @Qualifier("logDataSource") DataSource logDataSource) {

        DynamicDataSource dataSource = new DynamicDataSource();
        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put("build", buildDataSource);
        targetDataSources.put("cdrBase", cdrBaseDataSource);
        targetDataSources.put("cdrAudit", cdrAuditDataSource);
        targetDataSources.put("log", logDataSource);
        dataSource.setTargetDataSources(targetDataSources);
        dataSource.setDefaultTargetDataSource(buildDataSource);
        return dataSource;
    }

    @Bean(name = "buildTransactionManager")
    public DataSourceTransactionManager buildTransactionManager(@Qualifier("buildDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean(name = "transactionManager")
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}