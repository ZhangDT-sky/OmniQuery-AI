package com.omniquery.api;

import com.omniquery.core.config.OmniQueryProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = "omniquery.security.max-rows=7")
class OmniConfigurationBindingTest {

    @Autowired
    OmniQueryProperties properties;

    @Test
    void bindsNestedSecurityProperties() {
        assertEquals(7, properties.security().getMaxRows());
    }
}
