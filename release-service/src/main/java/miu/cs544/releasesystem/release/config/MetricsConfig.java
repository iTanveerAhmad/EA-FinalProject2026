package miu.cs544.releasesystem.release.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import miu.cs544.releasesystem.release.repository.ReleaseRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
@RequiredArgsConstructor
public class MetricsConfig {

    private final MeterRegistry meterRegistry;
    private final ReleaseRepository releaseRepository;

    // 1. Active Developers Count

    @PostConstruct
    public void registerMetrics() {
        Gauge.builder("active_developers_count", this, MetricsConfig::countActiveDevelopers)
                .description("Number of developers with at least one assigned task")
                .register(meterRegistry);
    }

    private double countActiveDevelopers() {
        return releaseRepository.findAll().stream()
                .flatMap(r -> r.getTasks().stream())
                .map(t -> t.getAssignedDeveloperId())
                .filter(id -> id != null && !id.isBlank())
                .collect(Collectors.toSet())
                .size();
    }

    // 2. Tasks Completed Counter
    @Bean
    public Counter tasksCompletedCounter(MeterRegistry registry) {
        return Counter.builder("tasks_completed_total")
                .description("Total number of tasks completed")
                .register(registry);
    }

    // 3. Kafka Events Published Counter
    @Bean
    public Counter kafkaEventsCounter(MeterRegistry registry) {
        return Counter.builder("kafka_events_published_total")
                .description("Total Kafka events published to the notification service")
                .register(registry);
    }

    // 4. AI Request Rate + Latency (Timer)
    @Bean
    public Timer aiRequestTimer(MeterRegistry registry) {
        return Timer.builder("ai_request_latency")
                .description("AI Request processing time and rate")
                .publishPercentileHistogram() // Important for AI Latency p99/p95 graphs
                .register(registry);
    }
}
