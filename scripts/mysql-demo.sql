DROP DATABASE IF EXISTS omniquery_demo;
CREATE DATABASE omniquery_demo CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE omniquery_demo;

CREATE TABLE customers (
    id BIGINT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    tenant_id VARCHAR(50) NOT NULL,
    created_by VARCHAR(50) NOT NULL
);

CREATE TABLE orders (
    id BIGINT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    total_amount DECIMAL(12, 2) NOT NULL,
    tenant_id VARCHAR(50) NOT NULL,
    created_by VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_orders_customer FOREIGN KEY (customer_id) REFERENCES customers(id)
);

INSERT INTO customers (id, name, tenant_id, created_by) VALUES
(1, 'Acme Corp', 'tenant_a', 'u1'),
(2, 'Northwind', 'tenant_a', 'u2'),
(3, 'Globex', 'tenant_b', 'u3');

INSERT INTO orders (id, customer_id, status, total_amount, tenant_id, created_by, created_at) VALUES
(1001, 1, 'PAID', 1200.00, 'tenant_a', 'u1', '2026-01-10 10:00:00'),
(1002, 1, 'PENDING', 300.00, 'tenant_a', 'u1', '2026-01-12 11:30:00'),
(1003, 2, 'PAID', 800.00, 'tenant_a', 'u2', '2026-01-15 09:20:00'),
(1004, 3, 'PAID', 9999.00, 'tenant_b', 'u3', '2026-01-16 14:00:00');

SELECT 'customers' AS table_name, COUNT(*) AS rows_count FROM customers
UNION ALL
SELECT 'orders', COUNT(*) FROM orders;
