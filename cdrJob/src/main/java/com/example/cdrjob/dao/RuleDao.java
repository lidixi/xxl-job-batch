package com.example.cdrjob.dao;

import com.example.cdrjob.config.DataSourceContextHolder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class RuleDao {

    private final JdbcTemplate jdbcTemplate;

    public RuleDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<String> getDatabases() {
        return jdbcTemplate.queryForList("SHOW DATABASES", String.class);
    }

    public List<String> getTables(String database) {
        return jdbcTemplate.queryForList("SHOW TABLES FROM " + database, String.class);
    }

    public List<String> getColumns(String database, String table) {
        return jdbcTemplate.queryForList("SHOW COLUMNS FROM " + table + " FROM " + database, String.class);
    }

    public void setDataSource(String database) {
        DataSourceContextHolder.setDataSource(database);
    }

    public String getBaseParentSQL(String group) {
        String sql = "SELECT BASE_PARENT FROM `GROUP` WHERE GROUP_NAME = ?";
        return jdbcTemplate.queryForObject(sql, String.class, group);
    }

    public String getAuditParentSQL(String group) {
        String sql = "SELECT AUDIT_PARENT FROM `GROUP` WHERE GROUP_NAME = ?";
        return jdbcTemplate.queryForObject(sql, String.class, group);
    }

    public String getWrongParentSQL(String group) {
        String sql = "SELECT WRONG_PARENT FROM `GROUP` WHERE GROUP_NAME = ?";
        return jdbcTemplate.queryForObject(sql, String.class, group);
    }

    public void createRule(String group, String ruleName, String baseSon, String auditSon, String wrongSon) {
        String sql = "INSERT INTO RULE (GROUP_NAME, RULE_NAME, BASE_SON, AUDIT_SON, WRONG_SON) VALUES (?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, group, ruleName, baseSon, auditSon, wrongSon);
    }

    public Map<String, Object> getRuleById(Long ruleId) {
        String sql = "SELECT r.RULE_NAME, g.GROUP_NAME, r.BASE_SON, r.AUDIT_SON, r.WRONG_SON " +
                "FROM RULE r " +
                "JOIN `GROUP` g ON r.GROUP_ID = g.ID " +
                "WHERE r.ID = ?";
        return jdbcTemplate.queryForMap(sql, ruleId);
    }

    public Long getGroupIdByGroupName(String groupName) {
        String sql = "SELECT ID FROM `GROUP` WHERE GROUP_NAME = ?";
        return jdbcTemplate.queryForObject(sql, Long.class, groupName);
    }

    public Map<String, String> getRuleSQLTemplates(Long ruleId) {
        String sql = "SELECT BASE_SON, AUDIT_SON, WRONG_SON FROM RULE WHERE ID = ?";
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
            Map<String, String> sqlTemplates = new HashMap<>();
            sqlTemplates.put("baseSQL", rs.getString("BASE_SON"));
            sqlTemplates.put("auditSQL", rs.getString("AUDIT_SON"));
            sqlTemplates.put("wrongSQL", rs.getString("WRONG_SON"));
            return sqlTemplates;
        }, ruleId);
    }
}