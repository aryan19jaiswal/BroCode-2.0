package com.broCode.repository;

import com.broCode.model.ChatSessionDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ChatSessionRepository extends MongoRepository<ChatSessionDocument, String> {
    List<ChatSessionDocument> findByUserIdOrderByUpdatedAtDesc(String userId);
    Optional<ChatSessionDocument> findByIdAndUserId(String id, String userId);
    void deleteByIdAndUserId(String id, String userId);
}
