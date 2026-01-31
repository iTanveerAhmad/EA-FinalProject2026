package com.example.releasesystem.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {

    public void sendEmail(String recipient, String subject, String body) {
        // Mock email sending
        log.info("----------------------------------------------------------------");
        log.info("SENDING EMAIL TO: {}", recipient);
        log.info("SUBJECT: {}", subject);
        log.info("BODY: {}", body);
        log.info("----------------------------------------------------------------");
    }
}
