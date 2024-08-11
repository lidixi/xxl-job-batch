package com.example.cdrjob.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Repository
public class DataDao {

    private final JdbcTemplate jdbcTemplate;

    public DataDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Integer executeQueryForInt(String sql) {
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }

    public List<Map<String, Object>> executeQueryForList(String sql) {
        return jdbcTemplate.queryForList(sql);
    }

    public void logExecution(Long ruleId, String groupName, Integer baseValue, Integer auditValue, int wrongValueCount, String wrongIds) {
        String sql = "INSERT INTO DATA (RULE, GROUP_NAME, BASE_VALUE, AUDIT_VALUE, WRONG_VALUE, RATE, RUN_TIME, WRONG_ID) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        // 计算基础值和稽查值的比率
        double rate = auditValue != 0 ? ((double) baseValue / auditValue) * 100 : 0;

        // 将记录插入到 DATA 表中
        jdbcTemplate.update(sql, ruleId, groupName, baseValue, auditValue, wrongValueCount, rate, new Date(), wrongIds);
    }

    public List<Map<String, Object>> getAllRecords() {
        String sql = "SELECT * FROM DATA";
        return jdbcTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> getWrongRecords(String wrongIds) {
        String sql = "SELECT * FROM PATIENT_BASE WHERE ID IN (" + wrongIds + ")";
        return jdbcTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> getRecentRates(String rule) {
        String sql = "SELECT AUDIT_VALUE, RATE FROM DATA WHERE RULE = ? ORDER BY RUN_TIME DESC LIMIT 15";
        return jdbcTemplate.queryForList(sql, rule);
    }

    public Map<String, Object> getGroupStats(String group) {
        String sql = "SELECT SUM(BASE_VALUE) AS totalBase, SUM(WRONG_VALUE) AS totalWrong FROM DATA WHERE `GROUP` = ?";
        Map<String, Object> result = jdbcTemplate.queryForMap(sql, group);

        double totalBase = ((Number) result.get("totalBase")).doubleValue();
        double totalWrong = ((Number) result.get("totalWrong")).doubleValue();
        double normalRate = (totalBase - totalWrong) / totalBase * 100;

        result.put("normalRate", normalRate);

        return result;
    }
}