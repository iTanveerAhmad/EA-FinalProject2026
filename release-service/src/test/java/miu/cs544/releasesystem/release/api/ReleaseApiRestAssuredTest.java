package miu.cs544.releasesystem.release.api;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * RestAssured integration tests for Release System REST API.
 * Uses Testcontainers for MongoDB and Kafka. Requires Docker to be running.
 * Skipped when Docker is unavailable; run with: docker-compose up -d mongodb kafka zookeeper (and mvn test)
 * or ensure Docker Desktop is running for Testcontainers.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
@EnabledIf("dockerAvailable")
class ReleaseApiRestAssuredTest {

    static boolean dockerAvailable() {
        try {
            DockerClientFactory.instance().client();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Container
    static MongoDBContainer mongo = new MongoDBContainer(DockerImageName.parse("mongo:7"));

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", () -> mongo.getConnectionString() + "/release_db");
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @LocalServerPort
    private int port;

    private String adminToken;
    private String developerToken;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.baseURI = "http://localhost";
    }

    private void registerAndLoginAdmin() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("username", "admin_rest", "password", "pass123", "role", "ADMIN"))
                .when()
                .post("/auth/register")
                .then()
                .statusCode(200);

        adminToken = given()
                .contentType(ContentType.JSON)
                .body(Map.of("username", "admin_rest", "password", "pass123"))
                .when()
                .post("/auth/login")
                .then()
                .statusCode(200)
                .extract()
                .path("token");
    }

    private void registerAndLoginDeveloper() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("username", "dev_rest", "password", "pass123", "role", "DEVELOPER"))
                .when()
                .post("/auth/register")
                .then()
                .statusCode(200);

        developerToken = given()
                .contentType(ContentType.JSON)
                .body(Map.of("username", "dev_rest", "password", "pass123"))
                .when()
                .post("/auth/login")
                .then()
                .statusCode(200)
                .extract()
                .path("token");
    }

    @Test
    void authRegister_returnsOk() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("username", "newuser", "password", "pass", "role", "DEVELOPER"))
                .when()
                .post("/auth/register")
                .then()
                .statusCode(200);
    }

    @Test
    void authLogin_returnsToken() {
        registerAndLoginAdmin();
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("username", "admin_rest", "password", "pass123"))
                .when()
                .post("/auth/login")
                .then()
                .statusCode(200)
                .body("token", notNullValue());
    }

    @Test
    void authLogin_invalidCredentials_returns401() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("username", "nonexistent", "password", "wrong"))
                .when()
                .post("/auth/login")
                .then()
                .statusCode(401);
    }

    @Test
    void releases_withoutAuth_returns401() {
        given()
                .when()
                .get("/releases")
                .then()
                .statusCode(401);
    }

    @Test
    void releases_asAdmin_returnsOk() {
        registerAndLoginAdmin();
        given()
                .header("Authorization", "Bearer " + adminToken)
                .when()
                .get("/releases")
                .then()
                .statusCode(200)
                .body("", anyOf(instanceOf(java.util.List.class), nullValue()));
    }

    @Test
    void releases_createRelease_asAdmin_returnsOk() {
        registerAndLoginAdmin();
        given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .body(Map.of("name", "Test Release", "description", "RestAssured test"))
                .when()
                .post("/releases")
                .then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("name", equalTo("Test Release"));
    }

    @Test
    void releases_asDeveloper_returns403() {
        registerAndLoginDeveloper();
        given()
                .header("Authorization", "Bearer " + developerToken)
                .contentType(ContentType.JSON)
                .body(Map.of("name", "Release", "description", "Desc"))
                .when()
                .post("/releases")
                .then()
                .statusCode(403);
    }

    @Test
    void tasks_getMyTasks_asDeveloper_returnsOk() {
        registerAndLoginDeveloper();
        given()
                .header("Authorization", "Bearer " + developerToken)
                .when()
                .get("/tasks/my")
                .then()
                .statusCode(200)
                .body("", instanceOf(java.util.List.class));
    }

    @Test
    void forum_addCommentAndGetComments_returnsOk() {
        registerAndLoginAdmin();
        String rId = given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .body(Map.of("name", "Forum Test Release", "description", "For forum tests"))
                .when()
                .post("/releases")
                .then()
                .statusCode(200)
                .extract()
                .path("id");
        String taskId = given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .body(Map.of("title", "Task 1", "description", "Desc", "assignedDeveloperId", "admin_rest", "orderIndex", 0))
                .when()
                .post("/releases/" + rId + "/tasks")
                .then()
                .statusCode(200)
                .extract()
                .path("tasks[0].id");
        given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .body(Map.of("content", "Test comment", "developerId", "admin_rest"))
                .when()
                .post("/tasks/" + taskId + "/comments")
                .then()
                .statusCode(200);
        given()
                .header("Authorization", "Bearer " + adminToken)
                .when()
                .get("/tasks/" + taskId + "/comments")
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1));
    }

    @Test
    void activity_stream_returnsSSE() {
        registerAndLoginAdmin();
        given()
                .header("Authorization", "Bearer " + adminToken)
                .when()
                .get("/activity/stream")
                .then()
                .statusCode(200)
                .contentType(containsString("text/event-stream"));
    }

    // -------------------------------------------------------------------------
    //  Additional tests for business rules & workflow constraints
    // -------------------------------------------------------------------------

    /**
     * Sequential Execution Rule:
     * Task 2 cannot be started before Task 1 is COMPLETED.
     */
    @Test
    void task_start_shouldFail_whenPreviousTaskNotCompleted() {
        registerAndLoginAdmin();
        registerAndLoginDeveloper();

        // 1) Admin creates release
        String rid = given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .body(Map.of("name", "Sequential Release", "description", "Seq test"))
                .when()
                .post("/releases")
                .then()
                .statusCode(200)
                .extract()
                .path("id");

        // 2) Admin adds two tasks for same developer with orderIndex 0 and 1
        String task1Id = given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .body(Map.of("title", "T1", "description", "First", "assignedDeveloperId", "dev_rest", "orderIndex", 0))
                .when()
                .post("/releases/" + rid + "/tasks")
                .then()
                .statusCode(200)
                .extract()
                .path("tasks[0].id");

        String task2Id = given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .body(Map.of("title", "T2", "description", "Second", "assignedDeveloperId", "dev_rest", "orderIndex", 1))
                .when()
                .post("/releases/" + rid + "/tasks")
                .then()
                .statusCode(200)
                .extract()
                .path("tasks[1].id");

        // 3) Developer tries to start Task 2 first -> should violate sequential rule (expect 409)
        given()
                .header("Authorization", "Bearer " + developerToken)
                .when()
                .patch("/tasks/" + task2Id + "/start")
                .then()
                .statusCode(409);

        // 4) Developer starts Task 1 -> OK
        given()
                .header("Authorization", "Bearer " + developerToken)
                .when()
                .patch("/tasks/" + task1Id + "/start")
                .then()
                .statusCode(200);
    }

    /**
     * Single In-Process Rule:
     * Developer cannot have more than one IN_PROCESS task.
     */
    @Test
    void task_start_shouldFail_whenDeveloperAlreadyHasInProcessTask() {
        registerAndLoginAdmin();
        registerAndLoginDeveloper();

        // 1) Admin creates release
        String rid = given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .body(Map.of("name", "Single InProcess Release", "description", "Rule test"))
                .when()
                .post("/releases")
                .then()
                .statusCode(200)
                .extract()
                .path("id");

        // 2) Two tasks assigned to same developer, sequential order (0, 1)
        String task1Id = given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .body(Map.of("title", "T1", "description", "First", "assignedDeveloperId", "dev_rest", "orderIndex", 0))
                .when()
                .post("/releases/" + rid + "/tasks")
                .then()
                .statusCode(200)
                .extract()
                .path("tasks[0].id");

        String task2Id = given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .body(Map.of("title", "T2", "description", "Second", "assignedDeveloperId", "dev_rest", "orderIndex", 1))
                .when()
                .post("/releases/" + rid + "/tasks")
                .then()
                .statusCode(200)
                .extract()
                .path("tasks[1].id");

        // 3) Start Task 1 as developer -> OK
        given()
                .header("Authorization", "Bearer " + developerToken)
                .when()
                .patch("/tasks/" + task1Id + "/start")
                .then()
                .statusCode(200);

        // 4) Try to start Task 2 while Task 1 is still IN_PROCESS -> should fail with 409
        given()
                .header("Authorization", "Bearer " + developerToken)
                .when()
                .patch("/tasks/" + task2Id + "/start")
                .then()
                .statusCode(409);
    }

    /**
     * Release completion rule:
     * Release cannot be marked as COMPLETED if any task is not COMPLETED.
     */
    @Test
    void completeRelease_shouldFail_whenTasksNotAllCompleted() {
        registerAndLoginAdmin();
        registerAndLoginDeveloper();

        // 1) Admin creates release
        String rid = given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .body(Map.of("name", "Completion Release", "description", "Completion rule test"))
                .when()
                .post("/releases")
                .then()
                .statusCode(200)
                .extract()
                .path("id");

        // 2) Two tasks for same developer, order 0 and 1
        String task1Id = given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .body(Map.of("title", "T1", "description", "First", "assignedDeveloperId", "dev_rest", "orderIndex", 0))
                .when()
                .post("/releases/" + rid + "/tasks")
                .then()
                .statusCode(200)
                .extract()
                .path("tasks[0].id");

        String task2Id = given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .body(Map.of("title", "T2", "description", "Second", "assignedDeveloperId", "dev_rest", "orderIndex", 1))
                .when()
                .post("/releases/" + rid + "/tasks")
                .then()
                .statusCode(200)
                .extract()
                .path("tasks[1].id");

        // 3) Start and complete only Task 1
        given()
                .header("Authorization", "Bearer " + developerToken)
                .when()
                .patch("/tasks/" + task1Id + "/start")
                .then()
                .statusCode(200);

        given()
                .header("Authorization", "Bearer " + developerToken)
                .when()
                .patch("/tasks/" + task1Id + "/complete")
                .then()
                .statusCode(200);

        // 4) Try to complete release while Task 2 is still TODO -> expect 409
        given()
                .header("Authorization", "Bearer " + adminToken)
                .when()
                .patch("/releases/" + rid + "/complete")
                .then()
                .statusCode(409);

        // 5) Complete Task 2 and then complete release -> expect 200
        given()
                .header("Authorization", "Bearer " + developerToken)
                .when()
                .patch("/tasks/" + task2Id + "/start")
                .then()
                .statusCode(200);

        given()
                .header("Authorization", "Bearer " + developerToken)
                .when()
                .patch("/tasks/" + task2Id + "/complete")
                .then()
                .statusCode(200);

        given()
                .header("Authorization", "Bearer " + adminToken)
                .when()
                .patch("/releases/" + rid + "/complete")
                .then()
                .statusCode(200);
    }

    @Test
    void actuator_health_isPublic() {
        given()
                .when()
                .get("/actuator/health")
                .then()
                .statusCode(200);
    }
}
