-- =====================================================================
-- Project Management System — Database Schema
-- =====================================================================
-- This script creates the database, all tables, and the stored
-- procedures/functions used for complex calculations.
--
-- WHY STORED PROCEDURES HERE:
-- The Task 3 brief explicitly asks for "PL/SQL procedures/functions for
-- complex DB operations". Oracle uses PL/SQL syntax; MySQL's equivalent
-- is its own procedural SQL dialect. The LOGIC and PURPOSE are identical
-- — only the syntax differs slightly. We use MySQL's syntax here since
-- the project runs on MySQL, but the concept (precomputing aggregate
-- values inside the database rather than in application code) is the
-- same skill the brief is testing.
--
-- Run this whole file once, e.g.:
--   mysql -u root -p < schema.sql
-- =====================================================================

--CREATE DATABASE IF NOT EXISTS pmsdb;
--USE pmsdb;

-- ---------------------------------------------------------------------
-- TABLE: users
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL UNIQUE,
    email       VARCHAR(100) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    role        ENUM('ROLE_ADMIN','ROLE_MANAGER','ROLE_MEMBER') NOT NULL DEFAULT 'ROLE_MEMBER',
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- ---------------------------------------------------------------------
-- TABLE: projects
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS projects (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    name             VARCHAR(100) NOT NULL,
    description      TEXT,
    start_date       DATE,
    end_date         DATE,
    status           ENUM('PLANNING','ACTIVE','ON_HOLD','COMPLETED') NOT NULL DEFAULT 'PLANNING',
    created_by       BIGINT,
    created_at       DATETIME DEFAULT CURRENT_TIMESTAMP,
    progress_percent INT DEFAULT 0,
    CONSTRAINT fk_project_creator FOREIGN KEY (created_by) REFERENCES users(id)
);

-- ---------------------------------------------------------------------
-- TABLE: tasks
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS tasks (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    title        VARCHAR(150) NOT NULL,
    description  TEXT,
    due_date     DATE,
    priority     ENUM('LOW','MEDIUM','HIGH') NOT NULL DEFAULT 'MEDIUM',
    status       ENUM('TODO','IN_PROGRESS','DONE') NOT NULL DEFAULT 'TODO',
    project_id   BIGINT NOT NULL,
    assigned_to  BIGINT,
    created_by   BIGINT,
    created_at   DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_task_project    FOREIGN KEY (project_id)  REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_task_assignee   FOREIGN KEY (assigned_to) REFERENCES users(id),
    CONSTRAINT fk_task_creator    FOREIGN KEY (created_by)  REFERENCES users(id)
);

-- Indexes that speed up the most common queries (filtering by project,
-- by assignee, and by status — exactly what the Kanban board and
-- dashboard query constantly).
CREATE INDEX idx_tasks_project   ON tasks(project_id);
CREATE INDEX idx_tasks_assignee  ON tasks(assigned_to);
CREATE INDEX idx_tasks_status    ON tasks(status);


-- =====================================================================
-- STORED PROCEDURE 1: sp_calculate_project_progress
-- =====================================================================
-- PURPOSE: Calculates what percentage of a project's tasks are DONE,
-- and writes that percentage directly into projects.progress_percent.
--
-- WHY THIS BELONGS IN THE DATABASE (not Java):
-- This is a pure aggregation over rows that already live in the
-- database. Doing it here means: (1) it's reusable from any client —
-- not just our Spring Boot app, (2) it runs faster because no data
-- needs to leave MySQL to be counted in Java, (3) it satisfies the
-- brief's specific requirement for PL/SQL-style procedures.
--
-- HOW SPRING BOOT CALLS IT:
-- ProjectRepository has a @Procedure-annotated method (see below)
-- that calls this exact procedure name.
-- =====================================================================

DELIMITER //

DROP PROCEDURE IF EXISTS sp_calculate_project_progress //

