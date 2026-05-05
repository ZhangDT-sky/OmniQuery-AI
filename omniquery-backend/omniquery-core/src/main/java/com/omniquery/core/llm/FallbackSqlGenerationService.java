package com.omniquery.core.llm;

import com.omniquery.core.model.GeneratedSql;
import com.omniquery.core.model.QueryIntent;
import com.omniquery.rag.model.RetrievedContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Profile("!llm")
public class FallbackSqlGenerationService implements SqlGenerationService {

    @Override
    public GeneratedSql generate(QueryIntent intent, RetrievedContext context) {
        String question = intent.normalizedQuestion().toLowerCase();
        if (question.contains("total") || question.contains("sum") || question.contains("amount")) {
            return new GeneratedSql(
                "SELECT c.name, SUM(o.total_amount) AS total_paid FROM orders o JOIN customers c ON c.id = o.customer_id WHERE o.status = 'PAID' GROUP BY c.name",
                List.of("orders", "customers"),
                List.of("name", "total_amount", "status"),
                "Generated deterministic aggregate query for local tests"
            );
        }
        return new GeneratedSql(
            "SELECT o.id, c.name, o.status, o.total_amount FROM orders o JOIN customers c ON c.id = o.customer_id ORDER BY o.created_at DESC",
            List.of("orders", "customers"),
            List.of("id", "name", "status", "total_amount", "created_at"),
            "Generated deterministic recent orders query for local tests"
        );
    }
}
