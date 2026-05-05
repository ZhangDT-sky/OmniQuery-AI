package com.omniquery.rag.repository;

import com.omniquery.rag.model.ExampleSqlDocument;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "omniquery.rag")
public class ExampleSqlCatalog {

    private List<ConfiguredExample> examples = new ArrayList<>();

    public List<ExampleSqlDocument> documents() {
        return examples.stream()
            .filter(ConfiguredExample::isValid)
            .map(example -> new ExampleSqlDocument(
                example.question,
                example.sql,
                List.copyOf(example.tables),
                example.explanation == null ? "" : example.explanation
            ))
            .toList();
    }

    public List<ConfiguredExample> getExamples() {
        return examples;
    }

    public void setExamples(List<ConfiguredExample> examples) {
        this.examples = examples == null ? new ArrayList<>() : examples;
    }

    public static class ConfiguredExample {
        private String question;
        private String sql;
        private List<String> tables = new ArrayList<>();
        private String explanation;

        private boolean isValid() {
            return hasText(question) && hasText(sql) && tables != null && !tables.isEmpty();
        }

        private boolean hasText(String value) {
            return value != null && !value.isBlank();
        }

        public String getQuestion() {
            return question;
        }

        public void setQuestion(String question) {
            this.question = question;
        }

        public String getSql() {
            return sql;
        }

        public void setSql(String sql) {
            this.sql = sql;
        }

        public List<String> getTables() {
            return tables;
        }

        public void setTables(List<String> tables) {
            this.tables = tables == null ? new ArrayList<>() : tables;
        }

        public String getExplanation() {
            return explanation;
        }

        public void setExplanation(String explanation) {
            this.explanation = explanation;
        }
    }
}
