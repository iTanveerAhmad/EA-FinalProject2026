# Copilot Instructions: Real-Time Release Management System

## Architecture Overview

**Event-Driven Microservices** with Kafka-based communication:
- **Release Service** (port 8080): Core business logic, workflow enforcement, task management, chat, activity streams
- **Notification Service** (port 8081): Consumes task/system events, sends notifications, maintains audit logs

Both services use **MongoDB** (separate databases), **Spring Boot 3.2**, **Java 21**, and emit/consume events via Kafka topics: `task-events` and `system-events`.

## Key Architectural Patterns

### 1. Event-Driven Communication
Events flow from Release Service → Kafka → Notification Service. **Do not** call services directly; always publish events:

```java
// In ReleaseService or TaskController, use KafkaProducerService:
kafkaProducerService.sendTaskAssignedEvent(event);
kafkaProducerService.sendHotfixTaskAddedEvent(event);
```

Event types: `TaskAssignedEvent`, `TaskCompletedEvent`, `HotfixTaskAddedEvent`, `StaleTaskDetectedEvent`, `SystemErrorEvent`

### 2. Workflow Constraints (Critical Business Logic)
- **Single In-Process Rule**: Only ONE task per developer can be in-progress at any time
- **Sequential Task Execution**: Tasks must be completed in release order; cannot start task N until task N-1 is complete
- **Hotfix Logic**: When tasks are added to a completed release, automatically re-open the release

Enforce these in `ReleaseService.startTask()` and related methods. See [ReleaseService](release-service/src/main/java/com/example/releasesystem/release/service/ReleaseService.java).

### 3. Security & Authentication
- JWT-based auth; tokens issued by `JwtService`
- Two roles: `ADMIN` (manage releases), `DEVELOPER` (execute tasks)
- `JwtAuthenticationFilter` validates tokens on each request
- Password storage: `PasswordEncoder` (bcrypt via Spring Security)

See [security package](release-service/src/main/java/com/example/releasesystem/release/security/).

## Project Structure & Key Files

```
release-service/src/main/java/.../release/
├── controller/         # REST endpoints; validate inputs, call services
├── domain/            # MongoDB documents (@Document): Release, Task, User, ChatSession
├── dto/               # Request/response objects: AuthRequest, RegisterRequest
├── event/             # Event POJOs published to Kafka
├── service/           # Business logic; use @Service + @RequiredArgsConstructor (Lombok)
│   ├── ReleaseService           # Core workflow, constraint checking
│   ├── KafkaProducerService     # Publishes events
│   ├── ActivityStreamService    # Real-time updates (SSE)
│   └── OllamaService            # LLM integration
├── repository/        # Spring Data MongoDB @Repository interfaces
├── security/          # JWT, authentication, user details
├── scheduler/         # Scheduled tasks (e.g., StaleTaskScheduler)
└── exception/         # Global exception handling (@ControllerAdvice)

notification-service/src/main/java/.../notification/
├── service/
│   ├── KafkaConsumerService    # @KafkaListener handlers
│   └── EmailService            # Email simulation
├── domain/
│   └── NotificationLog         # Audit trail
├── repository/
└── event/              # Event classes (duplicated from release-service)
```

## Critical Developer Workflows

### Build & Package
```bash
# Build both services (from project root)
mvn clean package

# This compiles, runs tests, creates JAR in target/ folder
```

### Run Services (Local Development)
```bash
# Terminal 1: Start infrastructure
docker-compose up -d

# Terminal 2: Release Service
java -jar release-service/target/release-service-0.0.1-SNAPSHOT.jar

# Terminal 3: Notification Service
java -jar notification-service/target/notification-service-0.0.1-SNAPSHOT.jar
```

### Testing
- Test files in `src/test/java`; follow `*Test` naming convention
- Use Spring Boot Test with `@SpringBootTest`
- Mock Kafka with `@EmbeddedKafka` for integration tests

### Database
- MongoDB connection strings in `application.yml`: `mongodb://admin:adminpassword@localhost:27017/{db_name}?authSource=admin`
- Credentials hardcoded for local dev; use environment variables in production
- Separate databases: `release_db` (Release Service), `notification_db` (Notification Service)

## Conventions & Patterns

### Service Layer
- Use **Lombok** for boilerplate: `@Data`, `@RequiredArgsConstructor`, `@Slf4j`
- Use constructor injection (Lombok's `@RequiredArgsConstructor` + final fields)
- Services should NOT call other services; use events instead
- All services use `@Transactional` where needed (MongoDB-aware)

### Controllers
- Route per entity: `@RequestMapping("/releases")`, `@RequestMapping("/tasks")`
- Use `@PatchMapping` for state transitions (`/tasks/{id}/start`, `/tasks/{id}/complete`)
- Extract `developerId` from JWT context (currently passed as `@RequestParam` for simplicity)
- Return `ResponseEntity<T>` for consistent HTTP responses

### Domain Entities
- Use `@Document(collection = "...")` for MongoDB mapping
- Always include `@Id` field (String, auto-generated by MongoDB)
- Use `@Data @NoArgsConstructor @AllArgsConstructor` from Lombok

### Kafka Patterns
- **Producer**: Inject `KafkaProducerService`, call `sendTaskAssignedEvent(event)` etc.
- **Consumer**: Use `@KafkaListener(topics = "...", groupId = "notification-group")` on methods taking `ConsumerRecord<String, String>`
- Topic naming: `task-events` (for task lifecycle), `system-events` (for errors)
- Partition key: Event type (e.g., "assigned", "completed") for ordering guarantees

### Error Handling
- Catch exceptions in `GlobalExceptionHandler` (use `@RestControllerAdvice`)
- Log errors with `@Slf4j`; use `log.error()` for failures
- Publish `SystemErrorEvent` for critical failures

## Integration Points

### Ollama (AI Chat)
- Endpoint: `http://localhost:11434/api/chat` (REST)
- Service: `OllamaService` handles requests/responses
- Feature: Developers can create chat sessions and ask context-aware questions

### Prometheus & Grafana
- Metrics exposed at `http://localhost:9090`
- Both services have actuator endpoints enabled; view at `/actuator/prometheus`
- Grafana (port 3000) pulls metrics from Prometheus

### Server-Sent Events (SSE)
- Endpoint: `GET /activity/stream` in Release Service
- Service: `ActivityStreamService` emits activity updates in real-time
- Used for live activity feeds; SSE client subscribes in browser

## Common Gotchas & Tips

1. **Event Ordering**: If task completion must trigger downstream actions, verify event consumption order in Notification Service
2. **Duplicate Events**: Kafka guarantees at-least-once delivery; idempotence is caller's responsibility
3. **MongoDB Transactions**: Limited support across multiple documents; design for eventual consistency
4. **JWT Expiry**: Configure token TTL in `JwtService`; handle refresh tokens if needed
5. **Stale Task Detection**: `StaleTaskScheduler` runs periodically; check cron expression in `@Scheduled`

## Setup Checklist for New Contributors

- [ ] Clone repo; run `mvn clean package` to verify build
- [ ] Run `docker-compose up -d`; verify Kafka, MongoDB, Ollama start
- [ ] Run both services; test `/auth/register` and `/auth/login`
- [ ] Use Postman collection (`ReleaseSystem.postman_collection.json`) to explore APIs
- [ ] Trace workflow: create release → add tasks → assign to developer → start task → complete task
- [ ] Monitor activity stream at `/activity/stream`; check Notification Service logs for Kafka events
