# Opster System - Gemini Context

## Project Overview
**Opster** is a full-stack operations management system designed to streamline server management, service deployment, and monitoring. It features a modern web interface and a robust Java backend with AI capabilities.

*   **Type:** Full-stack Web Application
*   **Backend:** Java 25, Spring Boot 3.5.9
*   **Frontend:** Vue 3, Vite, Element Plus
*   **Database:** SQLite

## Architecture & Technologies

### Backend (`opster-backend`)
*   **Framework:** Spring Boot 3.5.9
*   **Language:** Java 25
*   **ORM:** Spring Data JPA with Hibernate Community Dialect for SQLite
*   **Database:** SQLite (`opster.db`)
*   **AI Integration:** Spring AI Alibaba (DashScope `qwen-plus`) for intelligent command generation.
*   **SSH:** JSch for remote server interaction.
*   **WebSocket:** Real-time communication for terminal, logs, and execution status.
*   **Utils:** Hutool for general utility functions.

### Frontend (`opster-frontend`)
*   **Framework:** Vue 3
*   **Build Tool:** Vite
*   **UI Library:** Element Plus
*   **HTTP Client:** Axios
*   **Terminal:** Xterm.js for web-based terminal access.

## Key Features
1.  **Project Management:** Manage project metadata (Git repo, monitoring URL, etc.).
2.  **Server Management:** Manage server credentials and connectivity details.
3.  **Service Management:** Link projects to servers, define deployment scripts, and perform actions (Start, Stop, Restart, Deploy).
4.  **Deployment Records:** Track deployment history and status.
5.  **Scheduled Deployments:** Schedule automated deployment tasks.
6.  **Web Terminal:** Direct SSH access to servers via the browser.
7.  **AI Assistant:** AI-powered Linux command generation to assist with operations.
8.  **Dashboard:** Overview of system statistics.

## Setup & Development

### Prerequisites
*   **Java:** JDK 25
*   **Maven:** 4.0.0+
*   **Node.js:** 18+

### Configuration
*   **Backend Config:** `opster-backend/src/main/resources/application.yml`
    *   **Port:** 8080
    *   **Database:** `jdbc:sqlite:opster.db` (Auto-created)
    *   **External Tools:** Adjust `opster.maven-home` and `opster.java-home` to match your local environment if using local execution features.
    *   **AI Key:** Configured under `spring.ai.dashscope.api-key`.

### Running the Application

**1. Backend**
```bash
cd opster-backend
mvn spring-boot:run
```
*   The application will start on `http://localhost:8080`.
*   Swagger UI (if available) or API endpoints are accessible here.

**2. Frontend**
```bash
cd opster-frontend
npm install
npm run dev
```
*   The frontend will start on `http://localhost:5173` (proxies API requests to backend).

## Directory Structure

```
opster/
├── GEMINI.md          # Context file for AI agents
├── opster.sql         # SQL schema for manual DB initialization
├── opster-backend/    # Spring Boot Backend
│   ├── src/main/java/com/opster/
│   │   ├── config/    # App configurations (JPA, Web, WebSocket)
│   │   ├── handler/   # WebSocket handlers
│   │   ├── module/    # Feature modules (Controller, Service, Repository, Entity)
│   │   │   ├── dashboard/
│   │   │   ├── deployment/
│   │   │   ├── project/
│   │   │   ├── schedule/
│   │   │   ├── server/
│   │   │   ├── service/
│   │   │   └── terminal/ # AI Command Service & Terminal Logic
│   │   └── OpsterApplication.java
│   └── src/main/resources/
│       └── application.yml
└── opster-frontend/   # Vue 3 Frontend
    ├── src/
    │   ├── api/       # Axios request definitions
    │   ├── views/     # Page components (Dashboard, Server, etc.)
    │   └── App.vue
    └── package.json
```

## Development Conventions
*   **API Style:** RESTful APIs using Spring MVC.
*   **Database:** Use JPA repositories. Entities extend `BaseEntity` for auditing (create/update time).
*   **Frontend:** Composition API with `<script setup>`.
*   **Communication:**
    *   REST for CRUD operations.
    *   WebSocket for streaming data (logs, terminal).
