# ⚡ Habify — Daily Activity Tracker & Discipline Analytics

[![Java](https://img.shields.io/badge/Java-17%2B-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3.3-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Hibernate / JPA](https://img.shields.io/badge/Hibernate-JPA-59666C?style=for-the-badge&logo=hibernate&logoColor=white)](https://hibernate.org/)
[![Database](https://img.shields.io/badge/Database-MySQL%20%7C%20H2-4479A1?style=for-the-badge&logo=mysql&logoColor=white)](https://www.mysql.com/)
[![Frontend](https://img.shields.io/badge/Frontend-HTML5%20%2F%20CSS3%20%2F%20ES6-E34F26?style=for-the-badge&logo=javascript&logoColor=white)](https://developer.mozilla.org/)
[![Build](https://img.shields.io/badge/Build-Maven-C71A36?style=for-the-badge&logo=apache-maven&logoColor=white)](https://maven.apache.org/)
[![Tests](https://img.shields.io/badge/Tests-18%20Passing-success?style=for-the-badge&logo=junit5&logoColor=white)](https://junit.org/junit5/)

> **Build unshakeable habits, maintain unbroken streaks, and measure your personal discipline through real-time analytics.**

---

## 📖 Table of Contents

- [Overview](#-overview)
- [Key Features](#-key-features)
- [System Architecture](#-system-architecture)
- [Core Algorithms & Analytics Engine](#-core-algorithms--analytics-engine)
- [Tech Stack](#-tech-stack)
- [Project Structure](#-project-structure)
- [REST API Reference](#-rest-api-reference)
- [Database Configuration & Profiles](#-database-configuration--profiles)
- [Getting Started & Installation](#-getting-started--installation)
- [Testing & Quality Assurance](#-testing--quality-assurance)
- [Screenshots & UI Preview](#-screenshots--ui-preview)
- [Future Roadmap](#-future-roadmap)
- [License & Authors](#-license--authors)

---

## 🌟 Overview

**Habify** is a modern, full-stack personal discipline, daily habit tracking, and productivity analytics platform. Built using **Spring Boot 3 (Java 17)** and a responsive **Vanilla JavaScript / CSS3 Single-Page Interface**, Habify empowers individuals to cultivate positive routines, visualize consistency, and track progress over time.

Habify eliminates the friction of traditional habit apps by providing:
1. **Instant Guest Exploration**: Try the interactive weekly habit board immediately with zero setup.
2. **Frequency-Aware Streaks**: Rest days (e.g., gym on Mon/Wed/Fri) never penalize or break your active streak.
3. **Mathematical Discipline Scoring**: A rolling 7-day weighted completion index reflecting true habit execution.
4. **Interactive Horizontal Weekly Board**: One-click logging and instant visual feedback across the current week.

---
```

---

## System Modules Breakdown

Habify is designed around 6 core modular areas:

1. **Module 1 — Core Habit & Tracking Engine**: Habit lifecycle management, activity records, and daily completion logs.
2. **Module 2 — User, Authentication & Profile System**: Account management, security validation, and session management.
3. **Module 3 — Database & Persistence Layer**: Schema design, entity mappings, JPA query optimization, and database drivers.
4. **Module 4 — Analytics, Scoring & Insight Engine**: Algorithm-driven streak calculations, discipline scores, and progress trends.
5. **Module 5 — Web Frontend & Dashboard**: User interface layout, charts, dashboard components, and API consumption.
6. **Module 6 — Goals, Achievements, Calendar & Reports**: Progress reports, milestone tracking, and historical calendar summaries.

---

## Getting Started

### Prerequisites
- **JDK 17** or higher installed
- Maven installed (or use included `mvnw.cmd` / `./mvnw.cmd`)
- Modern web browser (Chrome, Edge, Firefox, Safari)

### Running Locally (Development Mode)

Windows (PowerShell):
```powershell
cd "d:\PBL\Habify - Daily Activity Tracker"
.\mvnw.cmd spring-boot:run
```

Unix / cross-platform (bash):
```bash
git clone https://github.com/iamriteshhh/Habify---Daily-Activity-Tracker.git
cd Habify---Daily-Activity-Tracker
./mvnw.cmd spring-boot:run
```

Open your browser and navigate to:
```
http://localhost:8080
```

### Running Tests
To run unit and integration test suites:
```bash
./mvnw.cmd test
```

---

## Key REST API Endpoints

The detailed REST API Reference appears above under "REST API Reference". The most-used endpoints include:

- `POST /api/auth/register` — Register a new user
- `POST /api/auth/login` — Authenticate and receive credentials
- `GET /api/habits` — Retrieve habits for the logged-in user
- `POST /api/habits` — Create a new habit
- `PUT /api/habits/{id}` — Update habit details
- `DELETE /api/habits/{id}` — Delete a habit
- `POST /api/habits/{id}/logs` — Log habit completion
- `GET /api/analytics/dashboard` — Fetch dashboard metrics

---

## License & Authors

Developed as a **Personal Discipline & Daily Activity Tracking System**.

- **Author**: PBL Development Team
- **Version**: `1.0.0-SNAPSHOT`
- **Framework**: Spring Boot 3.3.3 / Java 17

*⚡ Stay consistent. Build habits. Track progress with Habify.*
