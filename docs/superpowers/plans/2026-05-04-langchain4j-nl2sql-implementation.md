# LangChain4j NL2SQL Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild OmniQuery AI into a small, generic Spring Boot + LangChain4j NL2SQL engine with real schema/example retrieval, read-only tool boundaries, SQL AST guardrails, policy-driven ACL rewriting, minimal React UI, and focused test coverage.

**Architecture:** Keep the backend as a Maven multi-module project, but make the runtime path narrower: API -> engine -> retriever + LangChain4j SQL generator -> SQL guard -> ACL rewriter -> JdbcTemplate executor. Remove multi-agent, teaching-specific, fake vector, Electron, and unclosed MCP/PGVector paths from the main story.

**Tech Stack:** Java 17+, Spring Boot 3.4, LangChain4j, Alibaba Druid SQL parser, H2, JUnit 5, React + TypeScript + Vite.

---

## Source References

- LangChain4j RAG docs: `https://github.com/langchain4j/langchain4j/blob/main/docs/docs/tutorials/rag.md`
- LangChain4j AI Service builder with `contentRetriever`: docs show `AiServices.builder(...).contentRetriever(...)`.
- LangChain4j RAG docs identify `SqlDatabaseContentRetriever` as experimental, so this plan keeps SQL execution inside OmniQuery's own guard and ACL pipeline.

## Working Constraints

- Current workspace root `D:\wookspace\OmniQuery AI` is not a git repository. Commit steps below are written as checkpoints; skip the actual `git commit` command until a repository is initialized.
- PowerShell blocks `npm.ps1` in this environment. Use `npm.cmd` for frontend commands.
- Keep all runtime configuration out of committed secrets. Use env vars for LLM keys.

## Target File Structure

### Backend parent

- Modify: `D:\wookspace\OmniQuery AI\omniquery-backend\pom.xml`
  - Remove Spring AI dependency management.
  - Add LangChain4j version properties.
  - Remove inactive modules from the build once their code has been migrated or archived.

### SDK module

- Modify: `D:\wookspace\OmniQuery AI\omniquery-backend\omniquery-sdk\src\main\java\com\omniquery\sdk\model\SystemConfig.java`
  - Replace broad agent config with compact LLM/database/security settings.
- Modify: `D:\wookspace\OmniQuery AI\omniquery-backend\omniquery-sdk\src\main\java\com\omniquery\sdk\service\SystemConfigService.java`
  - Load config safely and keep env var override behavior predictable.
- Create: `D:\wookspace\OmniQuery AI\omniquery-backend\omniquery-sdk\src\main\java\com\omniquery\sdk\model\UserContext.java`
  - Hold `userId`, `tenantId`, and `roles`.

### RAG module

- Delete: `D:\wookspace\OmniQuery AI\omniquery-backend\omniquery-rag\src\main\java\com\omniquery\rag\store\SimpleInMemVectorStore.java`
- Delete: `D:\wookspace\OmniQuery AI\omniquery-backend\omniquery-rag\src\main\java\com\omniquery\rag\service\SchemaScanner.java`
- Delete: `D:\wookspace\OmniQuery AI\omniquery-backend\omniquery-rag\src\main\java\com\omniquery\rag\service\ExampleSqlStore.java`
- Create: `D:\wookspace\OmniQuery AI\omniquery-backend\omniquery-rag\src\main\java\com\omniquery\rag\model\SchemaDocument.java`
- Create: `D:\wookspace\OmniQuery AI\omniquery-backend\omniquery-rag\src\main\java\com\omniquery\rag\model\ExampleSqlDocument.java`
- Create: `D:\wookspace\OmniQuery AI\omniquery-backend\omniquery-rag\src\main\java\com\omniquery\rag\model\RetrievedContext.java`
- Create: `D:\wookspace\OmniQuery AI\omniquery-backend\omniquery-rag\src\main\java\com\omniquery\rag\repository\DemoKnowledgeBase.java`
- Create: `D:\wookspace\OmniQuery AI\omniquery-backend\omniquery-rag\src\main\java\com\omniquery\rag\service\SchemaRetriever.java`
- Create: `D:\wookspace\OmniQuery AI\omniquery-backend\omniquery-rag\src\main\java\com\omniquery\rag\service\ExampleRetriever.java`
- Create: `D:\wookspace\OmniQuery AI\omniquery-backend\omniquery-rag\src\main\java\com\omniquery\rag\service\RetrievalService.java`
- Test: `D:\wookspace\OmniQuery AI\omniquery-backend\omniquery-rag\src\test\java\com\omniquery\rag\service\SchemaRetrieverTest.java`
- Test: `D:\wookspace\OmniQuery AI\omniquery-backend\omniquery-rag\src\test\java\com\omniquery\rag\service\ExampleRetrieverTest.java`

### Security module

- Replace: `D:\wookspace\OmniQuery AI\omniquery-backend\omniquery-security\src\main\java\com\omniquery\security\DruidSecurityValidator.java`
- Create: `D:\wookspace\OmniQuery AI\omniquery-backend\omniquery-security\src\main\java\com\omniquery\security\model\TablePolicy.java`
- Create: `D:\wookspace\OmniQuery AI\omniquery-backend\omniquery-security\src\main\java\com\omniquery\security\model\SchemaPolicy.java`
- Create: `D:\wookspace\OmniQuery AI\omniquery-backend\omniquery-security\src\main\java\com\omniquery\security\model\AccessPolicy.java`
- Create: `D:\wookspace\OmniQuery AI\omniquery-backend\omniquery-security\src\main\java\com\omniquery\security\model\UserAccessContext.java`
- Create: `D:\wookspace\OmniQuery AI\omniquery-backend\omniquery-security\src\main\java\com\omniquery\security\model\SqlGuardResult.java`
- Create: `D:\wookspace\OmniQuery AI\omniquery-backend\omniquery-security\src\main\java\com\omniquery\security\SqlGuard.java`
- Create: `D:\wookspace\OmniQuery AI\omniquery-backend\omniquery-security\src\main\java\com\omniquery\security\AclRewriter.java`
- Test: `D:\wookspace\OmniQuery AI\omniquery-backend\omniquery-security\src\test\java\com\omniquery\security\SqlGuardTest.java`
- Test: `D:\wookspace\OmniQuery AI\omniquery-backend\omniquery-security\src\test\java\com\omniquery\security\AclRewriterTest.java`

### Core module

