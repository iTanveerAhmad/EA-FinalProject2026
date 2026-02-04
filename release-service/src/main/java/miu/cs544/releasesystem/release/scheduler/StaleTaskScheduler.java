package miu.cs544.releasesystem.release.scheduler;

import miu.cs544.releasesystem.release.domain.Release;
import miu.cs544.releasesystem.release.domain.TaskStatus;
import miu.cs544.releasesystem.release.domain.User;
import miu.cs544.releasesystem.release.event.StaleTaskDetectedEvent;
import miu.cs544.releasesystem.release.repository.ReleaseRepository;
import miu.cs544.releasesystem.release.repository.UserRepository;
import miu.cs544.releasesystem.release.service.KafkaProducerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile("!test")
public class StaleTaskScheduler {

    private final ReleaseRepository releaseRepository;
    private final UserRepository userRepository;
    private final KafkaProducerService kafkaProducerService;

    // Run every hour
    @Scheduled(fixedRate = 3600000)
    public void detectStaleTasks() {
        log.info("Running Stale Task Detection...");
        Instant now = Instant.now();
        Instant threshold = now.minus(Duration.ofHours(48));

        // Finding tasks is tricky with embedded docs if we want to be efficient.
        // For prototype, fetching all IN_PROGRESS releases is okay.
        List<Release> releases = releaseRepository.findAll();

        for (Release release : releases) {
            release.getTasks().stream()
                    .filter(t -> t.getStatus() == TaskStatus.IN_PROCESS)
                    .filter(t -> t.getStartedAt() != null && t.getStartedAt().isBefore(threshold))
                    .forEach(t -> {
                        log.warn("Stale task detected: {}", t.getId());
                        
                        Duration duration = Duration.between(t.getStartedAt(), now);
                        String durationStr = duration.toHours() + "h";

                        String developerEmail = userRepository.findFirstByUsername(t.getAssignedDeveloperId())
                                .map(User::getEmail)
                                .orElse(null);

                        kafkaProducerService.sendStaleTaskDetectedEvent(new StaleTaskDetectedEvent(
                                t.getId(),
                                t.getAssignedDeveloperId(),
                                developerEmail,
                                durationStr
                        ));
                    });
        }
    }
}
