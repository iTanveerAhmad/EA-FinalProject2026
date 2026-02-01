package miu.cs544.releasesystem.release.controller;

import miu.cs544.releasesystem.release.service.ActivityStreamService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/activity")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityStreamService activityStreamService;

    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasAnyRole('DEVELOPER', 'ADMIN')")
    public Flux<ServerSentEvent<Object>> streamEvents() {
        ServerSentEvent<Object> welcome = ServerSentEvent.builder()
                .event("connected")
                .data("Activity feed connected. Start a task, complete a task, or add a comment on My Tasks to see events here.")
                .build();
        return Flux.concat(Flux.just(welcome), activityStreamService.getStream());
    }
}
