package miu.cs544.releasesystem.release.repository;

import miu.cs544.releasesystem.release.domain.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> {
    // Return the first matching user to avoid exceptions when multiple docs exist with the same username
    Optional<User> findFirstByUsername(String username);
}