- Delete: `D:\wookspace\OmniQuery AI\omniquery-backend\omniquery-core\src\main\java\com\omniquery\core\agent\PlannerAgent.java`
- Delete: `D:\wookspace\OmniQuery AI\omniquery-backend\omniquery-core\src\main\java\com\omniquery\core\agent\RouterAgent.java`
- Delete: `D:\wookspace\OmniQuery AI\omniquery-backend\omniquery-core\src\main\java\com\omniquery\core\agent\SqlAgent.java`
- Delete: `D:\wookspace\OmniQuery AI\omniquery-backend\omniquery-core\src\main\java\com\omniquery\core\agent\SynthesizerAgent.java`
- Delete: `D:\wookspace\OmniQuery AI\omniquery-backend\omniquery-core\src\main\java\com\omniquery\core\mcp\McpClient.java`
- Replace: `D:\wookspace\OmniQuery AI\omniquery-backend\omniquery-core\src\main\java\com\omniquery\core\engine\OrchestratorKernel.java`
- Delete: `D:\wookspace\OmniQuery AI\omniquery-backend\omniquery-core\src\main\java\com\omniquery\core\config\DynamicAiFactory.java`
- Create: `D:\wookspace\OmniQuery AI\omniquery-backend\omniquery-core\src\main\java\com\omniquery\core\model\QueryIntent.java`
- Create: `D:\wookspace\OmniQuery AI\omniquery-backend\omniquery-core\src\main\java\com\omniquery\core\model\GeneratedSql.java`
- Create: `D:\wookspace\OmniQuery AI\omniquery-backend\omniquery-core\src\main\java\com\omniquery\core\model\QueryTrace.java`
- Create: `D:\wookspace\OmniQuery AI\omniquery-backend\omniquery-core\src\main\java\com\omniquery\core\model\QueryResponse.java`
- Create: `D:\wookspace\OmniQuery AI\omniquery-backend\omniquery-core\src\main\java\com\omniquery\core\service\QueryIntentNormalizer.java`
- Create: `D:\wookspace\OmniQuery AI\omniquery-backend\omniquery-core\src\main\java\com\omniquery\core\service\JdbcQueryExecutor.java`
- Create: `D:\wookspace\OmniQuery AI\omniquery-backend\omniquery-core\src\main\java\com\omniquery\core\llm\SqlGenerationService.java`
- Create: `D:\wookspace\OmniQuery AI\omniquery-backend\omniquery-core\src\main\java\com\omniquery\core\llm\LangChain4jConfig.java`
- Create: `D:\wookspace\OmniQuery AI\omniquery-backend\omniquery-core\src\main\java\com\omniquery\core\llm\SchemaTools.java`
- Create: `D:\wookspace\OmniQuery AI\omniquery-backend\omniquery-core\src\main\java\com\omniquery\core\llm\FallbackSqlGenerationService.java`
- Test: `D:\wookspace\OmniQuery AI\omniquery-backend\omniquery-core\src\test\java\com\omniquery\core\engine\OrchestratorKernelTest.java`

### API module

- Modify: `D:\wookspace\OmniQuery AI\omniquery-backend\omniquery-api\src\main\java\com\omniquery\api\controller\QueryController.java`
- Modify: `D:\wookspace\OmniQuery AI\omniquery-backend\omniquery-api\src\main\resources\application.yml`
- Create: `D:\wookspace\OmniQuery AI\omniquery-backend\omniquery-api\src\main\resources\schema.sql`
- Create: `D:\wookspace\OmniQuery AI\omniquery-backend\omniquery-api\src\main\resources\data.sql`
- Modify: `D:\wookspace\OmniQuery AI\omniquery-backend\omniquery-api\src\test\java\com\omniquery\api\OmniIntegrityTest.java`

### Frontend

- Replace: `D:\wookspace\OmniQuery AI\omniquery-frontend\src\App.tsx`
- Replace: `D:\wookspace\OmniQuery AI\omniquery-frontend\src\App.css`
- Replace: `D:\wookspace\OmniQuery AI\omniquery-frontend\src\index.css`
- Delete: `D:\wookspace\OmniQuery AI\omniquery-frontend\src\components\Config\SettingsDrawer.tsx`
- Keep: `D:\wookspace\OmniQuery AI\omniquery-frontend\package.json`

### Desktop

- Move or delete: `D:\wookspace\OmniQuery AI\omniquery-desktop`
- If deletion is chosen, document it in README.

---

## Task 1: Introduce Generic Query Models

**Files:**
- Create: `omniquery-backend/omniquery-core/src/main/java/com/omniquery/core/model/QueryIntent.java`
- Create: `omniquery-backend/omniquery-core/src/main/java/com/omniquery/core/model/GeneratedSql.java`
- Create: `omniquery-backend/omniquery-core/src/main/java/com/omniquery/core/model/QueryTrace.java`
- Create: `omniquery-backend/omniquery-core/src/main/java/com/omniquery/core/model/QueryResponse.java`
- Create: `omniquery-backend/omniquery-sdk/src/main/java/com/omniquery/sdk/model/UserContext.java`

- [ ] **Step 1: Create `UserContext`**

```java
package com.omniquery.sdk.model;

import java.util.List;

public record UserContext(
    String userId,
    String tenantId,
    List<String> roles
) {
    public static UserContext defaultUser() {
        return new UserContext("default-user", "tenant_a", List.of("user"));
    }
}
```

- [ ] **Step 2: Create `QueryIntent`**

```java
package com.omniquery.core.model;

import com.omniquery.sdk.model.UserContext;

public record QueryIntent(
    String originalQuestion,
    String normalizedQuestion,
    String dialect,
    UserContext userContext
) {}
```

- [ ] **Step 3: Create `GeneratedSql`**

```java
package com.omniquery.core.model;

import java.util.List;

public record GeneratedSql(
    String sql,
    List<String> tables,
    List<String> columns,
    String explanation
) {}
```

- [ ] **Step 4: Create `QueryTrace`**

```java
package com.omniquery.core.model;

public record QueryTrace(
    String phase,
    String message,
    Object detail
) {}
```

- [ ] **Step 5: Create `QueryResponse`**

```java
package com.omniquery.core.model;

import java.util.List;
import java.util.Map;

public record QueryResponse(
    boolean success,
    String answer,
    String rawSql,
    String guardedSql,
    List<Map<String, Object>> rows,
    String error,
    List<QueryTrace> trace
) {}
```

- [ ] **Step 6: Compile core and sdk**

Run:

```powershell
mvn -pl omniquery-sdk,omniquery-core -am test
```

Expected: `BUILD SUCCESS`.

---

## Task 2: Replace Fake Vector RAG With Explicit Schema and Example Retrieval

**Files:**
- Create: `omniquery-backend/omniquery-rag/src/main/java/com/omniquery/rag/model/SchemaDocument.java`
- Create: `omniquery-backend/omniquery-rag/src/main/java/com/omniquery/rag/model/ExampleSqlDocument.java`
- Create: `omniquery-backend/omniquery-rag/src/main/java/com/omniquery/rag/model/RetrievedContext.java`
- Create: `omniquery-backend/omniquery-rag/src/main/java/com/omniquery/rag/repository/DemoKnowledgeBase.java`
- Create: `omniquery-backend/omniquery-rag/src/main/java/com/omniquery/rag/service/SchemaRetriever.java`
- Create: `omniquery-backend/omniquery-rag/src/main/java/com/omniquery/rag/service/ExampleRetriever.java`
- Create: `omniquery-backend/omniquery-rag/src/main/java/com/omniquery/rag/service/RetrievalService.java`
- Test: `omniquery-backend/omniquery-rag/src/test/java/com/omniquery/rag/service/SchemaRetrieverTest.java`
- Test: `omniquery-backend/omniquery-rag/src/test/java/com/omniquery/rag/service/ExampleRetrieverTest.java`
- Delete after tests pass: `SimpleInMemVectorStore.java`, `SchemaScanner.java`, `ExampleSqlStore.java`

- [ ] **Step 1: Write `SchemaRetrieverTest`**

```java
package com.omniquery.rag.service;

import com.omniquery.rag.repository.DemoKnowledgeBase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SchemaRetrieverTest {

    @Test
    void retrievesRelevantSchemaByTableAndColumnWords() {
        SchemaRetriever retriever = new SchemaRetriever(new DemoKnowledgeBase());

        var results = retriever.retrieve("show recent orders and customer names", 3);

        assertFalse(results.isEmpty());
        assertTrue(results.stream().anyMatch(doc -> doc.tableName().equals("orders")));
        assertTrue(results.stream().anyMatch(doc -> doc.tableName().equals("customers")));
    }
}
```

- [ ] **Step 2: Write `ExampleRetrieverTest`**

