package miu.cs544.releasesystem.release.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Service
@Slf4j
public class ActivityStreamService {

    private final Sinks.Many<ServerSentEvent<Object>> sink;

    public ActivityStreamService() {
        // Replay the last 10 events to new subscribers
        this.sink = Sinks.many().replay().limit(10);
    }

    public void pushEvent(String type, Object data) {
        ServerSentEvent<Object> event = ServerSentEvent.builder()
                .event(type)
                .data(data)
                .build();
        
        Sinks.EmitResult result = sink.tryEmitNext(event);
        if (result.isFailure()) {
            log.error("Failed to emit SSE: {}", result);
        }
    }

    public Flux<ServerSentEvent<Object>> getStream() {
        return sink.asFlux();
    }
}
