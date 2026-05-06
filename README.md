# OmniQuery AI 中文说明

[English](README.en.md)

OmniQuery AI 是一个小而精的 Java/Spring Boot NL2SQL 查询引擎。它聚焦一件事：把自然语言问题转换成可审计、可控、可追踪的只读 SQL 查询。

这个项目不是大而全的 Agent 平台，也不强调多智能体概念。它的重点是真实数据库接入、Schema-RAG、LangChain4j SQL 生成、SQL 安全网关、ACL 多租户隔离和测试覆盖。

## 核心链路

```text
用户问题
  -> 意图归一化
  -> Schema / SQL Example 检索
  -> LangChain4j 结构化 SQL 生成
  -> Druid AST SQL Guard
  -> ACL 参数化注入
  -> JDBC 只读执行
  -> 返回结果和 trace
```

## 项目亮点

- **轻量级 NL2SQL 编排内核**：用 Orchestrator 串联检索、生成、校验、权限注入和执行，避免把项目做成难以解释的多 Agent 结构。
- **真实数据库 Schema-RAG**：通过 `DatabaseMetaData` 扫描 MySQL/PostgreSQL 表结构，生成 Schema 文档并写入 pgvector。
- **配置化 SQL 样例召回**：通过 `omniquery-examples.yml` 管理 few-shot SQL examples，并和 schema 一起进入向量检索。
- **LangChain4j 接入**：使用 OpenAI-compatible ChatModel，默认兼容 DashScope，模型名为 `kimi-k2.6`。
- **SQL 安全网关**：基于 Alibaba Druid AST 实现 SELECT-only、危险函数拦截、字段校验、JOIN 数量限制和 LIMIT 控制。
- **ACL 多租户隔离**：对生成 SQL 做参数化租户条件注入，避免直接拼接字符串和跨租户数据泄露。
- **只读 MCP 风格工具接口**：`/api/mcp` 暴露 `safe_query`，复用同一套安全查询链路。

## 目录结构

```text
omniquery-backend/
  omniquery-api       HTTP API、MCP endpoint、配置和 demo 数据
  omniquery-core      NL2SQL 编排、LLM 边界、SQL 执行
  omniquery-rag       Schema/Example 检索、pgvector、embedding
  omniquery-security  Druid SQL Guard、ACL rewrite
  omniquery-sdk       共享模型

omniquery-frontend/   极简 React 查询界面
scripts/              真实 MySQL demo 脚本
```

## 命令说明

下面的 Windows 命令同时提供 PowerShell 和 cmd 两个版本。

## 环境变量

LLM：

PowerShell：

```powershell
$env:MODEL_API_KEY='your-api-key'
$env:OMNIQUERY_LLM_BASE_URL='https://dashscope.aliyuncs.com/compatible-mode/v1'
$env:OMNIQUERY_LLM_MODEL='kimi-k2.6'
```

cmd：

```cmd
set MODEL_API_KEY=your-api-key
set OMNIQUERY_LLM_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
set OMNIQUERY_LLM_MODEL=kimi-k2.6
```

Embedding：

PowerShell：

```powershell
$env:EMBEDDING_API_KEY='your-siliconflow-api-key'
$env:OMNIQUERY_EMBEDDING_BASE_URL='https://api.siliconflow.cn/v1/embeddings'
$env:OMNIQUERY_EMBEDDING_MODEL='Qwen/Qwen3-Embedding-8B'
```

cmd：

```cmd
set EMBEDDING_API_KEY=your-siliconflow-api-key
set OMNIQUERY_EMBEDDING_BASE_URL=https://api.siliconflow.cn/v1/embeddings
set OMNIQUERY_EMBEDDING_MODEL=Qwen/Qwen3-Embedding-8B
```

MySQL：

PowerShell：

```powershell
$env:OMNIQUERY_MYSQL_URL='jdbc:mysql://localhost:3306/omniquery_demo?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai'
$env:OMNIQUERY_MYSQL_USERNAME='root'
$env:OMNIQUERY_MYSQL_PASSWORD='root'
```

cmd：

```cmd
set OMNIQUERY_MYSQL_URL=jdbc:mysql://localhost:3306/omniquery_demo?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai
set OMNIQUERY_MYSQL_USERNAME=root
set OMNIQUERY_MYSQL_PASSWORD=root
```

pgvector 默认连接：

```text
jdbc:postgresql://localhost:5432/postgres
username: postgres
password: root
```

## 默认 H2 Demo

默认 profile 使用内存 H2 数据库，可以快速验证基础链路。

PowerShell：

