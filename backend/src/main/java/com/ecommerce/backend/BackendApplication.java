package com.ecommerce.backend;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Slf4j
public class BackendApplication {

    public static void main(String[] args) {
        log.info("Starting E-commerce Backend Application");
        try {
            SpringApplication.run(BackendApplication.class, args);
            log.info("E-commerce Backend Application started successfully");
        } catch (Exception e) {
            log.error("Failed to start E-commerce Backend Application", e);
            throw e;
        }
    }
}
