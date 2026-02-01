package miu.cs544.releasesystem.release.repository;

import miu.cs544.releasesystem.release.domain.Release;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReleaseRepository extends MongoRepository<Release, String> {
    // Find releases that contain a specific task ID
    @Query("{ 'tasks._id': ?0 }")
    Release findByTaskId(String taskId);

    // Find if a developer has any IN_PROCESS task across all releases
    @Query("{ 'tasks': { $elemMatch: { 'assignedDeveloperId': ?0, 'status': 'IN_PROCESS' } } }")
    List<Release> findReleasesWithActiveTaskForDeveloper(String developerId);
}