```powershell
cd omniquery-backend
mvn test
mvn -pl omniquery-api -am spring-boot:run
```

cmd：

```cmd
cd omniquery-backend
mvn test
mvn -pl omniquery-api -am spring-boot:run
```

调用查询接口：

PowerShell：

```powershell
Invoke-RestMethod -Uri http://localhost:8080/api/query -Method POST -ContentType 'application/json' -Body '{"question":"show recent orders with customer names","tenantId":"tenant_a"}'
```

cmd：

```cmd
curl -X POST http://localhost:8080/api/query -H "Content-Type: application/json" -d "{\"question\":\"show recent orders with customer names\",\"tenantId\":\"tenant_a\"}"
```

## 真实 MySQL Demo

推荐用这个方式展示项目。脚本会创建独立测试库 `omniquery_demo`，不会使用你的业务库。

前置条件：

- 本地 MySQL 可访问，默认账号密码 `root/root`。
- pgvector 容器运行在 `localhost:5432`。
- 已设置 `MODEL_API_KEY`。
- 已设置 `EMBEDDING_API_KEY`。

一键初始化并启动：

PowerShell：

```powershell
.\scripts\run-mysql-demo.ps1 -Build
```

cmd：

```cmd
powershell -ExecutionPolicy Bypass -File scripts\run-mysql-demo.ps1 -Build
```

如果已经构建过，只想重新初始化 MySQL 并启动：

PowerShell：

```powershell
.\scripts\run-mysql-demo.ps1
```

cmd：

```cmd
powershell -ExecutionPolicy Bypass -File scripts\run-mysql-demo.ps1
```

如果想跳过初始化：

PowerShell：

```powershell
.\scripts\run-mysql-demo.ps1 -SkipInit
```

cmd：

```cmd
powershell -ExecutionPolicy Bypass -File scripts\run-mysql-demo.ps1 -SkipInit
```

自定义 MySQL 连接：

PowerShell：

```powershell
.\scripts\run-mysql-demo.ps1 -MysqlUser root -MysqlPassword root -MysqlHost localhost -MysqlPort 3306
```

cmd：

```cmd
powershell -ExecutionPolicy Bypass -File scripts\run-mysql-demo.ps1 -MysqlUser root -MysqlPassword root -MysqlHost localhost -MysqlPort 3306
```

验证 ACL 隔离：

PowerShell：

```powershell
Invoke-RestMethod -Uri http://localhost:8080/api/query -Method POST -ContentType 'application/json' -Body '{"question":"list recent orders with customer names","tenantId":"tenant_a"}'
Invoke-RestMethod -Uri http://localhost:8080/api/query -Method POST -ContentType 'application/json' -Body '{"question":"list recent orders with customer names","tenantId":"tenant_b"}'
```

cmd：

```cmd
curl -X POST http://localhost:8080/api/query -H "Content-Type: application/json" -d "{\"question\":\"list recent orders with customer names\",\"tenantId\":\"tenant_a\"}"
curl -X POST http://localhost:8080/api/query -H "Content-Type: application/json" -d "{\"question\":\"list recent orders with customer names\",\"tenantId\":\"tenant_b\"}"
```

预期结果：

- `tenant_a` 返回 3 条订单。
- `tenant_b` 返回 1 条订单。
- trace 中可以看到 retrieval、generation、guard、acl、execution 阶段。
- ACL 阶段会把 SQL 改写为带 `?` 参数的租户条件。

## Docker 一键部署

Docker Compose 会同时启动 MySQL、pgvector、Spring Boot 后端和 React 前端。敏感配置通过 `.env` 注入，不写入镜像，也不提交 Git。

服务组成：

```text
mysql      MySQL 8.0，初始化 omniquery_demo
pgvector   PostgreSQL + pgvector
api        Spring Boot，启用 mysql,llm,vector-rag
frontend   Nginx 托管前端静态文件
```

第一次运行前复制环境变量模板：

PowerShell：

```powershell
Copy-Item .env.example .env
```

cmd：

```cmd
copy .env.example .env
```

然后编辑 `.env`，至少填入：

```env
MODEL_API_KEY=your-dashscope-api-key
EMBEDDING_API_KEY=your-siliconflow-api-key
```

启动：

PowerShell：

```powershell
docker compose up -d --build
```

cmd：

```cmd
docker compose up -d --build
```

查看服务：

PowerShell：

```powershell
docker compose ps
docker compose logs -f api
```

cmd：

```cmd
docker compose ps
docker compose logs -f api
```

访问地址：

```text
前端: http://localhost:5173
后端: http://localhost:8080
MySQL: localhost:3307
pgvector: localhost:5433
```

