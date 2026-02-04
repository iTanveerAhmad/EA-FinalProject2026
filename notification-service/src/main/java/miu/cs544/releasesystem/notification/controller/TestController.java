package miu.cs544.releasesystem.notification.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import miu.cs544.releasesystem.notification.service.EmailService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Test endpoints for debugging email and notification flow.
 * Use GET /test/email?to=your@email.com to verify SMTP works.
 */
@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
@Slf4j
public class TestController {

    private final EmailService emailService;

    @GetMapping("/email")
    public ResponseEntity<String> sendTestEmail(@RequestParam String to) {
        try {
            emailService.sendEmail(to, "Test from Notification Service", 
                "If you receive this, SMTP is configured correctly.");
            log.info("Test email sent successfully to {}", to);
            return ResponseEntity.ok("Test email sent to " + to);
        } catch (Exception e) {
            log.error("Test email failed", e);
            return ResponseEntity.internalServerError()
                .body("Failed: " + e.getMessage());
        }
    }
}