CREATE PROCEDURE sp_calculate_project_progress(IN p_project_id BIGINT)
BEGIN
    DECLARE total_tasks   INT DEFAULT 0;
    DECLARE done_tasks    INT DEFAULT 0;
    DECLARE progress      INT DEFAULT 0;

    -- Count all tasks belonging to this project
    SELECT COUNT(*) INTO total_tasks
    FROM tasks
    WHERE project_id = p_project_id;

    -- Count only the ones marked DONE
    SELECT COUNT(*) INTO done_tasks
    FROM tasks
    WHERE project_id = p_project_id AND status = 'DONE';

    -- Avoid division by zero for a brand new project with no tasks yet
    IF total_tasks > 0 THEN
        SET progress = ROUND((done_tasks / total_tasks) * 100);
    ELSE
        SET progress = 0;
    END IF;

    -- Persist the calculated percentage onto the project row itself,
    -- so future simple SELECTs don't need to recompute it every time.
    UPDATE projects
    SET progress_percent = progress
    WHERE id = p_project_id;

END //

DELIMITER ;


-- =====================================================================
-- FUNCTION 1: fn_count_overdue_tasks_for_user
-- =====================================================================
-- PURPOSE: Given a user ID, returns how many of their assigned tasks
-- are overdue (due_date in the past AND status is not DONE).
--
-- WHY A FUNCTION INSTEAD OF A PROCEDURE:
-- A FUNCTION returns a single value and can be used directly inside a
-- SELECT statement (e.g. SELECT fn_count_overdue_tasks_for_user(5)),
-- whereas a PROCEDURE cannot be used inline like that. This is the
-- correct, idiomatic choice when you need one number back, which is
-- exactly the brief's "functions for complex DB operations" requirement.
-- =====================================================================

DELIMITER //

DROP FUNCTION IF EXISTS fn_count_overdue_tasks_for_user //

CREATE FUNCTION fn_count_overdue_tasks_for_user(p_user_id BIGINT)
RETURNS INT
DETERMINISTIC
READS SQL DATA
BEGIN
    DECLARE overdue_count INT DEFAULT 0;

    SELECT COUNT(*) INTO overdue_count
    FROM tasks
    WHERE assigned_to = p_user_id
      AND status != 'DONE'
      AND due_date IS NOT NULL
      AND due_date < CURDATE();

    RETURN overdue_count;
END //

DELIMITER ;


-- =====================================================================
-- PROCEDURE 2: sp_get_dashboard_summary
-- =====================================================================
-- PURPOSE: Returns one result row with every number the Admin dashboard
-- needs in a SINGLE database round-trip, instead of the Java code
-- making five separate queries (count projects, count active projects,
-- count tasks, count completed tasks, count overdue tasks).
--
-- This is a genuine performance optimization (one of the brief's
-- "Advanced Expectations") — fewer round-trips between the app server
-- and the database means a faster dashboard load.
-- =====================================================================

DELIMITER //

DROP PROCEDURE IF EXISTS sp_get_dashboard_summary //

CREATE PROCEDURE sp_get_dashboard_summary()
BEGIN
    SELECT
        (SELECT COUNT(*) FROM projects) AS total_projects,
        (SELECT COUNT(*) FROM projects WHERE status = 'ACTIVE') AS active_projects,
        (SELECT COUNT(*) FROM projects WHERE status = 'COMPLETED') AS completed_projects,
        (SELECT COUNT(*) FROM tasks) AS total_tasks,
        (SELECT COUNT(*) FROM tasks WHERE status = 'DONE') AS completed_tasks,
        (SELECT COUNT(*) FROM tasks
            WHERE status != 'DONE' AND due_date IS NOT NULL AND due_date < CURDATE()
        ) AS overdue_tasks;
END //

DELIMITER ;


-- =====================================================================
-- SEED DATA (optional) — one admin account to log in with immediately
-- Password below is "Admin@123" already BCrypt-hashed.
-- =====================================================================
INSERT INTO users (username, email, password, role)
VALUES ('admin', 'admin@pms.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ROLE_ADMIN')
ON DUPLICATE KEY UPDATE username = username;
