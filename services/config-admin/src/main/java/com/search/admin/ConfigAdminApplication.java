package com.search.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Configuration Administration Application
 *
 * Provides REST APIs for managing search metadata configuration
 * including sources and search objects
 */
@SpringBootApplication
public class ConfigAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigAdminApplication.class, args);
    }
}
