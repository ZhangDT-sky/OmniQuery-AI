package com.omniquery.api;

import com.omniquery.rag.repository.KnowledgeBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest(properties = {
    "spring.profiles.active=external-db",
    "spring.datasource.url=jdbc:h2:mem:external_db_context;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.sql.init.mode=never"
})
class ExternalDatabaseContextTest {

    @Autowired
    private KnowledgeBase knowledgeBase;

    @Test
    void startsExternalDatabaseKnowledgeBaseWithConfiguredExamples() {
        assertFalse(knowledgeBase.examples().isEmpty());
    }
}
