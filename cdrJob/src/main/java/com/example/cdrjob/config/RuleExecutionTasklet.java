package com.example.cdrjob.config;

import com.example.cdrjob.service.RuleExecutionService;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class RuleExecutionTasklet implements Tasklet {

    private final RuleExecutionService ruleExecutionService;

    public RuleExecutionTasklet(RuleExecutionService ruleExecutionService) {
        this.ruleExecutionService = ruleExecutionService;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext)
    {
        Map<String, Object> params = chunkContext.getStepContext().getJobParameters();
        Long ruleId = Long.parseLong(params.get("ruleId").toString());

        ruleExecutionService.executeRule(ruleId, params);

        return RepeatStatus.FINISHED;
    }
}