# OmniQuery AI 通用 NL2SQL 引擎重构设计

日期：2026-05-04

## 目标

将 OmniQuery AI 从“大而全的多 Agent 教学业务 Demo”重构为“小而精的通用 NL2SQL 引擎”。项目主要用于简历展示，重点体现 Java/Spring Boot 工程能力、LangChain4j 集成能力、RAG 上下文构建、工具调用边界、SQL 安全网关、ACL 注入和测试覆盖。

重构后的主叙事：

> 一个基于 Spring Boot + LangChain4j 的通用 NL2SQL 查询引擎，支持 Schema/Example RAG、只读工具调用、AST 级 SQL 审计、表列白名单、ACL 条件注入、执行链路 Trace 和可复现测试。

## 非目标

- 不继续做多智能体架构。
- 不绑定教学业务领域。
- 不保留 Electron 桌面端作为主交付。
- 不把 PGVector、MCP、多轮记忆作为当前核心卖点。
- 不让 LLM 直接执行数据库操作。
- 不追求复杂前端体验，React 只保留最小演示界面。

## 技术选择

### 后端

- Java 17+
- Spring Boot
- LangChain4j
- H2 作为内置 Demo 数据库
- Druid SQL Parser 负责 AST 解析、校验和改写
- JUnit 5 覆盖核心链路

### 前端

- React + TypeScript + Vite
- 单页最小 UI
- 展示自然语言输入、生成 SQL、Guard/ACL Trace、查询结果

### LangChain4j 使用边界

采用 LangChain4j 的稳定核心能力：

- AI Services：将 NL2SQL 生成定义为清晰 Java 接口。
- Tools：暴露只读工具，例如 schema 查询、example 查询。
- RAG：通过 ContentRetriever / RetrievalAugmentor 组织 schema 和 golden examples。

不使用 LangChain4j 实验性 SQL 自动执行能力作为主链路。SQL 执行必须经过本项目自己的 Guard、ACL 和 JdbcTemplate。

## 目标架构

后端主链路：

```text
HTTP Query Request
  -> QueryIntentNormalizer
  -> SchemaRetriever / ExampleRetriever
  -> LangChain4j SqlGenerationService
  -> SqlGuard
  -> AclRewriter
  -> JdbcQueryExecutor
  -> AnswerSynthesizer
  -> QueryTrace Response
```

模块边界：

- `api`：REST Controller 和 DTO。
- `engine`：NL2SQL 编排主链路。
- `llm`：LangChain4j AI Service、prompt、tool adapter。
- `rag`：schema/example 文档加载与检索。
- `security`：SQL Guard、SchemaPolicy、AccessPolicy、ACL AST 改写。
- `demo`：内置 H2 schema、seed data、通用样例。
- `frontend`：极简演示页面。

## 数据流

### 1. QueryIntentNormalizer

输入用户自然语言问题，输出单一查询任务：

- 原始问题
- 归一化后的查询目标
- 查询语言或方言配置
- 用户上下文，例如 user id、tenant id、roles

多意图不做 DAG。若用户输入明显包含多个独立查询，系统返回“请拆成单个查询”的错误，而不是假装支持复杂编排。

### 2. Schema / Example RAG

RAG 只负责提供 SQL 生成上下文，不负责执行。

文档类型：

- `SchemaDocument`：表名、列名、类型、业务说明、关联关系、可访问角色。
- `ExampleSqlDocument`：自然语言问题、golden SQL、适用表、说明。

初始实现优先保证可测和可信：

- 若 LangChain4j embedding store 配置简单稳定，则接入内存 embedding store。
- 若成本过高，先实现关键词检索并命名为 `SchemaRetriever`，不伪装成向量检索。

### 3. LangChain4j SqlGenerationService

定义一个窄接口，例如：

```java
interface SqlGenerationService {
    GeneratedSql generate(QueryIntent intent, RetrievedContext context);
}
```

输出必须是结构化对象：

- SQL 草稿
- 使用到的表
- 使用到的列
- 生成说明
- 不确定性提示

不接受 markdown SQL block 作为最终协议。

### 4. 只读工具调用

允许的工具：

- 查询 schema 片段
- 查询 golden examples
- 查询字段解释

禁止的工具：

- 直接执行 SQL
- 写入数据库
- 修改配置
- 访问本地文件系统

工具调用用于提高生成质量，不替代后端安全链路。

### 5. SqlGuard

职责：

- 只允许单条 SELECT。
- 拒绝 INSERT、UPDATE、DELETE、DROP、TRUNCATE、ALTER。
- 拒绝多语句。
- 拒绝未知表。
- 拒绝未知列。
- 拒绝未授权表。
- 强制 LIMIT。
- 可配置最大 JOIN 数。
- 可配置危险函数黑名单。

Guard 输出：

