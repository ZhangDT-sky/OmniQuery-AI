# OmniQuery AI

[中文说明](README.md)

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
- Schema and example retrieval backed by pgvector when `vector-rag` is enabled.
- Druid AST validation for SELECT-only SQL.
- Policy-driven ACL rewrite for tenant isolation.
- H2 demo data and JUnit coverage for reproducible evaluation.

## Command Style

Windows examples are provided in both PowerShell and cmd.

## Run Backend

PowerShell:

```powershell
cd omniquery-backend
mvn test
mvn -pl omniquery-api -am spring-boot:run
```

cmd:

```cmd
cd omniquery-backend
mvn test
mvn -pl omniquery-api -am spring-boot:run
```

The default profile starts an in-memory H2 demo database. For a real database, use one of the external profiles. The app scans JDBC metadata into schema retrieval and security policy at startup/runtime, and disables SQL initialization for external databases.

## MySQL Profile

PowerShell:

```powershell
cd omniquery-backend
$env:OMNIQUERY_MYSQL_URL='jdbc:mysql://localhost:3306/data_agent?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai'
$env:OMNIQUERY_MYSQL_USERNAME='root'
$env:OMNIQUERY_MYSQL_PASSWORD='root'
mvn -pl omniquery-api -am spring-boot:run -Dspring-boot.run.profiles=mysql
```

cmd:

```cmd
cd omniquery-backend
set OMNIQUERY_MYSQL_URL=jdbc:mysql://localhost:3306/data_agent?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai
set OMNIQUERY_MYSQL_USERNAME=root
set OMNIQUERY_MYSQL_PASSWORD=root
mvn -pl omniquery-api -am spring-boot:run -Dspring-boot.run.profiles=mysql
```

## PostgreSQL Profile

PowerShell:

```powershell
cd omniquery-backend
$env:OMNIQUERY_POSTGRES_URL='jdbc:postgresql://localhost:5432/postgres'
$env:OMNIQUERY_POSTGRES_USERNAME='postgres'
$env:OMNIQUERY_POSTGRES_PASSWORD='postgres'
mvn -pl omniquery-api -am spring-boot:run -Dspring-boot.run.profiles=postgres
```

cmd:

```cmd
cd omniquery-backend
set OMNIQUERY_POSTGRES_URL=jdbc:postgresql://localhost:5432/postgres
set OMNIQUERY_POSTGRES_USERNAME=postgres
set OMNIQUERY_POSTGRES_PASSWORD=postgres
mvn -pl omniquery-api -am spring-boot:run -Dspring-boot.run.profiles=postgres
```

## Run Frontend

PowerShell:

```powershell
cd omniquery-frontend
npm.cmd install
npm.cmd run dev
```

cmd:

```cmd
cd omniquery-frontend
npm install
npm run dev
```

Open `http://localhost:5173`.

## API Example

PowerShell:

```powershell
Invoke-RestMethod -Uri http://localhost:8080/api/query -Method POST -ContentType 'application/json' -Body '{"question":"show recent orders with customer names","tenantId":"tenant_a"}'
```

cmd:

```cmd
curl -X POST http://localhost:8080/api/query -H "Content-Type: application/json" -d "{\"question\":\"show recent orders with customer names\",\"tenantId\":\"tenant_a\"}"
```

## Run With LangChain4j

The `llm` profile uses a LangChain4j OpenAI-compatible chat model. By default, OmniQuery reads the API key from `MODEL_API_KEY`, uses DashScope compatible mode, and selects `kimi-k2.6`.

PowerShell:

```powershell
cd omniquery-backend
$env:MODEL_API_KEY='your-api-key'
$env:OMNIQUERY_LLM_BASE_URL='https://dashscope.aliyuncs.com/compatible-mode/v1'
$env:OMNIQUERY_LLM_MODEL='kimi-k2.6'
mvn -pl omniquery-api -am spring-boot:run -Dspring-boot.run.profiles=llm
```

cmd:

