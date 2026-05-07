package com.omniquery.core.service;

import com.omniquery.core.config.OmniQueryProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class JdbcQueryExecutor {

    private final JdbcTemplate jdbcTemplate;

    public JdbcQueryExecutor(JdbcTemplate jdbcTemplate, OmniQueryProperties properties) {
        this.jdbcTemplate = jdbcTemplate;
        this.jdbcTemplate.setQueryTimeout(properties.security().getQueryTimeoutSeconds());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> query(String sql) {
        return jdbcTemplate.queryForList(sql);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> query(String sql, List<Object> parameters) {
        return jdbcTemplate.queryForList(sql, parameters.toArray());
    }

    @Transactional(readOnly = true)
    public int count(String sql, List<Object> parameters) {
        String countSql = "SELECT COUNT(*) FROM (" + stripTrailingSemicolon(sql) + ") omniquery_preflight";
        Integer count = jdbcTemplate.queryForObject(countSql, Integer.class, parameters.toArray());
        return count == null ? 0 : count;
    }

    private String stripTrailingSemicolon(String sql) {
        String trimmed = sql.trim();
        while (trimmed.endsWith(";")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1).trim();
        }
        return trimmed;
    }
}