- 原始 SQL
- 格式化 SQL
- 校验结果
- 拦截原因
- 命中的表和列

### 6. ACL 重写

ACL 从硬编码 `user_id` 改成策略驱动。

策略示例：

```json
{
  "orders": {
    "tenantColumn": "tenant_id",
    "ownerColumn": "created_by",
    "roles": {
      "admin": "ALLOW_ALL",
      "user": "tenant_id = :tenantId"
    }
  }
}
```

初始支持：

- 表级 ACL。
- 租户隔离字段。
- 角色级 allow/deny。
- 已存在 WHERE 时追加 `AND`。
- 无 WHERE 时创建 WHERE。
- 保留 LIMIT。

复杂 SQL 的处理策略：

- 第一版只支持普通 SELECT QueryBlock。
- 遇到 UNION、子查询、复杂 CTE，返回不支持原因。
- 后续再扩大 AST 改写能力。

### 7. 执行与响应

SQL 通过 Guard 和 ACL 后，由 JdbcTemplate 执行只读查询。

响应包含：

- 自然语言回答
- 原始 SQL
- 改写后 SQL
- 查询结果
- Trace steps
- Token / latency / cost 估算
- 错误信息

## 前端设计

React 只保留一个最小工作台：

- 顶部输入框。
- 查询按钮。
- SQL 展示区。
- Trace 展示区。
- 表格结果区。

不保留复杂设置抽屉。LLM key、model、db 等通过后端配置文件或环境变量管理。

## 删除和归档清单

- `omniquery-desktop`：移出主路径，可改为 archived 或删除。
- 多 Agent 命名：PlannerAgent、RouterAgent、SynthesizerAgent 改成单链路服务命名。
- 教学业务插件：`GradeQueryTool` 替换为通用 demo schema 工具。
- 伪 RAG：全零向量实现必须删除或更名为关键词检索。
- MCP Client：不进入当前主链路。
- PGVector 配置：不作为第一版能力展示。
- 复杂运行时配置 UI：从前端移除。

## 测试策略

### 单元测试

- SqlGuard 允许合法 SELECT。
- SqlGuard 拒绝 UPDATE / DELETE / DROP / TRUNCATE。
- SqlGuard 拒绝多语句。
- SqlGuard 拒绝未知表。
- SqlGuard 拒绝未知列。
- SqlGuard 自动补 LIMIT。
- AclRewriter 对无 WHERE SQL 注入 ACL。
- AclRewriter 对已有 WHERE SQL 追加 ACL。
- AclRewriter 保留 LIMIT。
- SchemaRetriever 能按表名/字段名召回相关 schema。
- ExampleRetriever 能召回相关 golden SQL。

### 集成测试

- 使用 mock LLM 返回固定 SQL，验证完整链路。
- H2 demo 数据库端到端查询成功。
- 非法 SQL 被拦截且不执行。
- ACL 改写后的 SQL 只返回授权数据。

### 前端验证

- `npm run build` 通过。
- 页面能发起查询并展示 trace。

## 实施顺序

1. 建立新的目标包结构和 DTO。
2. 引入 LangChain4j 依赖，移除 Spring AI 主链路依赖。
3. 建立 demo schema、seed data 和 schema document。
4. 实现 SchemaRetriever / ExampleRetriever。
5. 实现 LangChain4j SqlGenerationService。
6. 重构 SqlGuard 和 ACL 策略模型。
7. 重写 engine 主链路。
8. 精简 API 和 React 页面。
9. 删除/归档 desktop 和未闭环模块。
10. 补齐单元测试和集成测试。
11. 重写 README，突出简历可讲点和启动方式。

## 简历表达建议

项目名：

> OmniQuery AI - 通用 NL2SQL 安全查询引擎

简历描述：

> 基于 Spring Boot 与 LangChain4j 实现通用 NL2SQL 查询引擎，构建 Schema/Example RAG、只读工具调用、SQL AST 安全网关与策略化 ACL 注入机制，支持自然语言到安全 SQL 的生成、审计、改写、执行和链路 Trace，并通过 H2 内置数据集和 JUnit 测试实现可复现演示。

核心亮点：

- LangChain4j AI Services 封装 NL2SQL 生成接口。
- Schema/Example RAG 提升 SQL 生成上下文质量。
- Druid AST 实现 SELECT-only、表列白名单、LIMIT 和危险语句拦截。
- 策略化 ACL 注入保障多租户数据隔离。
- Mock LLM + H2 集成测试保证核心链路可复现。

## 验收标准

- 后端 `mvn test` 通过。
- 前端 `npm run build` 通过。
- 无 Electron 主路径依赖。
- 无教学业务强绑定命名。
- 无伪向量 RAG。
- 非法 SQL 不会执行。
- ACL 改写有测试覆盖。
- README 能让面试官 5 分钟理解项目价值。

