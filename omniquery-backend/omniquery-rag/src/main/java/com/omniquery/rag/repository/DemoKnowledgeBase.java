package com.omniquery.rag.repository;

import com.omniquery.rag.model.ExampleSqlDocument;
import com.omniquery.rag.model.SchemaDocument;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Profile("!external-db")
public class DemoKnowledgeBase implements KnowledgeBase {

    @Override
    public List<SchemaDocument> schemas() {
        return List.of(
            new SchemaDocument(
                "customers",
                "Customer accounts that own orders",
                List.of("id BIGINT primary key", "name VARCHAR", "tenant_id VARCHAR", "created_by VARCHAR"),
                List.of("customers.id = orders.customer_id"),
                List.of("admin", "user")
            ),
            new SchemaDocument(
                "orders",
                "Orders placed by customers",
                List.of("id BIGINT primary key", "customer_id BIGINT", "status VARCHAR", "total_amount DECIMAL", "tenant_id VARCHAR", "created_by VARCHAR", "created_at TIMESTAMP"),
                List.of("orders.customer_id = customers.id"),
                List.of("admin", "user")
            )
        );
    }

    @Override
    public List<ExampleSqlDocument> examples() {
        return List.of(
            new ExampleSqlDocument(
                "total paid amount by customer",
                "SELECT c.name, SUM(o.total_amount) AS total_paid FROM orders o JOIN customers c ON c.id = o.customer_id WHERE o.status = 'PAID' GROUP BY c.name LIMIT 100",
                List.of("orders", "customers"),
                "Aggregate paid orders by customer"
            ),
            new ExampleSqlDocument(
                "recent orders with customer names",
                "SELECT o.id, c.name, o.status, o.total_amount FROM orders o JOIN customers c ON c.id = o.customer_id ORDER BY o.created_at DESC LIMIT 100",
                List.of("orders", "customers"),
                "Join orders to customers and sort by creation time"
            )
        );
    }
}
