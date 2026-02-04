package miu.cs544.releasesystem.release.service;

import miu.cs544.releasesystem.release.domain.*;
import miu.cs544.releasesystem.release.exception.BusinessRuleException;
import miu.cs544.releasesystem.release.dto.ReleaseRequest;
import miu.cs544.releasesystem.release.dto.TaskRequest;
import miu.cs544.releasesystem.release.event.HotfixTaskAddedEvent;
import miu.cs544.releasesystem.release.event.TaskAssignedEvent;
import miu.cs544.releasesystem.release.event.TaskCompletedEvent;
import miu.cs544.releasesystem.release.repository.ReleaseRepository;
import miu.cs544.releasesystem.release.repository.UserRepository;
import io.micrometer.core.instrument.MeterRegistry;
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
    private final UserRepository userRepository;
    private final KafkaProducerService kafkaProducerService;
    private final ActivityStreamService activityStreamService;
    private final MeterRegistry meterRegistry;

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

        String developerEmail = userRepository.findFirstByUsername(task.getAssignedDeveloperId())
                .map(User::getEmail)
                .orElse(null);

        if (release.getStatus() == ReleaseStatus.COMPLETED) {
            log.info("Adding Hotfix to completed release: {}", releaseId);
            release.setStatus(ReleaseStatus.IN_PROGRESS);
            
            HotfixTaskAddedEvent event = new HotfixTaskAddedEvent(
                task.getId(),
                task.getAssignedDeveloperId(),
                developerEmail,
                release.getId(),
                task.getTitle()
            );
            kafkaProducerService.sendHotfixTaskAddedEvent(event);
            activityStreamService.pushEvent("Hotfix Added", event);
        }

        TaskAssignedEvent event = new TaskAssignedEvent(
            task.getId(),
            task.getAssignedDeveloperId(),
            developerEmail,
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
                throw new BusinessRuleException("Previous task (Order " + previousTask.getOrderIndex() + ") is not completed.");
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
        meterRegistry.counter("tasks_completed_total").increment();

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
            throw new BusinessRuleException("Cannot complete release. Not all tasks are COMPLETED.");
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
            .orElseThrow(() -> new RuntimeException("Task not found"));
            
        Comment comment = new Comment();
        comment.setAuthorId(developerId);
        comment.setContent(content);
        comment.setTimestamp(Instant.now());
        
        task.getComments().add(comment);
        releaseRepository.save(release);
        
        activityStreamService.pushEvent("New Comment", "User " + developerId + " commented on " + task.getTitle());
    }

    /**
     * Get all comments for a task (threaded/nested structure).
     */
    public List<Comment> getCommentsForTask(String taskId) {
        Release release = releaseRepository.findByTaskId(taskId);
        if (release == null) throw new RuntimeException("Release not found");
        
        Task task = release.getTasks().stream()
            .filter(t -> t.getId().equals(taskId))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Task not found"));
        
        return task.getComments();
    }

    /**
     * Add a reply to a comment (supports nested threading - Reddit-style).
     */
    @Transactional
    public void addReplyToComment(String commentId, String developerId, String content) {
        Release release = findReleaseContainingComment(commentId);
        if (release == null) throw new RuntimeException("Comment not found");
        
        Comment parentComment = findCommentById(release, commentId);
        if (parentComment == null) throw new RuntimeException("Comment not found");
        
        Comment reply = new Comment();
        reply.setAuthorId(developerId);
        reply.setContent(content);
        reply.setTimestamp(Instant.now());
        
        parentComment.getReplies().add(reply);
        releaseRepository.save(release);
        
        activityStreamService.pushEvent("New Reply", "User " + developerId + " replied to a comment");
    }

    private Release findReleaseContainingComment(String commentId) {
        for (Release release : releaseRepository.findAll()) {
            if (findCommentById(release, commentId) != null) {
                return release;
            }
        }
        return null;
    }

    private Comment findCommentById(Release release, String commentId) {
        for (Task task : release.getTasks()) {
            Comment found = findCommentRecursive(task.getComments(), commentId);
            if (found != null) return found;
        }
        return null;
    }

    private Comment findCommentRecursive(List<Comment> comments, String commentId) {
        for (Comment c : comments) {
            if (commentId.equals(c.getId())) return c;
            Comment found = findCommentRecursive(c.getReplies(), commentId);
            if (found != null) return found;
        }
        return null;
    }
}