```cmd
cd omniquery-backend
set MODEL_API_KEY=your-api-key
set OMNIQUERY_LLM_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
set OMNIQUERY_LLM_MODEL=kimi-k2.6
mvn -pl omniquery-api -am spring-boot:run -Dspring-boot.run.profiles=llm
```

Combine profiles when querying a real database:

PowerShell:

```powershell
mvn -pl omniquery-api -am spring-boot:run -Dspring-boot.run.profiles=llm,mysql
```

cmd:

```cmd
mvn -pl omniquery-api -am spring-boot:run -Dspring-boot.run.profiles=llm,mysql
```

## Run With Vector RAG

The `vector-rag` profile stores schema/example embeddings in pgvector. By default, it connects to `jdbc:postgresql://localhost:5432/postgres` with `postgres/root`, reads the embedding key from `EMBEDDING_API_KEY`, and calls SiliconFlow's OpenAI-compatible embedding endpoint with `Qwen/Qwen3-Embedding-8B`.

PowerShell:

```powershell
cd omniquery-backend
$env:EMBEDDING_API_KEY='your-siliconflow-api-key'
mvn -pl omniquery-api -am spring-boot:run -Dspring-boot.run.profiles=vector-rag
```

cmd:

```cmd
cd omniquery-backend
set EMBEDDING_API_KEY=your-siliconflow-api-key
mvn -pl omniquery-api -am spring-boot:run -Dspring-boot.run.profiles=vector-rag
```

Use it together with the real LLM path:

PowerShell:

```powershell
mvn -pl omniquery-api -am spring-boot:run -Dspring-boot.run.profiles=llm,vector-rag
```

cmd:

```cmd
mvn -pl omniquery-api -am spring-boot:run -Dspring-boot.run.profiles=llm,vector-rag
```

## Real MySQL Demo

Use the demo script when you want to verify the full external-database path instead of the default H2 database. It creates an isolated `omniquery_demo` MySQL database with `customers` and `orders`, including two tenants for ACL checks.

Prerequisites:

- Local MySQL reachable with `root/root`, or pass different credentials to the script.
- pgvector running on `localhost:5432` with `postgres/root`.
- `MODEL_API_KEY` for DashScope compatible chat model.
- `EMBEDDING_API_KEY` for SiliconFlow embeddings.

PowerShell:

```powershell
.\scripts\run-mysql-demo.ps1 -Build
```

cmd:

```cmd
powershell -ExecutionPolicy Bypass -File scripts\run-mysql-demo.ps1 -Build
```

Use custom MySQL credentials:

PowerShell:

```powershell
.\scripts\run-mysql-demo.ps1 -MysqlUser root -MysqlPassword root -MysqlHost localhost -MysqlPort 3306
```

cmd:

```cmd
powershell -ExecutionPolicy Bypass -File scripts\run-mysql-demo.ps1 -MysqlUser root -MysqlPassword root -MysqlHost localhost -MysqlPort 3306
```

Then verify tenant isolation:

PowerShell:

```powershell
Invoke-RestMethod -Uri http://localhost:8080/api/query -Method POST -ContentType 'application/json' -Body '{"question":"list recent orders with customer names","tenantId":"tenant_a"}'
Invoke-RestMethod -Uri http://localhost:8080/api/query -Method POST -ContentType 'application/json' -Body '{"question":"list recent orders with customer names","tenantId":"tenant_b"}'
```

cmd:

```cmd
curl -X POST http://localhost:8080/api/query -H "Content-Type: application/json" -d "{\"question\":\"list recent orders with customer names\",\"tenantId\":\"tenant_a\"}"
curl -X POST http://localhost:8080/api/query -H "Content-Type: application/json" -d "{\"question\":\"list recent orders with customer names\",\"tenantId\":\"tenant_b\"}"
```

Expected result: `tenant_a` sees 3 order rows, while `tenant_b` sees only 1 row. The response trace should show schema/example retrieval, SQL generation, Druid guard validation, ACL parameter injection, and read-only MySQL execution.

## Docker One-Command Deployment

Docker Compose starts MySQL, pgvector, the Spring Boot API, and the React frontend together. Secrets are injected through `.env`; they are not baked into images and should not be committed.

