package miu.cs544.releasesystem.notification.repository;

import miu.cs544.releasesystem.notification.domain.NotificationLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationLogRepository extends MongoRepository<NotificationLog, String> {
}
