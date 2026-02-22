package com.broCode.config;

import org.springframework.context.annotation.Configuration;

/**
 * LLM provider configuration.
 * Currently GeminiLLMService is the sole provider and is auto-discovered via @Service.
 * This class is reserved for future multi-provider routing (e.g., OpenAI, Anthropic).
 */
@Configuration
public class LLMProviderConfig {
}
