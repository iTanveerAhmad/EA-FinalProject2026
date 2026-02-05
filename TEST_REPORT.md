# Test Report - Release System

**Date:** February 5, 2026  
**Build:** SUCCESS  
**Total Tests:** 19  
**Passed:** 19  
**Failed:** 0  
**Errors:** 0  
**Skipped:** 0  

---

## Summary

| Module | Tests | Passed | Failed | Errors | Skipped | Time |
|--------|-------|--------|--------|--------|---------|------|
| release-service | 16 | 16 | 0 | 0 | 0 | ~44s |
| notification-service | 3 | 3 | 0 | 0 | 0 | ~3s |
| **Total** | **19** | **19** | **0** | **0** | **0** | **~48s** |

---

## Prerequisites

Before running tests, ensure:
- **MongoDB** is running on `localhost:27017` (e.g., `docker-compose up -d mongodb`)
- **Kafka** and **Zookeeper** are running on `localhost:9092` (e.g., `docker-compose up -d kafka zookeeper`)

---

## release-service (16 tests)

### ReleaseApiRestAssuredTest (14 tests) — Integration

| # | Test | Status | Description |
|---|------|--------|-------------|
| 1 | authRegister_returnsOk | PASS | User registration returns 200 |
| 2 | authLogin_returnsToken | PASS | Login returns JWT token |
| 3 | authLogin_invalidCredentials_returns401 | PASS | Invalid credentials return 401 |
| 4 | releases_withoutAuth_returns401 | PASS | Unauthenticated access returns 401 |
| 5 | releases_asAdmin_returnsOk | PASS | Admin can list releases |
| 6 | releases_createRelease_asAdmin_returnsOk | PASS | Admin can create release |
| 7 | releases_asDeveloper_returns403 | PASS | Developer cannot create release (403) |
| 8 | tasks_getMyTasks_asDeveloper_returnsOk | PASS | Developer can get assigned tasks |
| 9 | forum_addCommentAndGetComments_returnsOk | PASS | Add comment and retrieve comments |
| 10 | activity_stream_returnsSSE | PASS | Activity stream returns SSE content type |
| 11 | task_start_shouldFail_whenPreviousTaskNotCompleted | PASS | Sequential rule: Task 2 cannot start before Task 1 |
| 12 | task_start_shouldFail_whenDeveloperAlreadyHasInProcessTask | PASS | Single in-process rule: Developer cannot have 2 IN_PROCESS tasks |
| 13 | completeRelease_shouldFail_whenTasksNotAllCompleted | PASS | Release cannot complete until all tasks done |
| 14 | actuator_health_isPublic | PASS | Actuator health endpoint is public |

### ReleaseServiceApplicationTests (2 tests) — Application Context

| # | Test | Status | Description |
|---|------|--------|-------------|
| 15 | testRegisterAdmin | PASS | Admin registration works |
| 16 | contextLoads | PASS | Spring context loads successfully |

---

## notification-service (3 tests)

### KafkaConsumerServiceTest (3 tests) — Unit

| # | Test | Status | Description |
|---|------|--------|-------------|
| 1 | listenTaskEvents_sendsEmailAndLogsNotification_onAssignedEvent | PASS | TaskAssigned event triggers email and logs |
| 2 | listenSystemEvents_sendsAdminAlert | PASS | System error event sends admin alert |
| 3 | listenTaskEvents_logsFailure_whenEmailSendThrows | PASS | Email failure is logged gracefully |

---

## Run Command

```bash
mvn test -DskipITs=false
```

Full output is captured in `test-results-full.txt`.