验证查询：

PowerShell：

```powershell
Invoke-RestMethod -Uri http://localhost:8080/api/query -Method POST -ContentType 'application/json' -Body '{"question":"list recent orders with customer names","tenantId":"tenant_a"}'
Invoke-RestMethod -Uri http://localhost:8080/api/query -Method POST -ContentType 'application/json' -Body '{"question":"list recent orders with customer names","tenantId":"tenant_b"}'
```

cmd：

```cmd
curl -X POST http://localhost:8080/api/query -H "Content-Type: application/json" -d "{\"question\":\"list recent orders with customer names\",\"tenantId\":\"tenant_a\"}"
curl -X POST http://localhost:8080/api/query -H "Content-Type: application/json" -d "{\"question\":\"list recent orders with customer names\",\"tenantId\":\"tenant_b\"}"
```

检查 pgvector 文档：

PowerShell：

```powershell
docker compose exec pgvector psql -U postgres -d postgres -c "SELECT kind, count(*) FROM omniquery_rag_documents GROUP BY kind ORDER BY kind;"
```

cmd：

```cmd
docker compose exec pgvector psql -U postgres -d postgres -c "SELECT kind, count(*) FROM omniquery_rag_documents GROUP BY kind ORDER BY kind;"
```

停止服务：

PowerShell：

```powershell
docker compose down
```

cmd：

```cmd
docker compose down
```

清理数据卷并重新初始化数据库：

PowerShell：

```powershell
docker compose down -v
docker compose up -d --build
```

cmd：

```cmd
docker compose down -v
docker compose up -d --build
```

## Vector RAG

启用 `vector-rag` 后，系统会把 schema 和配置化 examples 向量化写入 pgvector。

PowerShell：

```powershell
cd omniquery-backend
mvn -pl omniquery-api -am spring-boot:run -Dspring-boot.run.profiles=vector-rag
```

cmd：

```cmd
cd omniquery-backend
mvn -pl omniquery-api -am spring-boot:run -Dspring-boot.run.profiles=vector-rag
```

和真实 MySQL、LLM 一起使用：

PowerShell：

```powershell
mvn -pl omniquery-api -am spring-boot:run -Dspring-boot.run.profiles=mysql,llm,vector-rag
```

cmd：

```cmd
mvn -pl omniquery-api -am spring-boot:run -Dspring-boot.run.profiles=mysql,llm,vector-rag
```

可用以下 SQL 检查 pgvector 文档数量：

PowerShell：

```powershell
docker exec pgvector psql -U postgres -d postgres -c "SELECT kind, count(*) FROM omniquery_rag_documents GROUP BY kind ORDER BY kind;"
```

cmd：

```cmd
docker exec pgvector psql -U postgres -d postgres -c "SELECT kind, count(*) FROM omniquery_rag_documents GROUP BY kind ORDER BY kind;"
```

真实 MySQL demo 下预期为：

```text
example  2
schema   2
```

## MCP 风格只读工具

列出工具：

PowerShell：

```powershell
Invoke-RestMethod -Uri http://localhost:8080/api/mcp -Method POST -ContentType 'application/json' -Body '{"jsonrpc":"2.0","id":"1","method":"tools/list","params":{}}'
```

cmd：

```cmd
curl -X POST http://localhost:8080/api/mcp -H "Content-Type: application/json" -d "{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"method\":\"tools/list\",\"params\":{}}"
```

调用 `safe_query`：

PowerShell：

```powershell
Invoke-RestMethod -Uri http://localhost:8080/api/mcp -Method POST -ContentType 'application/json' -Body '{"jsonrpc":"2.0","id":"2","method":"tools/call","params":{"name":"safe_query","arguments":{"question":"show total paid amount by customer","tenantId":"tenant_a"}}}'
```

cmd：

```cmd
curl -X POST http://localhost:8080/api/mcp -H "Content-Type: application/json" -d "{\"jsonrpc\":\"2.0\",\"id\":\"2\",\"method\":\"tools/call\",\"params\":{\"name\":\"safe_query\",\"arguments\":{\"question\":\"show total paid amount by customer\",\"tenantId\":\"tenant_a\"}}}"
```

## 测试

后端：

PowerShell：

```powershell
mvn -f omniquery-backend\pom.xml test
```

cmd：

```cmd
mvn -f omniquery-backend\pom.xml test
```

前端：

PowerShell：

```powershell
npm.cmd --prefix omniquery-frontend run build
```

cmd：

```cmd
npm --prefix omniquery-frontend run build
```
