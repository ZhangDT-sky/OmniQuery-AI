package com.omniquery.core.llm;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SqlGenerationOutputTest {

    @Test
    void convertsStructuredOutputToGeneratedSql() {
        SqlGenerationOutput output = new SqlGenerationOutput();
        output.setSql(" SELECT id FROM orders ");
        output.setTables(List.of("orders"));
        output.setColumns(List.of("id"));
        output.setExplanation("structured");

        var generated = output.toGeneratedSql();

        assertEquals("SELECT id FROM orders", generated.sql());
        assertEquals(List.of("orders"), generated.tables());
        assertEquals(List.of("id"), generated.columns());
        assertEquals("structured", generated.explanation());
    }

    @Test
    void rejectsMissingSql() {
        SqlGenerationOutput output = new SqlGenerationOutput();

        assertThrows(IllegalArgumentException.class, output::toGeneratedSql);
    }
}
