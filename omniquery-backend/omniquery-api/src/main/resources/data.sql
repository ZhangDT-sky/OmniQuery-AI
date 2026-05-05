INSERT INTO customers (id, name, tenant_id, created_by) VALUES
(1, 'Acme Corp', 'tenant_a', 'u1'),
(2, 'Northwind', 'tenant_a', 'u2'),
(3, 'Globex', 'tenant_b', 'u3');

INSERT INTO orders (id, customer_id, status, total_amount, tenant_id, created_by, created_at) VALUES
(1001, 1, 'PAID', 1200.00, 'tenant_a', 'u1', TIMESTAMP '2026-01-10 10:00:00'),
(1002, 1, 'PENDING', 300.00, 'tenant_a', 'u1', TIMESTAMP '2026-01-12 11:30:00'),
(1003, 2, 'PAID', 800.00, 'tenant_a', 'u2', TIMESTAMP '2026-01-15 09:20:00'),
(1004, 3, 'PAID', 9999.00, 'tenant_b', 'u3', TIMESTAMP '2026-01-16 14:00:00');
