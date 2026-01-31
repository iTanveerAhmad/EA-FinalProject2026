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

@Service
@RequiredArgsConstructor
@Slf4j
public class ReleaseService {

    private final ReleaseRepository releaseRepository;
    private final KafkaProducerService kafkaProducerService;
    private final ActivityStreamService activityStreamService; // Injected

    public Release createRelease(ReleaseRequest request) {
        Release release = new Release();
        release.setName(request.getName());
        release.setDescription(request.getDescription());
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
        // activityStreamService.pushEvent("Task Assigned", event); // Optional, maybe too noisy

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
             throw new RuntimeException("Developer already has an IN_PROCESS task. Finish it first!");
        }

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
        task.setStartedAt(Instant.now()); // Set timestamp
        releaseRepository.save(release);
        
        activityStreamService.pushEvent("Task Started", "Task " + task.getTitle() + " started by " + developerId);
        log.info("Task {} started by {}", taskId, developerId);
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
        releaseRepository.save(release);

        TaskCompletedEvent event = new TaskCompletedEvent(taskId, developerId, release.getId());
        kafkaProducerService.sendTaskCompletedEvent(event);
        activityStreamService.pushEvent("Task Completed", event);
        
        log.info("Task {} completed", taskId);
    }

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
    
    public List<Task> getTasksForDeveloper(String developerId) {
        List<Release> allReleases = releaseRepository.findAll();
        return allReleases.stream()
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