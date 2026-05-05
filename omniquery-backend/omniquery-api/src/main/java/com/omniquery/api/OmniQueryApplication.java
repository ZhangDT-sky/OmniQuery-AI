package com.omniquery.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "com.omniquery")
public class OmniQueryApplication {
    public static void main(String[] args) {
        SpringApplication.run(OmniQueryApplication.class, args);
    }
}
