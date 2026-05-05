package com.omniquery.core.config;

import com.omniquery.core.service.QueryIntentNormalizer;
import com.omniquery.rag.model.SchemaDocument;
import com.omniquery.rag.repository.KnowledgeBase;
import com.omniquery.security.AclRewriter;
import com.omniquery.security.SqlGuard;
import com.omniquery.security.model.AccessPolicy;
import com.omniquery.security.model.SchemaPolicy;
import com.omniquery.security.model.TablePolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Set;

@Configuration
@EnableConfigurationProperties(OmniQueryProperties.class)
public class SecurityPolicyConfig {

    @Bean
    QueryIntentNormalizer queryIntentNormalizer() {
        return new QueryIntentNormalizer();
    }

    @Bean
    SchemaPolicy schemaPolicy(KnowledgeBase knowledgeBase) {
        return new SchemaPolicy(knowledgeBase.schemas().stream()
            .collect(Collectors.toMap(
                SchemaDocument::tableName,
                schema -> new TablePolicy(
                    schema.tableName(),
                    schema.columns().stream().map(this::columnName).collect(Collectors.toSet()),
                    Set.copyOf(schema.roles())
                )
            )));
    }

    @Bean
    SqlGuard sqlGuard(SchemaPolicy schemaPolicy, OmniQueryProperties properties) {
        return new SqlGuard(
            schemaPolicy,
            properties.security().getMaxRows(),
            properties.security().getMaxJoins(),
            properties.security().getDangerousFunctions()
        );
    }

    @Bean
    AclRewriter aclRewriter(KnowledgeBase knowledgeBase) {
        return new AclRewriter(new AccessPolicy(knowledgeBase.schemas().stream()
            .filter(schema -> schema.columns().stream().map(this::columnName).anyMatch("tenant_id"::equals))
            .collect(Collectors.toMap(SchemaDocument::tableName, schema -> "tenant_id"))));
    }

    private String columnName(String columnDefinition) {
        return columnDefinition.split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
    }
}
