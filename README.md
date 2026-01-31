# Real-Time Release Management System

A comprehensive backend platform designed for fast-growing software companies to manage software releases, enforce strict workflows, and facilitate real-time collaboration.

## 🚀 Project Overview

This system manages the full lifecycle of software releases, from task assignment to completion and hotfixes. It emphasizes strict process enforcement, real-time updates, and AI-driven developer assistance.

### Key Features
*   **Workflow Enforcement:** Ensures tasks are executed sequentially and enforces a strict "single task in-process" rule per developer.
*   **Event-Driven Architecture:** Utilizes **Apache Kafka** for decoupled communication between release management and notifications.
*   **Real-Time Collaboration:** Features **Server-Sent Events (SSE)** for live activity feeds and updates.
*   **AI Integration:** Embedded **Ollama** chatbot to assist developers with context-aware queries.
*   **Hotfix Management:** Automated logic to handle post-release hotfixes, including re-opening releases and notifying stakeholders.
*   **Observability:** Integrated **Prometheus** and **Grafana** for real-time metrics and system health monitoring.

## 🏗 System Architecture

The project consists of two primary microservices and a Dockerized infrastructure layer.

### 1. Release Service (Port 8080)
*   **Responsibilities:** Core business logic, workflow management, task tracking, forum discussions, and AI chat.
*   **Tech Stack:** Spring Boot, Spring Data MongoDB, Spring Kafka, Spring WebFlux (SSE).
*   **Database:** MongoDB (`release_db`).

### 2. Notification Service (Port 8081)
*   **Responsibilities:** Consumes Kafka events to send notifications (email simulation) and maintain audit logs.
*   **Tech Stack:** Spring Boot, Spring Kafka, Spring Data MongoDB.
*   **Database:** MongoDB (`notification_db`).

### Infrastructure (Docker Compose)
*   **Message Broker:** Kafka + Zookeeper
*   **Database:** MongoDB
*   **AI Model:** Ollama (Local LLM)
*   **Monitoring:** Prometheus + Grafana

## 🛠 Prerequisites

*   **Java:** JDK 17 or higher
*   **Build Tool:** Maven 3.6+
*   **Containerization:** Docker & Docker Compose

## 🚦 Getting Started

### 1. Start Infrastructure
Run the required backing services (MongoDB, Kafka, Ollama, Prometheus, Grafana) using Docker Compose.

```bash
docker-compose up -d
```

### 2. Build the Project
Build both microservices using Maven.

```bash
mvn clean package
```

### 3. Run the Services
You can run the services independently in separate terminal windows.

**Run Release Service:**
```bash
java -jar release-service/target/release-service-0.0.1-SNAPSHOT.jar
```

**Run Notification Service:**
```bash
java -jar notification-service/target/notification-service-0.0.1-SNAPSHOT.jar
```

## 🔌 API Endpoints

### Authentication
*   `POST /auth/register` - Register a new user (`ADMIN` or `DEVELOPER`).
*   `POST /auth/login` - Authenticate and receive a JWT.

### Releases (Admin)
*   `POST /releases` - Create a new release.
*   `POST /releases/{id}/tasks` - Add tasks to a release (triggers Hotfix logic if release is completed).
*   `PATCH /releases/{id}/complete` - Mark a release as completed.

### Tasks (Developer)
*   `PATCH /tasks/{id}/start` - Start a task (Validates sequential order & global constraints).
*   `PATCH /tasks/{id}/complete` - Complete a task.

### Real-time & Chat
*   `GET /activity/stream` - Subscribe to real-time activity feed (SSE).
*   `POST /chat/session` - Start an AI chat session.
*   `POST /chat/{sessionId}/message` - Send a message to the AI assistant.

## 📊 Monitoring
*   **Prometheus:** `http://localhost:9090`
*   **Grafana:** `http://localhost:3000` (Default login: `admin` / `admin`)
"# MIU_CS544_Project" 
"# koznak-MIU_CS544_Project" 
