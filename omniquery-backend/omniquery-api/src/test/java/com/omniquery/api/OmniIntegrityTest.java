package com.omniquery.api;

import com.omniquery.core.engine.OrchestratorKernel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class OmniIntegrityTest {

    @Autowired
    OrchestratorKernel kernel;

    @Test
    void contextLoads() {
        assertNotNull(kernel);
    }
}