```java
package com.omniquery.rag.service;

import com.omniquery.rag.repository.DemoKnowledgeBase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExampleRetrieverTest {

    @Test
    void retrievesGoldenSqlExamplesForRevenueQuestion() {
        ExampleRetriever retriever = new ExampleRetriever(new DemoKnowledgeBase());

        var results = retriever.retrieve("total paid amount by customer", 2);

        assertFalse(results.isEmpty());
        assertTrue(results.get(0).sql().toLowerCase().contains("sum"));
    }
}
```

- [ ] **Step 3: Run RAG tests and verify failure**

Run:

```powershell
mvn -pl omniquery-rag -Dtest=SchemaRetrieverTest,ExampleRetrieverTest test
```

Expected: FAIL because retriever classes do not exist.

- [ ] **Step 4: Create RAG model records**

```java
package com.omniquery.rag.model;

import java.util.List;

public record SchemaDocument(
    String tableName,
    String description,
    List<String> columns,
    List<String> relationships,
    List<String> roles
) {
    public String searchableText() {
        return String.join(" ", tableName, description, String.join(" ", columns), String.join(" ", relationships));
    }
}
```

```java
package com.omniquery.rag.model;

import java.util.List;

public record ExampleSqlDocument(
    String question,
    String sql,
    List<String> tables,
    String explanation
) {
    public String searchableText() {
        return String.join(" ", question, sql, String.join(" ", tables), explanation);
    }
}
```

```java
package com.omniquery.rag.model;

import java.util.List;

public record RetrievedContext(
    List<SchemaDocument> schemas,
    List<ExampleSqlDocument> examples
) {
    public String toPromptContext() {
        StringBuilder builder = new StringBuilder();
        builder.append("SCHEMA:\n");
        schemas.forEach(schema -> builder
            .append("- Table ").append(schema.tableName())
            .append(": ").append(schema.description())
            .append("; columns=").append(schema.columns())
            .append("; relationships=").append(schema.relationships())
            .append("\n"));
        builder.append("EXAMPLES:\n");
        examples.forEach(example -> builder
            .append("- Q: ").append(example.question())
            .append("\n  SQL: ").append(example.sql())
            .append("\n"));
        return builder.toString();
    }
}
```

- [ ] **Step 5: Create `DemoKnowledgeBase`**

```java
package com.omniquery.rag.repository;

import com.omniquery.rag.model.ExampleSqlDocument;
import com.omniquery.rag.model.SchemaDocument;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class DemoKnowledgeBase {

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
```

- [ ] **Step 6: Create keyword retrievers**

```java
package com.omniquery.rag.service;

import com.omniquery.rag.model.SchemaDocument;
import com.omniquery.rag.repository.DemoKnowledgeBase;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class SchemaRetriever {

    private final DemoKnowledgeBase knowledgeBase;

    public SchemaRetriever(DemoKnowledgeBase knowledgeBase) {
        this.knowledgeBase = knowledgeBase;
    }

    public List<SchemaDocument> retrieve(String question, int limit) {
        String normalized = question.toLowerCase();
        return knowledgeBase.schemas().stream()
            .sorted(Comparator.comparingInt((SchemaDocument doc) -> score(doc.searchableText(), normalized)).reversed())
            .filter(doc -> score(doc.searchableText(), normalized) > 0)
            .limit(limit)
            .toList();
    }

    private int score(String text, String question) {
        String lower = text.toLowerCase();
        int score = 0;
        for (String token : question.split("\\W+")) {
            if (!token.isBlank() && lower.contains(token)) {
                score++;
            }
        }
        return score;
    }
}
```

```java
package com.omniquery.rag.service;

import com.omniquery.rag.model.ExampleSqlDocument;
import com.omniquery.rag.repository.DemoKnowledgeBase;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class ExampleRetriever {

    private final DemoKnowledgeBase knowledgeBase;

    public ExampleRetriever(DemoKnowledgeBase knowledgeBase) {
        this.knowledgeBase = knowledgeBase;
    }

    public List<ExampleSqlDocument> retrieve(String question, int limit) {
        String normalized = question.toLowerCase();
        return knowledgeBase.examples().stream()
            .sorted(Comparator.comparingInt((ExampleSqlDocument doc) -> score(doc.searchableText(), normalized)).reversed())
            .filter(doc -> score(doc.searchableText(), normalized) > 0)
            .limit(limit)
            .toList();
    }

    private int score(String text, String question) {
        String lower = text.toLowerCase();
        int score = 0;
        for (String token : question.split("\\W+")) {
            if (!token.isBlank() && lower.contains(token)) {
                score++;
            }
        }
        return score;
    }
}
```

```java
package com.omniquery.rag.service;

import com.omniquery.rag.model.RetrievedContext;
import org.springframework.stereotype.Service;

@Service
public class RetrievalService {

    private final SchemaRetriever schemaRetriever;
    private final ExampleRetriever exampleRetriever;

    public RetrievalService(SchemaRetriever schemaRetriever, ExampleRetriever exampleRetriever) {
        this.schemaRetriever = schemaRetriever;
        this.exampleRetriever = exampleRetriever;
    }

    public RetrievedContext retrieve(String question) {
        return new RetrievedContext(
            schemaRetriever.retrieve(question, 5),
            exampleRetriever.retrieve(question, 3)
        );
    }
}
```

- [ ] **Step 7: Run RAG tests**

Run:

```powershell
mvn -pl omniquery-rag test
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 8: Delete fake vector files**

Use `apply_patch` delete hunks for:

```text
omniquery-backend/omniquery-rag/src/main/java/com/omniquery/rag/store/SimpleInMemVectorStore.java
omniquery-backend/omniquery-rag/src/main/java/com/omniquery/rag/service/SchemaScanner.java
omniquery-backend/omniquery-rag/src/main/java/com/omniquery/rag/service/ExampleSqlStore.java
```

- [ ] **Step 9: Remove Spring AI dependencies from `omniquery-rag/pom.xml`**

Replace the dependencies block with:

```xml
<dependencies>
    <dependency>
        <groupId>com.omniquery</groupId>
        <artifactId>omniquery-sdk</artifactId>
        <version>${project.version}</version>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter</artifactId>
    </dependency>
</dependencies>
```

- [ ] **Step 10: Run backend tests**

Run:

```powershell
mvn test
```

Expected: failures in core/api are acceptable at this point if they reference removed `SchemaScanner` or `SimpleInMemVectorStore`; RAG module must pass.

---

## Task 3: Build Policy-Driven SQL Guard and ACL Rewriter

**Files:**
- Create security models listed in the Target File Structure.
- Replace `DruidSecurityValidator.java` with compatibility wrapper or delete after callers are migrated.
- Test `SqlGuardTest.java` and `AclRewriterTest.java`.

- [ ] **Step 1: Write `SqlGuardTest`**

```java
package com.omniquery.security;

