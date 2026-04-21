package com.budget.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Enables @Async so emails are sent in background
 * without slowing down API responses
 */
@Configuration
@EnableAsync
public class AsyncConfig {
    // Spring handles the rest automatically
}
