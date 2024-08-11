package com.example.cdrjob.service;

import com.example.cdrjob.config.DataSourceContextHolder;
import com.example.cdrjob.dao.RuleDao;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class RuleService {

    private final RuleDao ruleDao;

    public RuleService(RuleDao ruleDao) {
        this.ruleDao = ruleDao;
    }

    public List<String> getDatabases() {
        DataSourceContextHolder.setDataSource("build");
        List<String> databases = ruleDao.getDatabases();
        DataSourceContextHolder.clearDataSource();
        return databases;
    }

    public List<String> getTables(String database) {
        DataSourceContextHolder.setDataSource("build");
        ruleDao.setDataSource(database);
        List<String> tables = ruleDao.getTables(database);
        DataSourceContextHolder.clearDataSource();
        return tables;
    }

    public List<String> getColumns(String database, String table) {
        DataSourceContextHolder.setDataSource("build");
        ruleDao.setDataSource(database);
        List<String> columns = ruleDao.getColumns(database, table);
        DataSourceContextHolder.clearDataSource();
        return columns;
    }

    public void createSpecificRule(String group, String ruleName, String database, String table, String field, String hospital, String startDate, String endDate, String minValue, String maxValue, String field1, String field2) {
        DataSourceContextHolder.setDataSource("build");
        Long groupId = ruleDao.getGroupIdByGroupName(group);
        // 获取 SQL 父模板
        String baseParent = ruleDao.getBaseParentSQL(String.valueOf(groupId));
        String auditParent = ruleDao.getAuditParentSQL(String.valueOf(groupId));
        String wrongParent = ruleDao.getWrongParentSQL(String.valueOf(groupId));

        // 生成SQL子模板
        String baseSon = generateSQL(baseParent, table, field, hospital, startDate, endDate, minValue, maxValue, field1, field2);
        String auditSon = generateSQL(auditParent, table, field, hospital, startDate, endDate, minValue, maxValue, field1, field2);
        String wrongSon = generateSQL(wrongParent, table, field, hospital, startDate, endDate, minValue, maxValue, field1, field2);

        // 持久化具体规则
        ruleDao.createRule(group, ruleName, baseSon, auditSon, wrongSon);
        DataSourceContextHolder.clearDataSource();
    }

    public Map<String, String> getRuleSQLTemplates(Long ruleId) {
        DataSourceContextHolder.setDataSource("build");
        DataSourceContextHolder.clearDataSource();
        return ruleDao.getRuleSQLTemplates(ruleId);
    }

    private String generateSQL(String sqlTemplate, String table, String field, String hospital, String startDate, String endDate, String minValue, String maxValue, String field1, String field2) {
        // 替换模板中的参数
        sqlTemplate = sqlTemplate.replace("{table}", table)
                .replace("{field}", field != null ? field : "")
                .replace("{hospital}", hospital != null ? " AND YQ_NAME IN (" + hospital + ")" : "")
                .replace("{startDate}", startDate != null ? " AND CREATE_DATE >= DATE'" + startDate + "'" : "")
                .replace("{endDate}", endDate != null ? " AND CREATE_DATE <= DATE'" + endDate + "'" : "")
                .replace("{minValue}", minValue != null ? minValue : "")
                .replace("{maxValue}", maxValue != null ? maxValue : "")
                .replace("{field1}", field1 != null ? field1 : "")
                .replace("{field2}", field2 != null ? field2 : "");

        return sqlTemplate;
    }
}