import com.omniquery.security.model.SchemaPolicy;
import com.omniquery.security.model.TablePolicy;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SqlGuardTest {

    private final SchemaPolicy policy = new SchemaPolicy(Map.of(
        "customers", new TablePolicy("customers", Set.of("id", "name", "tenant_id", "created_by"), Set.of("admin", "user")),
        "orders", new TablePolicy("orders", Set.of("id", "customer_id", "status", "total_amount", "tenant_id", "created_by", "created_at"), Set.of("admin", "user"))
    ));

    @Test
    void allowsSelectAndAddsLimit() {
        SqlGuard guard = new SqlGuard(policy, 100);
        var result = guard.validate("SELECT id, status FROM orders", Set.of("user"));
        assertTrue(result.allowed());
        assertTrue(result.sql().toUpperCase().contains("LIMIT 100"));
    }

    @Test
    void rejectsMutations() {
        SqlGuard guard = new SqlGuard(policy, 100);
        var result = guard.validate("DELETE FROM orders WHERE id = 1", Set.of("user"));
        assertFalse(result.allowed());
        assertTrue(result.reason().contains("Only SELECT"));
    }

    @Test
    void rejectsUnknownColumns() {
        SqlGuard guard = new SqlGuard(policy, 100);
        var result = guard.validate("SELECT password FROM customers", Set.of("user"));
        assertFalse(result.allowed());
        assertTrue(result.reason().contains("Unknown column"));
    }
}
```

- [ ] **Step 2: Write `AclRewriterTest`**

```java
package com.omniquery.security;

import com.omniquery.security.model.AccessPolicy;
import com.omniquery.security.model.UserAccessContext;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AclRewriterTest {

    @Test
    void injectsAclWhenWhereIsMissing() {
        AclRewriter rewriter = new AclRewriter(new AccessPolicy(Map.of("orders", "tenant_id")));

        String sql = rewriter.rewrite(
            "SELECT id, status FROM orders LIMIT 100",
            new UserAccessContext("u1", "tenant_a", Set.of("user"))
        );

        assertTrue(sql.toLowerCase().contains("where tenant_id = 'tenant_a'"));
        assertTrue(sql.toLowerCase().contains("limit 100"));
    }

    @Test
    void appendsAclToExistingWhere() {
        AclRewriter rewriter = new AclRewriter(new AccessPolicy(Map.of("orders", "tenant_id")));

        String sql = rewriter.rewrite(
            "SELECT id FROM orders WHERE status = 'PAID' LIMIT 100",
            new UserAccessContext("u1", "tenant_a", Set.of("user"))
        );

        assertTrue(sql.toLowerCase().contains("status = 'PAID'".toLowerCase()));
        assertTrue(sql.toLowerCase().contains("and tenant_id = 'tenant_a'"));
    }

    @Test
    void adminBypassesAcl() {
        AclRewriter rewriter = new AclRewriter(new AccessPolicy(Map.of("orders", "tenant_id")));

        String sql = rewriter.rewrite(
            "SELECT id FROM orders LIMIT 100",
            new UserAccessContext("admin", "tenant_a", Set.of("admin"))
        );

        assertFalse(sql.toLowerCase().contains("tenant_id ="));
    }
}
```

- [ ] **Step 3: Run security tests and verify failure**

Run:

```powershell
mvn -pl omniquery-security -Dtest=SqlGuardTest,AclRewriterTest test
```

Expected: FAIL because new security classes do not exist.

- [ ] **Step 4: Create security model records**

```java
package com.omniquery.security.model;

import java.util.Set;

public record TablePolicy(String tableName, Set<String> columns, Set<String> roles) {}
```

```java
package com.omniquery.security.model;

import java.util.Map;

public record SchemaPolicy(Map<String, TablePolicy> tables) {}
```

```java
package com.omniquery.security.model;

import java.util.Map;

public record AccessPolicy(Map<String, String> tenantColumns) {}
```

```java
package com.omniquery.security.model;

import java.util.Set;

public record UserAccessContext(String userId, String tenantId, Set<String> roles) {
    public boolean isAdmin() {
        return roles != null && roles.contains("admin");
    }
}
```

```java
package com.omniquery.security.model;

import java.util.Set;

public record SqlGuardResult(
    boolean allowed,
    String sql,
    String reason,
    Set<String> tables,
    Set<String> columns
) {}
```

- [ ] **Step 5: Implement `SqlGuard`**

Implementation rules:

- Parse with `SQLUtils.parseStatements(sql, JdbcConstants.MYSQL)`.
- Reject if statement count is not exactly one.
- Reject if statement is not `SQLSelectStatement`.
- For `SQLSelectQueryBlock`, extract table names from `getFrom()`.
- For first implementation, support `SQLExprTableSource` and `SQLJoinTableSource`.
- Reject unknown tables.
- Reject selected columns when they are explicit unknown identifiers.
- Add `LIMIT maxRows` when missing.

Use this class shape:

```java
package com.omniquery.security;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLLimit;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.SQLNumberExpr;
import com.alibaba.druid.sql.ast.statement.*;
import com.alibaba.druid.util.JdbcConstants;
import com.omniquery.security.model.SchemaPolicy;
import com.omniquery.security.model.SqlGuardResult;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SqlGuard {

    private final SchemaPolicy schemaPolicy;
    private final int maxRows;

    public SqlGuard(SchemaPolicy schemaPolicy, int maxRows) {
        this.schemaPolicy = schemaPolicy;
        this.maxRows = maxRows;
    }

    public SqlGuardResult validate(String sql, Set<String> roles) {
        List<SQLStatement> statements = SQLUtils.parseStatements(sql, JdbcConstants.MYSQL);
        if (statements.size() != 1) {
            return denied("Expected exactly one SQL statement");
        }
        if (!(statements.get(0) instanceof SQLSelectStatement selectStatement)) {
            return denied("Only SELECT statements are allowed");
        }
        if (!(selectStatement.getSelect().getQuery() instanceof SQLSelectQueryBlock queryBlock)) {
            return denied("Only simple SELECT query blocks are supported");
        }

        Set<String> tables = new HashSet<>();
        collectTables(queryBlock.getFrom(), tables);
        for (String table : tables) {
            var tablePolicy = schemaPolicy.tables().get(table.toLowerCase());
            if (tablePolicy == null) {
                return denied("Unknown table: " + table);
            }
            boolean roleAllowed = roles.stream().anyMatch(tablePolicy.roles()::contains);
            if (!roleAllowed) {
                return denied("Role cannot access table: " + table);
            }
        }

        Set<String> columns = new HashSet<>();
        queryBlock.getSelectList().forEach(item -> {
            String expr = item.getExpr().toString();
            if (!expr.equals("*") && expr.matches("[a-zA-Z_][a-zA-Z0-9_\\.]*")) {
                columns.add(expr.contains(".") ? expr.substring(expr.indexOf('.') + 1) : expr);
            }
        });
        for (String column : columns) {
            boolean known = tables.stream()
                .map(table -> schemaPolicy.tables().get(table.toLowerCase()))
                .anyMatch(policy -> policy.columns().contains(column.toLowerCase()));
            if (!known) {
                return denied("Unknown column: " + column);
            }
        }

        if (queryBlock.getLimit() == null) {
            queryBlock.setLimit(new SQLLimit(new SQLNumberExpr(maxRows)));
        }
        String guardedSql = SQLUtils.toSQLString(statements, JdbcConstants.MYSQL);
        return new SqlGuardResult(true, guardedSql, null, tables, columns);
    }

    private SqlGuardResult denied(String reason) {
        return new SqlGuardResult(false, null, reason, Set.of(), Set.of());
    }

    private void collectTables(SQLTableSource source, Set<String> tables) {
        if (source instanceof SQLExprTableSource tableSource) {
            tables.add(tableSource.getTableName().toLowerCase());
        } else if (source instanceof SQLJoinTableSource join) {
            collectTables(join.getLeft(), tables);
            collectTables(join.getRight(), tables);
        }
    }
}
```

- [ ] **Step 6: Implement `AclRewriter`**

Use Druid AST to append `tenant_id = :tenantId` semantics. The generated SQL will contain a quoted tenant value in this first version because JdbcTemplate execution will run the final SQL after validation.

```java
package com.omniquery.security;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.SQLBinaryOpExpr;
import com.alibaba.druid.sql.ast.expr.SQLBinaryOperator;
import com.alibaba.druid.sql.ast.expr.SQLCharExpr;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.statement.SQLExprTableSource;
import com.alibaba.druid.sql.ast.statement.SQLSelectQueryBlock;
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;
import com.alibaba.druid.util.JdbcConstants;
import com.omniquery.security.model.AccessPolicy;
import com.omniquery.security.model.UserAccessContext;

