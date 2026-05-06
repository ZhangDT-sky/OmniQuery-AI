# OmniQuery AI

[中文说明](README.zh-CN.md)

OmniQuery AI is a compact Java/Spring Boot NL2SQL engine. It turns a natural-language question into SQL, retrieves schema and example context, validates generated SQL with an AST guard, injects tenant ACL conditions, executes read-only queries, and returns a traceable response.

## Why This Project Exists

The goal is not to build a large agent platform. The goal is to make one agent capability production-shaped: safe natural-language database querying.

## Core Flow

```text
question -> schema/example retrieval -> LangChain4j SQL generation boundary -> SQL guard -> ACL rewrite -> query execution -> trace
```

## Highlights

- LangChain4j dependency boundary for NL2SQL generation.
- Read-only tool calling for schema and example lookup.
- Real schema/example retrieval instead of fake vector search.
- Druid AST validation for SELECT-only SQL.
- Policy-driven ACL rewrite for tenant isolation.
- H2 demo data and JUnit coverage for reproducible evaluation.

## Run Backend

```powershell
cd omniquery-backend
mvn test
mvn -pl omniquery-api -am spring-boot:run
```

The default profile starts an in-memory H2 demo database. For a real database, use one of the external profiles. The app scans JDBC metadata into schema retrieval and security policy at startup/runtime, and disables SQL initialization for external databases.

MySQL:

```powershell
cd omniquery-backend
mvn -pl omniquery-api -am spring-boot:run -Dspring-boot.run.profiles=mysql
```

Override the MySQL target when needed:

```powershell
$env:OMNIQUERY_MYSQL_URL='jdbc:mysql://localhost:3306/data_agent?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai'
$env:OMNIQUERY_MYSQL_USERNAME='root'
$env:OMNIQUERY_MYSQL_PASSWORD='root'
```

PostgreSQL:

```powershell
cd omniquery-backend
$env:OMNIQUERY_POSTGRES_URL='jdbc:postgresql://localhost:5432/postgres'
$env:OMNIQUERY_POSTGRES_USERNAME='postgres'
$env:OMNIQUERY_POSTGRES_PASSWORD='postgres'
mvn -pl omniquery-api -am spring-boot:run -Dspring-boot.run.profiles=postgres
```

## Run Frontend

```powershell
cd omniquery-frontend
npm.cmd install
npm.cmd run dev
```

Open `http://localhost:5173`.

## API Example

```powershell
Invoke-RestMethod -Uri http://localhost:8080/api/query -Method POST -ContentType 'application/json' -Body '{"question":"show recent orders with customer names","tenantId":"tenant_a"}'
```

## Run With LangChain4j

The `llm` profile uses a LangChain4j OpenAI-compatible chat model. By default, OmniQuery reads the API key from `MODEL_API_KEY`, uses DashScope compatible mode, and selects `kimi-k2.6`.

```powershell
cd omniquery-backend
$env:MODEL_API_KEY='your-api-key'
$env:OMNIQUERY_LLM_BASE_URL='https://dashscope.aliyuncs.com/compatible-mode/v1'
$env:OMNIQUERY_LLM_MODEL='kimi-k2.6'
mvn -pl omniquery-api -am spring-boot:run -Dspring-boot.run.profiles=llm
```

Combine profiles when querying a real database:

```powershell
mvn -pl omniquery-api -am spring-boot:run -Dspring-boot.run.profiles=llm,mysql
```

## Run With Vector RAG

The `vector-rag` profile stores schema/example embeddings in pgvector. By default, it connects to `jdbc:postgresql://localhost:5432/postgres` with `postgres/root`, reads the embedding key from `EMBEDDING_API_KEY`, and calls SiliconFlow's OpenAI-compatible embedding endpoint with `Qwen/Qwen3-Embedding-8B`.

```powershell
cd omniquery-backend
$env:EMBEDDING_API_KEY='your-siliconflow-api-key'
mvn -pl omniquery-api -am spring-boot:run -Dspring-boot.run.profiles=vector-rag
```

Use it together with the real LLM path:

```powershell
mvn -pl omniquery-api -am spring-boot:run -Dspring-boot.run.profiles=llm,vector-rag
```

## Real MySQL Demo

Use the demo script when you want to verify the full external-database path instead of the default H2 database. It creates an isolated `omniquery_demo` MySQL database with `customers` and `orders`, including two tenants for ACL checks.

Prerequisites:

- Local MySQL reachable with `root/root`, or pass different credentials to the script.
- pgvector running on `localhost:5432` with `postgres/root`.
- `MODEL_API_KEY` for DashScope compatible chat model.
- `EMBEDDING_API_KEY` for SiliconFlow embeddings.

```powershell
.\scripts\run-mysql-demo.ps1 -Build
```

Use custom MySQL credentials:

```powershell
.\scripts\run-mysql-demo.ps1 -MysqlUser root -MysqlPassword root -MysqlHost localhost -MysqlPort 3306
```

Then verify tenant isolation:

```powershell
Invoke-RestMethod -Uri http://localhost:8080/api/query -Method POST -ContentType 'application/json' -Body '{"question":"list recent orders with customer names","tenantId":"tenant_a"}'
Invoke-RestMethod -Uri http://localhost:8080/api/query -Method POST -ContentType 'application/json' -Body '{"question":"list recent orders with customer names","tenantId":"tenant_b"}'
```

Expected result: `tenant_a` sees 3 order rows, while `tenant_b` sees only 1 row. The response trace should show schema/example retrieval, SQL generation, Druid guard validation, ACL parameter injection, and read-only MySQL execution.

## Read-Only MCP Tools

OmniQuery also exposes a lightweight JSON-RPC tool endpoint at `/api/mcp`. The endpoint is intentionally read-only: `safe_query` reuses the same NL2SQL engine, SQL guard, ACL rewrite, and JDBC execution path as the HTTP API.

List tools:

```powershell
Invoke-RestMethod -Uri http://localhost:8080/api/mcp -Method POST -ContentType 'application/json' -Body '{"jsonrpc":"2.0","id":"1","method":"tools/list","params":{}}'
```

Call `safe_query`:

```powershell
Invoke-RestMethod -Uri http://localhost:8080/api/mcp -Method POST -ContentType 'application/json' -Body '{"jsonrpc":"2.0","id":"2","method":"tools/call","params":{"name":"safe_query","arguments":{"question":"show total paid amount by customer","tenantId":"tenant_a"}}}'
```

## Resume Description

Built a generic NL2SQL query engine with Spring Boot and LangChain4j, including schema/example retrieval, read-only MCP-style tool calling, Druid AST SQL guardrails, policy-driven ACL injection, traceable execution, and H2-backed integration tests.
