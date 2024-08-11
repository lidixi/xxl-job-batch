package com.example.cdrjob.controller;

import com.example.cdrjob.service.RuleService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/rules")
public class RuleController {

    private final RuleService ruleService;

    public RuleController(RuleService ruleService) {
        this.ruleService = ruleService;
    }

    @GetMapping("/databases")
    public List<String> getDatabases() {
        return ruleService.getDatabases();
    }

    @GetMapping("/tables")
    public List<String> getTables(@RequestParam String database) {
        return ruleService.getTables(database);
    }

    @GetMapping("/columns")
    public List<String> getColumns(@RequestParam String database, @RequestParam String table) {
        return ruleService.getColumns(database, table);
    }

    @PostMapping("/create")
    public String createRule(@RequestBody Map<String, String> ruleDetails) {
        String group = ruleDetails.get("group");
        String ruleName = ruleDetails.get("ruleName");
        String database = ruleDetails.get("database");
        String table = ruleDetails.get("table");
        String field = ruleDetails.get("field");
        String hospital = ruleDetails.get("hospital");
        String startDate = ruleDetails.get("startDate");
        String endDate = ruleDetails.get("endDate");
        String minValue = ruleDetails.get("minValue");
        String maxValue = ruleDetails.get("maxValue");
        String field1 = ruleDetails.get("field1");
        String field2 = ruleDetails.get("field2");

        ruleService.createSpecificRule(group, ruleName, database, table, field, hospital, startDate, endDate, minValue, maxValue, field1, field2);

        return "Rule created successfully";
    }

    @GetMapping("/sql-templates")
    public Map<String, String> getRuleSQLTemplates(@RequestParam Long ruleId) {
        return ruleService.getRuleSQLTemplates(ruleId);
    }
}