import java.util.List;

public class AclRewriter {

    private final AccessPolicy accessPolicy;

    public AclRewriter(AccessPolicy accessPolicy) {
        this.accessPolicy = accessPolicy;
    }

    public String rewrite(String sql, UserAccessContext user) {
        if (user.isAdmin()) {
            return sql;
        }
        List<SQLStatement> statements = SQLUtils.parseStatements(sql, JdbcConstants.MYSQL);
        SQLSelectStatement select = (SQLSelectStatement) statements.get(0);
        SQLSelectQueryBlock queryBlock = (SQLSelectQueryBlock) select.getSelect().getQuery();
        if (!(queryBlock.getFrom() instanceof SQLExprTableSource tableSource)) {
            return sql;
        }
        String table = tableSource.getTableName().toLowerCase();
        String tenantColumn = accessPolicy.tenantColumns().get(table);
        if (tenantColumn == null) {
            return sql;
        }
        SQLBinaryOpExpr aclCondition = new SQLBinaryOpExpr(
            new SQLIdentifierExpr(tenantColumn),
            SQLBinaryOperator.Equality,
            new SQLCharExpr(user.tenantId())
        );
        if (queryBlock.getWhere() == null) {
            queryBlock.setWhere(aclCondition);
        } else {
            queryBlock.setWhere(new SQLBinaryOpExpr(queryBlock.getWhere(), SQLBinaryOperator.BooleanAnd, aclCondition));
        }
        return SQLUtils.toSQLString(statements, JdbcConstants.MYSQL);
    }
}
```

- [ ] **Step 7: Run security tests**

Run:

```powershell
mvn -pl omniquery-security test
```

Expected: `BUILD SUCCESS`.

---

## Task 4: Add H2 Generic Demo Dataset

**Files:**
- Create: `omniquery-backend/omniquery-api/src/main/resources/schema.sql`
- Create: `omniquery-backend/omniquery-api/src/main/resources/data.sql`
- Modify: `omniquery-backend/omniquery-api/src/main/resources/application.yml`

- [ ] **Step 1: Create schema**

```sql
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS customers;

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
```

- [ ] **Step 2: Create seed data**

```sql
INSERT INTO customers (id, name, tenant_id, created_by) VALUES
(1, 'Acme Corp', 'tenant_a', 'u1'),
(2, 'Northwind', 'tenant_a', 'u2'),
(3, 'Globex', 'tenant_b', 'u3');

INSERT INTO orders (id, customer_id, status, total_amount, tenant_id, created_by, created_at) VALUES
(1001, 1, 'PAID', 1200.00, 'tenant_a', 'u1', TIMESTAMP '2026-01-10 10:00:00'),
(1002, 1, 'PENDING', 300.00, 'tenant_a', 'u1', TIMESTAMP '2026-01-12 11:30:00'),
(1003, 2, 'PAID', 800.00, 'tenant_a', 'u2', TIMESTAMP '2026-01-15 09:20:00'),
(1004, 3, 'PAID', 9999.00, 'tenant_b', 'u3', TIMESTAMP '2026-01-16 14:00:00');
```

- [ ] **Step 3: Configure H2 startup**

Use this `application.yml` shape:

```yaml
spring:
  application:
    name: omniquery-api
  datasource:
    url: jdbc:h2:mem:omniquery;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1
    username: sa
    password:
    driver-class-name: org.h2.Driver
  sql:
    init:
      mode: always

