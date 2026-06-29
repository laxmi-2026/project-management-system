# Project Management System — Backend

Task 3 — PIP — Spring Boot 3.x / Java 21 / MySQL / JWT / Role-Based Security

## Tech Stack
- Java 21
- Spring Boot 3.5.0
- Spring Security + JWT (jjwt 0.12.3)
- Spring Data JPA + MySQL 8
- MySQL Stored Procedures & Functions (PL/SQL-equivalent)
- SpringDoc OpenAPI (Swagger)
- JUnit 5 + Mockito

## Folder Structure (in the order things were built)

```
src/main/java/com/pms/
├── ProjectManagementApplication.java   ← main() entry point
├── model/              ← JPA entities (User, Project, Task)
├── dto/                ← request/response objects (records use Java 21 features)
├── repository/         ← Spring Data JPA repos + StoredProcedureService
├── security/           ← JwtUtil, JwtAuthFilter, SecurityConfig, UserDetailsServiceImpl
├── service/            ← business logic (AuthService, ProjectService, TaskService, DashboardService)
├── controller/         ← REST endpoints (AuthController, ProjectController, TaskController, DashboardController)
├── exception/          ← GlobalExceptionHandler + custom exceptions
└── config/             ← SwaggerConfig

src/main/resources/
├── application.properties
└── schema.sql           ← tables + stored procedures/functions (run this once manually)

src/test/java/com/pms/service/
├── AuthServiceTest.java     (7 tests)
├── ProjectServiceTest.java  (9 tests)
└── TaskServiceTest.java     (8 tests)
```

## Setup Steps

### 1. Create the database and run the schema script
```bash
mysql -u root -p < src/main/resources/schema.sql
```
This creates the `pmsdb` database, all 3 tables, the 2 stored procedures
(`sp_calculate_project_progress`, `sp_get_dashboard_summary`), the 1
stored function (`fn_count_overdue_tasks_for_user`), and seeds one admin
account:
- email: `admin@pms.com`
- password: `Admin@123`

### 2. Update application.properties
Edit `src/main/resources/application.properties` and set your own MySQL
password:
```properties
spring.datasource.password=YOUR_MYSQL_PASSWORD
```

### 3. Run the backend
```bash
./mvnw spring-boot:run
```
- API runs at: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

### 4. Run unit tests
```bash
./mvnw test
```
Expected: `Tests run: 24, Failures: 0, Errors: 0`

## Roles
| Role | Can Do |
|---|---|
| ROLE_ADMIN | Everything — create/edit/delete projects & tasks, delete any project (if no incomplete tasks), see all dashboards |
| ROLE_MANAGER | Create/edit projects & tasks (cannot delete projects) |
| ROLE_MEMBER | View assigned projects/tasks, drag-and-drop their own task status only |

## Why MySQL Stored Procedures (not just Java)
The brief asks for "PL/SQL procedures/functions for complex DB
operations." Oracle's syntax is PL/SQL; MySQL's procedural SQL is the
direct equivalent skill on a different database engine. `schema.sql`
contains:
1. **`sp_calculate_project_progress(project_id)`** — recalculates a
   project's `progress_percent` columan by counting DONE vs total tasks,
   called every time a task is created/updated/deleted/status-changed.
2. **`fn_count_overdue_tasks_for_user(user_id)`** — a scalar function
   used directly inside a SELECT to get one Member's overdue task count.
3. **`sp_get_dashboard_summary()`** — returns all 6 admin dashboard
   numbers in a single round trip instead of 6 separate queries.
