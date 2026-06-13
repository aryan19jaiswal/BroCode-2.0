package com.broCode.dto;

/**
 * Lightweight DTO for serializing LangChain4j ChatMessage objects to/from Redis.
 * Using a simple role+content pair avoids coupling to LangChain4j's internal
 * class hierarchy and survives library version upgrades cleanly.
 */
public record StoredMessage(String role, String content) {}