omniquery:
  llm:
    provider: openai-compatible
    api-key: ${OPENAI_API_KEY:}
    base-url: ${OPENAI_API_BASE_URL:https://api.openai.com/v1}
    model-name: ${OPENAI_MODEL:gpt-4o-mini}
```

- [ ] **Step 4: Run API tests**

Run:

```powershell
mvn -pl omniquery-api -am test
```

Expected: failures referencing removed Spring AI classes may remain until Task 5; H2 schema must not cause SQL init errors.

---

## Task 5: Introduce LangChain4j SQL Generation Boundary

**Files:**
- Modify: `omniquery-backend/pom.xml`
- Modify: `omniquery-backend/omniquery-core/pom.xml`
- Create: `omniquery-backend/omniquery-core/src/main/java/com/omniquery/core/llm/SqlGenerationService.java`
- Create: `omniquery-backend/omniquery-core/src/main/java/com/omniquery/core/llm/LangChain4jConfig.java`
- Create: `omniquery-backend/omniquery-core/src/main/java/com/omniquery/core/llm/SchemaTools.java`
- Create: `omniquery-backend/omniquery-core/src/main/java/com/omniquery/core/llm/FallbackSqlGenerationService.java`

- [ ] **Step 1: Update parent POM properties and dependency management**

Remove Spring AI BOM and repositories. Add:

```xml
<properties>
    <java.version>17</java.version>
    <langchain4j.version>1.3.0</langchain4j.version>
    <druid.version>1.2.23</druid.version>
    <lombok.version>1.18.30</lombok.version>
</properties>
```

Add dependencies directly in child modules instead of importing Spring AI BOM.

- [ ] **Step 2: Update core POM dependencies**

Use this dependency list:

```xml
<dependencies>
    <dependency>
        <groupId>com.omniquery</groupId>
        <artifactId>omniquery-sdk</artifactId>
        <version>${project.version}</version>
    </dependency>
    <dependency>
        <groupId>com.omniquery</groupId>
        <artifactId>omniquery-security</artifactId>
        <version>${project.version}</version>
    </dependency>
    <dependency>
        <groupId>com.omniquery</groupId>
        <artifactId>omniquery-rag</artifactId>
        <version>${project.version}</version>
    </dependency>
    <dependency>
        <groupId>dev.langchain4j</groupId>
        <artifactId>langchain4j</artifactId>
        <version>${langchain4j.version}</version>
    </dependency>
    <dependency>
        <groupId>dev.langchain4j</groupId>
        <artifactId>langchain4j-open-ai</artifactId>
        <version>${langchain4j.version}</version>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-jdbc</artifactId>
    </dependency>
</dependencies>
```

- [ ] **Step 3: Create SQL generation interface**

```java
package com.omniquery.core.llm;

import com.omniquery.core.model.GeneratedSql;
import com.omniquery.core.model.QueryIntent;
import com.omniquery.rag.model.RetrievedContext;

public interface SqlGenerationService {
    GeneratedSql generate(QueryIntent intent, RetrievedContext context);
}
```

- [ ] **Step 4: Create read-only schema tools**

```java
package com.omniquery.core.llm;

import com.omniquery.rag.service.ExampleRetriever;
import com.omniquery.rag.service.SchemaRetriever;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

@Component
public class SchemaTools {

    private final SchemaRetriever schemaRetriever;
    private final ExampleRetriever exampleRetriever;

    public SchemaTools(SchemaRetriever schemaRetriever, ExampleRetriever exampleRetriever) {
        this.schemaRetriever = schemaRetriever;
        this.exampleRetriever = exampleRetriever;
    }

    @Tool("Find relevant database schema fragments for a natural language question. This tool is read-only.")
    public String findSchema(String question) {
        return schemaRetriever.retrieve(question, 5).toString();
    }

    @Tool("Find relevant NL2SQL examples for a natural language question. This tool is read-only.")
    public String findExamples(String question) {
        return exampleRetriever.retrieve(question, 3).toString();
    }
}
```

- [ ] **Step 5: Create fallback deterministic generator for tests and no-key local use**

```java
package com.omniquery.core.llm;

import com.omniquery.core.model.GeneratedSql;
import com.omniquery.core.model.QueryIntent;
import com.omniquery.rag.model.RetrievedContext;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Primary
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
```

- [ ] **Step 6: Create LangChain4j config as inactive until explicitly enabled**

Use `@Profile("llm")` so tests are deterministic:

```java
package com.omniquery.core.llm;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("llm")
public class LangChain4jConfig {

    @Bean
    ChatModel chatModel(
        @Value("${omniquery.llm.api-key}") String apiKey,
        @Value("${omniquery.llm.base-url}") String baseUrl,
        @Value("${omniquery.llm.model-name}") String modelName
    ) {
        return OpenAiChatModel.builder()
            .apiKey(apiKey)
            .baseUrl(baseUrl)
            .modelName(modelName)
            .temperature(0.0)
            .build();
    }
}
```

- [ ] **Step 7: Run core compile**

Run:

```powershell
mvn -pl omniquery-core -am test
```

Expected: compile failures remain only for old deleted agents if they are still referenced; resolve in Task 6.

---

## Task 6: Rewrite Engine as Single NL2SQL Pipeline

**Files:**
- Replace: `omniquery-backend/omniquery-core/src/main/java/com/omniquery/core/engine/OrchestratorKernel.java`
- Create: `omniquery-backend/omniquery-core/src/main/java/com/omniquery/core/service/QueryIntentNormalizer.java`
- Create: `omniquery-backend/omniquery-core/src/main/java/com/omniquery/core/service/JdbcQueryExecutor.java`
- Test: `omniquery-backend/omniquery-core/src/test/java/com/omniquery/core/engine/OrchestratorKernelTest.java`

- [ ] **Step 1: Write engine integration test with deterministic generator**

```java
package com.omniquery.core.engine;

import com.omniquery.core.llm.FallbackSqlGenerationService;
import com.omniquery.core.service.JdbcQueryExecutor;
import com.omniquery.core.service.QueryIntentNormalizer;
import com.omniquery.rag.repository.DemoKnowledgeBase;
import com.omniquery.rag.service.ExampleRetriever;
import com.omniquery.rag.service.RetrievalService;
import com.omniquery.rag.service.SchemaRetriever;
import com.omniquery.security.AclRewriter;
import com.omniquery.security.SqlGuard;
import com.omniquery.security.model.AccessPolicy;
import com.omniquery.security.model.SchemaPolicy;
import com.omniquery.security.model.TablePolicy;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class OrchestratorKernelTest {

    @Test
    void runsEndToEndWithAclFilteredRows() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource("jdbc:h2:mem:kernel_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1", "sa", "");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE customers (id BIGINT PRIMARY KEY, name VARCHAR(100), tenant_id VARCHAR(50), created_by VARCHAR(50))");
        jdbc.execute("CREATE TABLE orders (id BIGINT PRIMARY KEY, customer_id BIGINT, status VARCHAR(30), total_amount DECIMAL(12,2), tenant_id VARCHAR(50), created_by VARCHAR(50), created_at TIMESTAMP)");
        jdbc.execute("INSERT INTO customers VALUES (1, 'Acme Corp', 'tenant_a', 'u1'), (2, 'Globex', 'tenant_b', 'u2')");
        jdbc.execute("INSERT INTO orders VALUES (1001, 1, 'PAID', 1200.00, 'tenant_a', 'u1', CURRENT_TIMESTAMP), (1002, 2, 'PAID', 9999.00, 'tenant_b', 'u2', CURRENT_TIMESTAMP)");

        DemoKnowledgeBase kb = new DemoKnowledgeBase();
        RetrievalService retrieval = new RetrievalService(new SchemaRetriever(kb), new ExampleRetriever(kb));
        SchemaPolicy schemaPolicy = new SchemaPolicy(Map.of(
            "customers", new TablePolicy("customers", Set.of("id", "name", "tenant_id", "created_by"), Set.of("admin", "user")),
            "orders", new TablePolicy("orders", Set.of("id", "customer_id", "status", "total_amount", "tenant_id", "created_by", "created_at"), Set.of("admin", "user"))
        ));
        OrchestratorKernel kernel = new OrchestratorKernel(
            new QueryIntentNormalizer(),
            retrieval,
            new FallbackSqlGenerationService(),
            new SqlGuard(schemaPolicy, 100),
            new AclRewriter(new AccessPolicy(Map.of("orders", "tenant_id"))),
            new JdbcQueryExecutor(jdbc)
        );

        var response = kernel.handle("tenant_a", "show total paid amount by customer");

        assertTrue(response.success(), response.error());
        assertEquals(1, response.rows().size());
        assertTrue(response.guardedSql().toLowerCase().contains("tenant_id"));
        assertFalse(response.rows().toString().contains("9999"));
    }
}
```

- [ ] **Step 2: Create `QueryIntentNormalizer`**

```java
package com.omniquery.core.service;

import com.omniquery.core.model.QueryIntent;
import com.omniquery.sdk.model.UserContext;
import org.springframework.stereotype.Service;

public class QueryIntentNormalizer {

