package com.example.cdrjob.job;

import com.example.cdrjob.config.DataSourceContextHolder;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;

import java.util.HashMap;
import java.util.Map;

@Component
public class CDRJobHandler {

    private static Logger logger = LoggerFactory.getLogger(CDRJobHandler.class);

    private final JobLauncher jobLauncher;
    private final Job ruleExecutionJob;

    public CDRJobHandler(JobLauncher jobLauncher, Job ruleExecutionJob) {
        this.jobLauncher = jobLauncher;
        this.ruleExecutionJob = ruleExecutionJob;
    }

    @XxlJob("CDR")
    public void execute(String param) throws Exception {
        XxlJobHelper.log("CDRJobHandler executed with param: " + param);

        Map<String, Object> params = parseParam(param);

        // 解析任务参数，设置数据源
        if (params.containsKey("databaseType")) {
            String databaseType = params.get("databaseType").toString();
            DataSourceContextHolder.setDataSource(databaseType);
            logger.info("DataSource set to: {}", databaseType);
        }

        // 解析并处理多个 ruleId
        if (params.containsKey("ruleId")) {
            String ruleIdParam = params.get("ruleId").toString();
            String[] ruleIds = ruleIdParam.split(",");
            for (String ruleId : ruleIds) {
                JobParameters jobParameters = new JobParametersBuilder()
                        .addLong("ruleId", Long.parseLong(ruleId.trim()))
                        .toJobParameters();

                jobLauncher.run(ruleExecutionJob, jobParameters);
                logger.info("Job launched for ruleId: {}", ruleId);
            }
        }

        XxlJobHelper.log("CDRJobHandler executed with param: " + param);
        DataSourceContextHolder.clearDataSource();
    }

    private Map<String, Object> parseParam(String param) {
        Map<String, Object> params = new HashMap<>();
        String[] keyValuePairs = param.split(",");
        for (String pair : keyValuePairs) {
            String[] entry = pair.split("=");
            params.put(entry[0].trim(), entry[1].trim());
        }
        return params;
    }
}