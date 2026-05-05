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
}