    public QueryIntent normalize(String tenantId, String question) {
        UserContext user = new UserContext("default-user", tenantId, java.util.List.of("user"));
        return new QueryIntent(question, question.trim(), "mysql", user);
    }
}
```

- [ ] **Step 3: Create `JdbcQueryExecutor`**

```java
package com.omniquery.core.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class JdbcQueryExecutor {

    private final JdbcTemplate jdbcTemplate;

    public JdbcQueryExecutor(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> query(String sql) {
        return jdbcTemplate.queryForList(sql);
    }
}
```

- [ ] **Step 4: Replace `OrchestratorKernel`**

```java
package com.omniquery.core.engine;

import com.omniquery.core.llm.SqlGenerationService;
import com.omniquery.core.model.QueryResponse;
import com.omniquery.core.model.QueryTrace;
import com.omniquery.core.service.JdbcQueryExecutor;
import com.omniquery.core.service.QueryIntentNormalizer;
import com.omniquery.rag.service.RetrievalService;
import com.omniquery.security.AclRewriter;
import com.omniquery.security.SqlGuard;
import com.omniquery.security.model.UserAccessContext;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class OrchestratorKernel {

    private final QueryIntentNormalizer normalizer;
    private final RetrievalService retrievalService;
    private final SqlGenerationService sqlGenerationService;
    private final SqlGuard sqlGuard;
    private final AclRewriter aclRewriter;
    private final JdbcQueryExecutor queryExecutor;

    public OrchestratorKernel(
        QueryIntentNormalizer normalizer,
        RetrievalService retrievalService,
        SqlGenerationService sqlGenerationService,
        SqlGuard sqlGuard,
        AclRewriter aclRewriter,
        JdbcQueryExecutor queryExecutor
    ) {
        this.normalizer = normalizer;
        this.retrievalService = retrievalService;
        this.sqlGenerationService = sqlGenerationService;
        this.sqlGuard = sqlGuard;
        this.aclRewriter = aclRewriter;
        this.queryExecutor = queryExecutor;
    }

    public QueryResponse handle(String tenantId, String question) {
        List<QueryTrace> trace = new ArrayList<>();
        try {
            var intent = normalizer.normalize(tenantId, question);
            trace.add(new QueryTrace("intent", "Normalized user question", intent));

            var context = retrievalService.retrieve(intent.normalizedQuestion());
            trace.add(new QueryTrace("retrieval", "Retrieved schema and examples", context));

            var generated = sqlGenerationService.generate(intent, context);
            trace.add(new QueryTrace("generation", generated.explanation(), generated));

            var roles = Set.copyOf(intent.userContext().roles());
            var guardResult = sqlGuard.validate(generated.sql(), roles);
            if (!guardResult.allowed()) {
                trace.add(new QueryTrace("guard", "SQL rejected", guardResult.reason()));
                return new QueryResponse(false, null, generated.sql(), null, List.of(), guardResult.reason(), trace);
            }
            trace.add(new QueryTrace("guard", "SQL accepted", guardResult));

            var accessUser = new UserAccessContext(intent.userContext().userId(), intent.userContext().tenantId(), roles);
            String rewrittenSql = aclRewriter.rewrite(guardResult.sql(), accessUser);
            trace.add(new QueryTrace("acl", "ACL policy applied", rewrittenSql));

            var rows = queryExecutor.query(rewrittenSql);
            trace.add(new QueryTrace("execution", "Query executed", rows.size() + " rows"));

            String answer = "Returned " + rows.size() + " row(s).";
            return new QueryResponse(true, answer, generated.sql(), rewrittenSql, rows, null, trace);
        } catch (Exception e) {
            trace.add(new QueryTrace("error", "Pipeline failed", e.getMessage()));
            return new QueryResponse(false, null, null, null, List.of(), e.getMessage(), trace);
        }
    }
}
```

- [ ] **Step 5: Register security beans for runtime**

Add configuration class:

```java
package com.omniquery.core.config;

import com.omniquery.security.AclRewriter;
import com.omniquery.security.SqlGuard;
import com.omniquery.security.model.AccessPolicy;
import com.omniquery.security.model.SchemaPolicy;
import com.omniquery.security.model.TablePolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.Set;

@Configuration
public class SecurityPolicyConfig {

    @Bean
    SchemaPolicy schemaPolicy() {
        return new SchemaPolicy(Map.of(
            "customers", new TablePolicy("customers", Set.of("id", "name", "tenant_id", "created_by"), Set.of("admin", "user")),
            "orders", new TablePolicy("orders", Set.of("id", "customer_id", "status", "total_amount", "tenant_id", "created_by", "created_at"), Set.of("admin", "user"))
        ));
    }

    @Bean
    SqlGuard sqlGuard(SchemaPolicy schemaPolicy) {
        return new SqlGuard(schemaPolicy, 100);
    }

    @Bean
    AclRewriter aclRewriter() {
        return new AclRewriter(new AccessPolicy(Map.of("orders", "tenant_id")));
    }
}
```

- [ ] **Step 6: Run core test**

Run:

```powershell
mvn -pl omniquery-core -Dtest=OrchestratorKernelTest test
```

Expected: `BUILD SUCCESS`.

---

## Task 7: Simplify API Contract

**Files:**
- Modify: `omniquery-backend/omniquery-api/src/main/java/com/omniquery/api/controller/QueryController.java`
- Modify: `omniquery-backend/omniquery-api/src/test/java/com/omniquery/api/OmniIntegrityTest.java`

- [ ] **Step 1: Replace query controller request shape**

```java
package com.omniquery.api.controller;

import com.omniquery.core.engine.OrchestratorKernel;
import com.omniquery.core.model.QueryResponse;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/query")
public class QueryController {

    private final OrchestratorKernel kernel;

    public QueryController(OrchestratorKernel kernel) {
        this.kernel = kernel;
    }

    @PostMapping
    public QueryResponse query(@RequestBody QueryRequest request) {
        return kernel.handle(request.tenantId() == null ? "tenant_a" : request.tenantId(), request.question());
    }

    public record QueryRequest(String question, String tenantId) {}
}
```

- [ ] **Step 2: Rewrite integrity test**

```java
package com.omniquery.api;

import com.omniquery.core.engine.OrchestratorKernel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class OmniIntegrityTest {

    @Autowired
    OrchestratorKernel kernel;

    @Test
    void contextLoads() {
        assertNotNull(kernel);
    }
}
```

- [ ] **Step 3: Run API tests**

Run:

```powershell
mvn -pl omniquery-api -am test
```

Expected: `BUILD SUCCESS`.

---

## Task 8: Remove Multi-Agent, Teaching, MCP, PGVector, and Desktop Main Paths

**Files:**
- Delete old core agent classes.
- Delete `omniquery-backend/omniquery-core/src/main/java/com/omniquery/core/mcp/McpClient.java`.
- Delete or archive `omniquery-backend/omniquery-plugins`.
- Delete or archive `omniquery-backend/omniquery-metadata`.
- Modify `omniquery-backend/pom.xml` modules.
- Delete or archive `omniquery-desktop`.

- [ ] **Step 1: Remove old core classes using apply_patch delete hunks**

Delete:

```text
omniquery-core/src/main/java/com/omniquery/core/agent/PlannerAgent.java
omniquery-core/src/main/java/com/omniquery/core/agent/RouterAgent.java
omniquery-core/src/main/java/com/omniquery/core/agent/SqlAgent.java
omniquery-core/src/main/java/com/omniquery/core/agent/SynthesizerAgent.java
omniquery-core/src/main/java/com/omniquery/core/mcp/McpClient.java
omniquery-core/src/main/java/com/omniquery/core/model/OrchestrationPhase.java
omniquery-core/src/main/java/com/omniquery/core/permission/PermissionManager.java
omniquery-core/src/main/java/com/omniquery/core/permission/PermissionResult.java
```

- [ ] **Step 2: Remove unused backend modules from parent POM**

Set modules to:

```xml
<modules>
    <module>omniquery-api</module>
    <module>omniquery-core</module>
    <module>omniquery-sdk</module>
    <module>omniquery-security</module>
    <module>omniquery-rag</module>
</modules>
```

- [ ] **Step 3: Remove inactive module directories or move them under archive**

Preferred final state:

```text
archive/omniquery-desktop
archive/omniquery-plugins
archive/omniquery-metadata
```

Use PowerShell `Move-Item` only after verifying destination:

```powershell
New-Item -ItemType Directory -Force -Path archive
Move-Item -LiteralPath omniquery-desktop -Destination archive\omniquery-desktop
Move-Item -LiteralPath omniquery-backend\omniquery-plugins -Destination archive\omniquery-plugins
Move-Item -LiteralPath omniquery-backend\omniquery-metadata -Destination archive\omniquery-metadata
```

- [ ] **Step 4: Run full backend tests**

Run:

```powershell
mvn -f omniquery-backend/pom.xml test
```

Expected: `BUILD SUCCESS`.

---

## Task 9: Replace Frontend With Minimal NL2SQL Workbench

**Files:**
- Replace: `omniquery-frontend/src/App.tsx`
- Replace: `omniquery-frontend/src/App.css`
- Replace: `omniquery-frontend/src/index.css`
- Delete: `omniquery-frontend/src/components/Config/SettingsDrawer.tsx`

- [ ] **Step 1: Replace `App.tsx`**

```tsx
import { useState } from 'react';
import './App.css';

type QueryTrace = {
  phase: string;
  message: string;
  detail: unknown;
};

type QueryResponse = {
  success: boolean;
  answer: string | null;
  rawSql: string | null;
  guardedSql: string | null;
  rows: Record<string, unknown>[];
  error: string | null;
  trace: QueryTrace[];
};

function App() {
  const [question, setQuestion] = useState('show recent orders with customer names');
  const [response, setResponse] = useState<QueryResponse | null>(null);
  const [loading, setLoading] = useState(false);

  async function runQuery() {
    setLoading(true);
    try {
      const res = await fetch('http://localhost:8080/api/query', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ question, tenantId: 'tenant_a' }),
      });
      setResponse(await res.json());
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="page">
      <section className="shell">
        <header>
          <h1>OmniQuery AI</h1>
          <p>Generic NL2SQL engine with RAG, SQL guard, ACL rewrite, and trace.</p>
        </header>

        <div className="query-bar">
          <input value={question} onChange={(event) => setQuestion(event.target.value)} />
          <button onClick={runQuery} disabled={loading || !question.trim()}>
            {loading ? 'Running' : 'Run'}
          </button>
        </div>

        {response && (
          <div className="result-grid">
            <section>
              <h2>SQL</h2>
              <pre>{response.guardedSql || response.rawSql || response.error}</pre>
            </section>
            <section>
              <h2>Trace</h2>
              <ol>
                {response.trace.map((step, index) => (
                  <li key={`${step.phase}-${index}`}>
                    <strong>{step.phase}</strong>
                    <span>{step.message}</span>
                  </li>
                ))}
              </ol>
            </section>
            <section className="wide">
              <h2>Rows</h2>
              <pre>{JSON.stringify(response.rows, null, 2)}</pre>
            </section>
          </div>
        )}
      </section>
    </main>
  );
}

