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
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReleaseService {

    private final ReleaseRepository releaseRepository;
    private final UserRepository userRepository;
    private final KafkaProducerService kafkaProducerService;
    private final ActivityStreamService activityStreamService;
    private final MeterRegistry meterRegistry;

    // Metrics fields
    private Counter kafkaEventsCounter;
    private Timer aiRequestTimer;

    @PostConstruct
    public void initMetrics() {
        // 1. Active Developers Gauge
        meterRegistry.gauge("active_developers_count", this, ReleaseService::countActiveDevelopers);

        // THIS IS THE LINE THAT MAKES IT APPEAR IN ACTUATOR
        this.kafkaEventsCounter = meterRegistry.counter("kafka_events_published_total");

        this.aiRequestTimer = Timer.builder("ai_request_latency")
                .publishPercentileHistogram()
                .register(meterRegistry);
    }

    public int countActiveDevelopers() {
        List<User> allUsers = userRepository.findAll();
        int count = 0;
        for (User user : allUsers) {
            if (user.getRole() == Role.DEVELOPER) {
                List<Release> releases = releaseRepository.findAll();
                boolean hasActive = releases.stream()
                        .flatMap(r -> r.getTasks().stream())
                        .anyMatch(t -> user.getId().equals(t.getAssignedDeveloperId()) && t.getStatus() == TaskStatus.IN_PROCESS);
                if (hasActive) count++;
            }
        }
        return count;
    }

    public Release createRelease(ReleaseRequest request) {
        Release release = new Release();
        release.setName(request.getName());
        release.setDescription(request.getDescription());
        release.setCreatedAt(Instant.now());
        release.setUpdatedAt(Instant.now());
        return releaseRepository.save(release);
    }

    public List<Release> getAllReleases() {
        return releaseRepository.findAll();
    }

    public Release getReleaseById(String id) {
        return releaseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Release not found"));
    }

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
            release.setReopened(true);
            release.setHotfixCount(release.getHotfixCount() + 1);
            release.setReopenedAt(Instant.now());

            HotfixTaskAddedEvent event = new HotfixTaskAddedEvent(
                    task.getId(), task.getAssignedDeveloperId(), developerEmail, release.getId(), task.getTitle()
            );

            kafkaProducerService.sendHotfixTaskAddedEvent(event);
            kafkaEventsCounter.increment(); // Kafka Metric

            aiRequestTimer.record(() -> activityStreamService.pushEvent("Hotfix Added", event)); // AI Metric
        }

        TaskAssignedEvent event = new TaskAssignedEvent(
                task.getId(), task.getAssignedDeveloperId(), developerEmail, release.getId()
        );
        kafkaProducerService.sendTaskAssignedEvent(event);
        kafkaEventsCounter.increment(); // Kafka Metric

        release.setUpdatedAt(Instant.now());
        return releaseRepository.save(release);
    }

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

        List<Release> activeReleases = releaseRepository.findReleasesWithActiveTaskForDeveloper(developerId);
        if (!activeReleases.isEmpty()) {
            throw new RuntimeException("Developer already has an IN_PROCESS task.");
        }

        List<Task> sortedTasks = release.getTasks().stream()
                .sorted(Comparator.comparingInt(Task::getOrderIndex))
                .toList();

        int taskIndex = sortedTasks.indexOf(task);
        if (taskIndex > 0) {
            Task previousTask = sortedTasks.get(taskIndex - 1);
            if (previousTask.getStatus() != TaskStatus.COMPLETED) {
                throw new BusinessRuleException("Previous task is not completed.");
            }
        }

        task.setStatus(TaskStatus.IN_PROCESS);
        task.setStartedAt(Instant.now());
        releaseRepository.save(release);

        aiRequestTimer.record(() -> activityStreamService.pushEvent("Task Started", "Task " + task.getTitle() + " started by " + developerId));
    }

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
        task.setCompletedAt(Instant.now());
        release.setUpdatedAt(Instant.now());
        releaseRepository.save(release);

        // Task Completion Metrics
        meterRegistry.counter("tasks_completed_total").increment();

        TaskCompletedEvent event = new TaskCompletedEvent(taskId, developerId, release.getId());
        kafkaProducerService.sendTaskCompletedEvent(event);
        kafkaEventsCounter.increment(); // Kafka Metric

        aiRequestTimer.record(() -> activityStreamService.pushEvent("Task Completed", event));
    }

    @Transactional
    public void completeRelease(String releaseId) {
        Release release = getReleaseById(releaseId);
        boolean allCompleted = release.getTasks().stream().allMatch(t -> t.getStatus() == TaskStatus.COMPLETED);
        if (!allCompleted) throw new BusinessRuleException("Cannot complete release. Not all tasks are COMPLETED.");

        release.setStatus(ReleaseStatus.COMPLETED);
        release.setUpdatedAt(Instant.now());
        releaseRepository.save(release);
    }

    public List<Task> getTasksForDeveloper(String developerId) {
        return releaseRepository.findAll().stream()
                .flatMap(r -> r.getTasks().stream())
                .filter(t -> developerId.equals(t.getAssignedDeveloperId()))
                .toList();
    }

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

        aiRequestTimer.record(() -> activityStreamService.pushEvent("New Comment", "User " + developerId + " commented on " + task.getTitle()));
    }

    public List<Comment> getCommentsForTask(String taskId) {
        Release release = releaseRepository.findByTaskId(taskId);
        if (release == null) throw new RuntimeException("Release not found");

        Task task = release.getTasks().stream()
                .filter(t -> t.getId().equals(taskId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Task not found"));

        return task.getComments();
    }

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

        aiRequestTimer.record(() -> activityStreamService.pushEvent("New Reply", "User " + developerId + " replied to a comment"));
    }

    private Release findReleaseContainingComment(String commentId) {
        for (Release release : releaseRepository.findAll()) {
            if (findCommentById(release, commentId) != null) return release;
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