package miu.cs544.releasesystem.release.repository;

import miu.cs544.releasesystem.release.domain.ChatSession;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatSessionRepository extends MongoRepository<ChatSession, String> {
    List<ChatSession> findByDeveloperId(String developerId);
}
