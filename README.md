# Habify — Daily Activity Tracker & Discipline Analytics System

Habify is a full-stack personal discipline and productivity tracking web application built using **Java 17**, **Spring Boot 3.3.3**, and modern Web technologies (`HTML5`, `CSS3`, `JavaScript`). It empowers users to define custom habits, track daily activities, maintain consistency streaks, calculate productivity scores, and view interactive performance analytics.

---

## Key Features

- **User Authentication & Profiles**: Secure user registration, authentication, and password encryption using BCrypt.
- **Core Habit Engine**: Create, edit, activate/deactivate, categorize, and prioritize habits with customizable frequencies and targets.
- **Daily Activity Logging**: Record habit completions (`COMPLETED`, `PARTIAL`, `MISSED`) with log notes and completion timestamps.
- **Streak & Scoring Engine**: Automatic real-time calculation of current streaks, longest streaks, daily completion percentages, and discipline scores.
- **Analytics & Insights**: Historical trend tracking, weekly/monthly completion rates, and category-wise performance analytics.
- **Interactive Dashboard**: Responsive web frontend with live progress widgets, daily trackers, and statistical summaries.

---

## Technology Stack

- **Backend**: Java 17, Spring Boot 3.3.3 (Spring Data JPA, Spring Validation, Spring Security Crypto)
- **Database**: Dual configuration supporting **H2 Database** (in-memory dev/testing) and **MySQL** (production)
- **Frontend**: HTML5, Vanilla CSS3, JavaScript (Fetch API, DOM manipulation)
- **Build & Dependency Management**: Apache Maven (`pom.xml`, Maven Wrapper `mvnw.cmd`)

---

## Project Architecture & Structure

```
Habify---Daily-Activity-Tracker/
├── pom.xml                                    # Maven dependencies & plugins
├── README.md                                  # Project documentation
├── src/
    ├── main/
    │   ├── java/com/habify/
    │   │   ├── HabifyApplication.java        # Spring Boot main application launcher
    │   │   ├── config/                        # App security & bean configurations
    │   │   ├── controller/                    # REST API Controllers (Auth, Habit, Log, Analytics)
    │   │   ├── dto/                           # Request & Response Data Transfer Objects
    │   │   ├── entity/                        # JPA Entities (User, Habit, HabitLog, Enums)
    │   │   ├── exception/                     # Global exception handling & error responses
    │   │   ├── repository/                    # Spring Data JPA Repositories
    │   │   └── service/                       # Business logic services (Habits, Logs, Streaks, Scores)
    │   └── resources/
    │       ├── application.properties          # Default Spring Boot & H2 database configuration
    │       ├── application-mysql.properties    # MySQL database configuration
    │       ├── schema-mysql.sql               # MySQL DDL table schemas
    │       └── static/                        # Static Web Frontend assets
    │           ├── index.html                 # Landing page
    │           ├── auth.html                  # Login & registration portal
    │           ├── dashboard.html             # User habits & analytics dashboard
    │           ├── css/                       # Modular CSS stylesheets
    │           └── js/                        # Frontend JavaScript controllers & API adapters
    └── test/                                  # Unit and integration test suites
```

---

## System Modules Breakdown

Habify is designed around 6 core modular areas:

1. **Module 1 — Core Habit & Tracking Engine**: Habit lifecycle management, activity records, and daily completion logs.
2. **Module 2 — User, Authentication & Profile System**: Account management, security validation, and session management.
3. **Module 4 — Analytics, Scoring & Insight Engine**: Algorithm-driven streak calculations, discipline scores, and progress trends.
4. **Module 5 — Web Frontend & Dashboard**: User interface layout, charts, dashboard components, and API consumption.
5. **Module 3 — Database & Persistence Layer**: Schema design, entity mappings, JPA query optimization, and database drivers.
6. **Module 6 — Goals, Achievements, Calendar & Reports**: Progress reports, milestone tracking, and historical calendar summaries.

---

## Getting Started

### Prerequisites
- **JDK 17** or higher installed
- Maven installed (or use included `./mvnw.cmd`)

### Running Locally (Development Mode)
1. Clone the repository:
   ```bash
   git clone https://github.com/iamriteshhh/Habify---Daily-Activity-Tracker.git
   cd Habify---Daily-Activity-Tracker
   ```
2. Build and run using the Maven wrapper:
   ```bash
   ./mvnw.cmd spring-boot:run
   ```
3. Open your browser and navigate to:
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

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/auth/register` | Register a new user account |
| `POST` | `/api/auth/login` | Authenticate user and receive credentials |
| `GET` | `/api/habits` | Retrieve all habits for the logged-in user |
| `POST` | `/api/habits` | Create a new habit |
| `PUT` | `/api/habits/{id}` | Update habit details |
| `DELETE` | `/api/habits/{id}` | Delete a habit |
| `POST` | `/api/habits/{id}/logs` | Log habit completion status for a date |
| `GET` | `/api/analytics/dashboard` | Fetch user dashboard metrics and discipline score |

---

## License

This project is developed for educational and academic purposes. All rights reserved.