Services:

```text
mysql      MySQL 8.0 with omniquery_demo initialization
pgvector   PostgreSQL with pgvector
api        Spring Boot with mysql,llm,vector-rag profiles
frontend   Nginx serving frontend static files
```

Copy the environment template before the first run:

PowerShell:

```powershell
Copy-Item .env.example .env
```

cmd:

```cmd
copy .env.example .env
```

Then edit `.env` and set at least:

```env
MODEL_API_KEY=your-dashscope-api-key
EMBEDDING_API_KEY=your-siliconflow-api-key
```

Start:

PowerShell:

```powershell
docker compose up -d --build
```

cmd:

```cmd
docker compose up -d --build
```

Inspect services:

PowerShell:

```powershell
docker compose ps
docker compose logs -f api
```

cmd:

```cmd
docker compose ps
docker compose logs -f api
```

URLs:

```text
Frontend: http://localhost:5173
Backend:  http://localhost:8080
MySQL:    localhost:3307
pgvector: localhost:5433
```

Verify query behavior:

PowerShell:

```powershell
Invoke-RestMethod -Uri http://localhost:8080/api/query -Method POST -ContentType 'application/json' -Body '{"question":"list recent orders with customer names","tenantId":"tenant_a"}'
Invoke-RestMethod -Uri http://localhost:8080/api/query -Method POST -ContentType 'application/json' -Body '{"question":"list recent orders with customer names","tenantId":"tenant_b"}'
```

cmd:

```cmd
curl -X POST http://localhost:8080/api/query -H "Content-Type: application/json" -d "{\"question\":\"list recent orders with customer names\",\"tenantId\":\"tenant_a\"}"
curl -X POST http://localhost:8080/api/query -H "Content-Type: application/json" -d "{\"question\":\"list recent orders with customer names\",\"tenantId\":\"tenant_b\"}"
```

Check pgvector documents:

PowerShell:

```powershell
docker compose exec pgvector psql -U postgres -d postgres -c "SELECT kind, count(*) FROM omniquery_rag_documents GROUP BY kind ORDER BY kind;"
```

cmd:

```cmd
docker compose exec pgvector psql -U postgres -d postgres -c "SELECT kind, count(*) FROM omniquery_rag_documents GROUP BY kind ORDER BY kind;"
```

Stop services:

PowerShell:

```powershell
docker compose down
```

cmd:

```cmd
docker compose down
```

Reset volumes and reinitialize databases:

PowerShell:

```powershell
docker compose down -v
docker compose up -d --build
```

cmd:

```cmd
docker compose down -v
docker compose up -d --build
```

## Read-Only MCP Tools

OmniQuery also exposes a lightweight JSON-RPC tool endpoint at `/api/mcp`. The endpoint is intentionally read-only: `safe_query` reuses the same NL2SQL engine, SQL guard, ACL rewrite, and JDBC execution path as the HTTP API.

List tools:

PowerShell:

```powershell
Invoke-RestMethod -Uri http://localhost:8080/api/mcp -Method POST -ContentType 'application/json' -Body '{"jsonrpc":"2.0","id":"1","method":"tools/list","params":{}}'
```

cmd:

```cmd
curl -X POST http://localhost:8080/api/mcp -H "Content-Type: application/json" -d "{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"method\":\"tools/list\",\"params\":{}}"
```

Call `safe_query`:

PowerShell:

```powershell
Invoke-RestMethod -Uri http://localhost:8080/api/mcp -Method POST -ContentType 'application/json' -Body '{"jsonrpc":"2.0","id":"2","method":"tools/call","params":{"name":"safe_query","arguments":{"question":"show total paid amount by customer","tenantId":"tenant_a"}}}'
```

cmd:

```cmd
curl -X POST http://localhost:8080/api/mcp -H "Content-Type: application/json" -d "{\"jsonrpc\":\"2.0\",\"id\":\"2\",\"method\":\"tools/call\",\"params\":{\"name\":\"safe_query\",\"arguments\":{\"question\":\"show total paid amount by customer\",\"tenantId\":\"tenant_a\"}}}"
```
