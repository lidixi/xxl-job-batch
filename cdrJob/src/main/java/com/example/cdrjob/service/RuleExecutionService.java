package com.example.cdrjob.service;

import com.example.cdrjob.config.DataSourceContextHolder;
import com.example.cdrjob.dao.DataDao;
import com.example.cdrjob.dao.RuleDao;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class RuleExecutionService {

    private final DataDao dataDao;
    private final RuleDao ruleDao;

    public RuleExecutionService(DataDao dataDao, RuleDao ruleDao) {
        this.dataDao = dataDao;
        this.ruleDao = ruleDao;
    }

    public void executeRule(Long ruleId, Map<String, Object> params) {
        // 动态设置数据源，根据任务参数决定使用哪个数据源
        if (params.containsKey("databaseType")) {
            String databaseType = params.get("databaseType").toString();
            DataSourceContextHolder.setDataSource(databaseType);
        } else {
            // 默认设置为 build 数据源
            DataSourceContextHolder.setDataSource("build");
        }

        Map<String, Object> rule = ruleDao.getRuleById(ruleId);

        String groupName = (String) rule.get("GROUP_NAME");
        String baseQuery = applyParams((String) rule.get("BASE_SON"), params);
        String auditQuery = applyParams((String) rule.get("AUDIT_SON"), params);
        String wrongQuery = applyParams((String) rule.get("WRONG_SON"), params);

        Integer baseValue = dataDao.executeQueryForInt(baseQuery);

        DataSourceContextHolder.setDataSource("cdrAudit");
        Integer auditValue = dataDao.executeQueryForInt(auditQuery);

        DataSourceContextHolder.setDataSource("cdrBase");
        List<Map<String, Object>> wrongValues = dataDao.executeQueryForList(wrongQuery);
        // 处理异常值查询结果，生成 WRONG_VALUE 和 WRONG_ID
        StringBuilder wrongIds = new StringBuilder();
        for (Map<String, Object> row : wrongValues) {
            if (!wrongIds.isEmpty()) {
                wrongIds.append(",");
            }
            wrongIds.append(row.get("ID"));
        }
        int wrongValueCount = wrongValues.size();

        DataSourceContextHolder.setDataSource("log");
        dataDao.logExecution(ruleId, groupName, baseValue, auditValue, wrongValueCount, wrongIds.toString());

        DataSourceContextHolder.clearDataSource();
    }

    private String applyParams(String sql, Map<String, Object> params) {
        // 替换 SQL 模板中的占位符为实际的参数值
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            sql = sql.replace("{" + entry.getKey() + "}", entry.getValue().toString());
        }
        return sql;
    }
}