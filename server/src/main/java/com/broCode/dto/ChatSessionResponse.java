package com.broCode.dto;

import java.util.List;

public record ChatSessionResponse(
        String id,
        String title,
        List<StoredMessage> messages,
        long createdAt
) {}
