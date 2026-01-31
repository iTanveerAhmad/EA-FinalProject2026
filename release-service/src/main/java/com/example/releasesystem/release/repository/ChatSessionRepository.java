package com.example.releasesystem.release.repository;

import com.example.releasesystem.release.domain.ChatSession;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatSessionRepository extends MongoRepository<ChatSession, String> {
    List<ChatSession> findByDeveloperId(String developerId);
}
