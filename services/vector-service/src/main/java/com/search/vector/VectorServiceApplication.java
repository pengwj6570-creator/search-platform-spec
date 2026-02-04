package com.search.vector;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Vector Service Application
 *
 * Provides text and image embedding APIs using deep learning models.
 * Supports BGE, GTE, CLIP and other embedding models.
 */
@SpringBootApplication
@EnableAsync
public class VectorServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(VectorServiceApplication.class, args);
    }
}
