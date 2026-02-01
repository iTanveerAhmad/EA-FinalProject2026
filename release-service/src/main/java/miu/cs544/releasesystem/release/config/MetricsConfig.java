package miu.cs544.releasesystem.release.config;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import miu.cs544.releasesystem.release.repository.ReleaseRepository;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
@RequiredArgsConstructor
public class MetricsConfig {

    private final MeterRegistry meterRegistry;
    private final ReleaseRepository releaseRepository;

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
}
