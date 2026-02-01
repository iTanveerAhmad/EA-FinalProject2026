package com.example.releasesystem.release.service;

import com.example.releasesystem.release.domain.*;
import com.example.releasesystem.release.dto.ReleaseRequest;
import com.example.releasesystem.release.dto.TaskRequest;
import com.example.releasesystem.release.event.HotfixTaskAddedEvent;
import com.example.releasesystem.release.event.TaskAssignedEvent;
import com.example.releasesystem.release.event.TaskCompletedEvent;
import com.example.releasesystem.release.repository.ReleaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * ReleaseService implements the core business logic for release management.
 * 
 * Key Workflow Constraints:
 * 1. Single In-Process Rule: Only ONE task per developer can be in-progress at any time.
 * 2. Sequential Task Execution: Tasks must be completed in release order; cannot start task N until task N-1 is complete.
 * 3. Hotfix Logic: When tasks are added to a completed release, automatically re-open the release.
 * 
 * All service methods use event-driven communication via Kafka for asynchronous updates to the Notification Service.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReleaseService {

    private final ReleaseRepository releaseRepository;
    private final KafkaProducerService kafkaProducerService;
    private final ActivityStreamService activityStreamService;

    /**
     * Create a new release.
     */
    public Release createRelease(ReleaseRequest request) {
        Release release = new Release();
        release.setName(request.getName());
        release.setDescription(request.getDescription());
        return releaseRepository.save(release);
    }

    /**
     * Get all releases.
     */
    public List<Release> getAllReleases() {
        return releaseRepository.findAll();
    }

    /**
     * Get a specific release by ID.
     */
    public Release getReleaseById(String id) {
        return releaseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Release not found"));
    }

    /**
     * Add a task to a release.
     * 
     * If adding to a completed release (Hotfix Logic):
     * - The release is automatically re-opened
     * - A HotfixTaskAddedEvent is published to Kafka
     */
    @Transactional
    public Release addTaskToRelease(String releaseId, TaskRequest taskRequest) {
        Release release = getReleaseById(releaseId);

        Task task = new Task();
        task.setTitle(taskRequest.getTitle());
        task.setDescription(taskRequest.getDescription());
        task.setAssignedDeveloperId(taskRequest.getAssignedDeveloperId());
        task.setOrderIndex(taskRequest.getOrderIndex());
        
        release.getTasks().add(task);

        if (release.getStatus() == ReleaseStatus.COMPLETED) {
            log.info("Adding Hotfix to completed release: {}", releaseId);
            release.setStatus(ReleaseStatus.IN_PROGRESS);
            
            HotfixTaskAddedEvent event = new HotfixTaskAddedEvent(
                task.getId(),
                task.getAssignedDeveloperId(),
                release.getId(),
                task.getTitle()
            );
            kafkaProducerService.sendHotfixTaskAddedEvent(event);
            activityStreamService.pushEvent("Hotfix Added", event);
        }

        TaskAssignedEvent event = new TaskAssignedEvent(
            task.getId(),
            task.getAssignedDeveloperId(),
            release.getId()
        );
        kafkaProducerService.sendTaskAssignedEvent(event);

        return releaseRepository.save(release);
    }

    /**
     * Start a task. Enforces workflow constraints:
     * 1. Single In-Process Rule: Developer cannot have another IN_PROCESS task
     * 2. Sequential Task Execution: All previous tasks must be COMPLETED
     * 3. Only assigned developer can start the task
     */
    @Transactional
    public void startTask(String taskId, String developerId) {
        Release release = releaseRepository.findByTaskId(taskId);
        if (release == null) throw new RuntimeException("Task not found in any release");

        Task task = release.getTasks().stream()
                .filter(t -> t.getId().equals(taskId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Task not found"));

        if (!developerId.equals(task.getAssignedDeveloperId())) {
             throw new RuntimeException("Developer not assigned to this task");
        }

        // Single In-Process Rule: Check if developer has another task in progress
        List<Release> activeReleases = releaseRepository.findReleasesWithActiveTaskForDeveloper(developerId);
        if (!activeReleases.isEmpty()) {
             throw new RuntimeException("Developer already has an IN_PROCESS task. Finish it first!");
        }

        // Sequential Task Execution: Verify all previous tasks are completed
        List<Task> sortedTasks = release.getTasks().stream()
                .sorted(Comparator.comparingInt(Task::getOrderIndex))
                .toList();

        int taskIndex = sortedTasks.indexOf(task);
        if (taskIndex > 0) {
            Task previousTask = sortedTasks.get(taskIndex - 1);
            if (previousTask.getStatus() != TaskStatus.COMPLETED) {
                throw new RuntimeException("Previous task (Order " + previousTask.getOrderIndex() + ") is not completed.");
            }
        }

        task.setStatus(TaskStatus.IN_PROCESS);
        task.setStartedAt(Instant.now()); // Used by StaleTaskScheduler
        releaseRepository.save(release);
        
        activityStreamService.pushEvent("Task Started", "Task " + task.getTitle() + " started by " + developerId);
        log.info("Task {} started by {}", taskId, developerId);
    }

    /**
     * Complete a task. Only the assigned developer can complete the task.
     * Publishes TaskCompletedEvent to notify downstream services.
     */
    @Transactional
    public void completeTask(String taskId, String developerId) {
        Release release = releaseRepository.findByTaskId(taskId);
        if (release == null) throw new RuntimeException("Release not found");

        Task task = release.getTasks().stream()
                .filter(t -> t.getId().equals(taskId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Task not found"));

        if (!developerId.equals(task.getAssignedDeveloperId())) {
             throw new RuntimeException("Developer mismatch");
        }

        task.setStatus(TaskStatus.COMPLETED);
        releaseRepository.save(release);

        TaskCompletedEvent event = new TaskCompletedEvent(taskId, developerId, release.getId());
        kafkaProducerService.sendTaskCompletedEvent(event);
        activityStreamService.pushEvent("Task Completed", event);
        
        log.info("Task {} completed", taskId);
    }

    /**
     * Complete a release. All tasks must be COMPLETED.
     * Once completed, if hotfixes are added, the release is automatically re-opened.
     */
    @Transactional
    public void completeRelease(String releaseId) {
        Release release = getReleaseById(releaseId);

        boolean allCompleted = release.getTasks().stream()
                .allMatch(t -> t.getStatus() == TaskStatus.COMPLETED);

        if (!allCompleted) {
            throw new RuntimeException("Cannot complete release. Not all tasks are COMPLETED.");
        }

        release.setStatus(ReleaseStatus.COMPLETED);
        releaseRepository.save(release);
        log.info("Release {} marked as COMPLETED", releaseId);
    }
    
    /**
     * Get all tasks assigned to a specific developer across all releases.
     */
    public List<Task> getTasksForDeveloper(String developerId) {
        List<Release> allReleases = releaseRepository.findAll();
        return allReleases.stream()
                .flatMap(r -> r.getTasks().stream())
                .filter(t -> developerId.equals(t.getAssignedDeveloperId()))
                .toList();
    }
    
    /**
     * Add a comment to a task for collaborative discussion.
     */
    @Transactional
    public void addComment(String taskId, String developerId, String content) {
        Release release = releaseRepository.findByTaskId(taskId);
        if(release == null) throw new RuntimeException("Release not found");
        
        Task task = release.getTasks().stream()
            .filter(t -> t.getId().equals(taskId))
            .findFirst()
            .orElseThrow();
            
        Comment comment = new Comment();
        comment.setAuthorId(developerId);
        comment.setContent(content);
        comment.setTimestamp(Instant.now());
        
        task.getComments().add(comment);
        releaseRepository.save(release);
        
        activityStreamService.pushEvent("New Comment", "User " + developerId + " commented on " + task.getTitle());
    }
}