export default App;
```

- [ ] **Step 2: Replace CSS**

`App.css`:

```css
.page {
  min-height: 100vh;
  background: #f6f7f9;
  color: #171a1f;
}

.shell {
  width: min(1120px, calc(100% - 32px));
  margin: 0 auto;
  padding: 32px 0;
}

header {
  margin-bottom: 24px;
}

h1 {
  margin: 0 0 8px;
  font-size: 32px;
}

p {
  margin: 0;
  color: #5b6472;
}

.query-bar {
  display: grid;
  grid-template-columns: 1fr 120px;
  gap: 12px;
  margin-bottom: 24px;
}

input,
button {
  height: 44px;
  border-radius: 6px;
  font-size: 15px;
}

input {
  border: 1px solid #c8ced8;
  padding: 0 12px;
}

button {
  border: 0;
  background: #1f6feb;
  color: white;
  font-weight: 700;
}

button:disabled {
  opacity: 0.55;
}

.result-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

section {
  background: white;
  border: 1px solid #d9dee7;
  border-radius: 8px;
  padding: 16px;
}

h2 {
  margin: 0 0 12px;
  font-size: 16px;
}

pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 13px;
}

ol {
  margin: 0;
  padding-left: 20px;
}

li {
  margin-bottom: 10px;
}

li span {
  display: block;
  color: #5b6472;
  margin-top: 2px;
}

.wide {
  grid-column: 1 / -1;
}

@media (max-width: 760px) {
  .query-bar,
  .result-grid {
    grid-template-columns: 1fr;
  }
}
```

`index.css`:

```css
* {
  box-sizing: border-box;
}

body {
  margin: 0;
  font-family: Inter, ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
}
```

- [ ] **Step 3: Remove SettingsDrawer import references**

Confirm:

```powershell
Select-String -Path omniquery-frontend\src\*.tsx -Pattern SettingsDrawer
```

Expected: no matches.

- [ ] **Step 4: Build frontend**

Run:

```powershell
npm.cmd run build
```

Expected: `✓ built`.

---

## Task 10: Rewrite README and Resume Story

**Files:**
- Create or replace: `D:\wookspace\OmniQuery AI\README.md`
- Replace corrupted docs only if they are still referenced from README.

- [ ] **Step 1: Create root README**

```markdown
# OmniQuery AI

OmniQuery AI is a compact Java/Spring Boot NL2SQL engine. It turns a natural-language question into SQL, retrieves schema and example context, validates generated SQL with an AST guard, injects tenant ACL conditions, executes read-only queries, and returns a traceable response.

## Why This Project Exists

The goal is not to build a large agent platform. The goal is to make one agent capability production-shaped: safe natural-language database querying.

## Core Flow

```text
question -> schema/example retrieval -> LangChain4j SQL generation -> SQL guard -> ACL rewrite -> query execution -> trace
```

## Highlights

- LangChain4j boundary for NL2SQL generation.
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

## Resume Description

Built a generic NL2SQL query engine with Spring Boot and LangChain4j, including schema/example retrieval, read-only tool calling, Druid AST SQL guardrails, policy-driven ACL injection, traceable execution, and H2-backed integration tests.
```

- [ ] **Step 2: Verify README commands**

Run:

```powershell
mvn -f omniquery-backend/pom.xml test
npm.cmd --prefix omniquery-frontend run build
```

Expected: backend `BUILD SUCCESS`; frontend `✓ built`.

---

## Task 11: Final Verification

**Files:**
- No new files unless verification exposes failures.

- [ ] **Step 1: Search for removed architecture terms**

Run:

```powershell
Get-ChildItem -Path . -Recurse -File -Include *.java,*.ts,*.tsx,*.md,*.xml,*.json -ErrorAction SilentlyContinue |
  Where-Object { $_.FullName -notmatch '\\node_modules\\|\\target\\|\\dist\\|\\archive\\' } |
  Select-String -Pattern 'Multi-Agent|PlannerAgent|RouterAgent|Teaching|GradeQueryTool|SimpleInMemVectorStore|PGVector|MCP'
```

Expected: matches only in archived files or design/plan docs explaining removal.

- [ ] **Step 2: Run full backend test suite**

Run:

```powershell
mvn -f omniquery-backend/pom.xml test
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 3: Run frontend build**

Run:

```powershell
npm.cmd --prefix omniquery-frontend run build
```

Expected: `✓ built`.

- [ ] **Step 4: Optional manual API smoke test**

Start backend:

```powershell
mvn -f omniquery-backend/pom.xml -pl omniquery-api -am spring-boot:run
```

In another terminal:

```powershell
Invoke-RestMethod -Uri http://localhost:8080/api/query -Method POST -ContentType 'application/json' -Body '{"question":"show total paid amount by customer","tenantId":"tenant_a"}'
```

Expected:

- `success` is `true`.
- `guardedSql` contains an ACL predicate for `tenant_a`.
- returned rows do not include `tenant_b` data.

---

## Self-Review

### Spec Coverage

- Generic NL2SQL: Tasks 1, 5, 6, 7.
- LangChain4j boundary: Task 5.
- Tools retained as read-only: Task 5.
- RAG retained honestly: Task 2.
- ACL optimized: Task 3.
- Desktop removed from main path: Task 8.
- Teaching business removed: Tasks 2, 4, 8, 10.
- Minimal React UI: Task 9.
- Tests: Tasks 2, 3, 6, 7, 11.

### Risk Notes

- Druid AST table and column extraction can become complex for aliases and joins. The first implementation supports simple query blocks and joins; unsupported SQL is rejected or left unmodified according to the task-specific tests.
- LangChain4j dependency version may need adjustment if Maven cannot resolve `1.3.0` from the local network environment. Use Maven Central's latest compatible 1.x release and update both parent property and plan notes in the final implementation summary.
- The first ACL rewriter applies table-level tenant ACL directly to simple table sources. Join-aware ACL can be added by aliasing predicates in a separate focused task after this plan is green.

