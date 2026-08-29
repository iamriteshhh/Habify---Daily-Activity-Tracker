-- =========================================================
-- Habify (Phase 1) - Database Schema Reference (MySQL DDL)
-- =========================================================

CREATE DATABASE IF NOT EXISTS habify_db;
USE habify_db;

-- 1. Users Table
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(120) NOT NULL UNIQUE,
    username VARCHAR(60) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Habits Table
CREATE TABLE IF NOT EXISTS habits (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    category VARCHAR(50) NOT NULL DEFAULT 'Other',
    priority VARCHAR(20) DEFAULT 'MEDIUM',
    frequency_type VARCHAR(30) NOT NULL DEFAULT 'DAILY', -- 'DAILY' or 'SPECIFIC_DAYS'
    scheduled_days VARCHAR(100), -- Comma-separated: 'MONDAY,WEDNESDAY,FRIDAY'
    target_value INT DEFAULT 1,
    unit VARCHAR(30) DEFAULT 'times',
    preferred_time VARCHAR(20), -- 'MORNING', 'AFTERNOON', 'EVENING', 'ANYTIME'
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_habit_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 3. Habit Logs Table
CREATE TABLE IF NOT EXISTS habit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    habit_id BIGINT NOT NULL,
    log_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL, -- 'COMPLETED', 'PARTIAL', 'MISSED'
    completion_percentage INT NOT NULL DEFAULT 0, -- 0 to 100
    completion_time TIME,
    note VARCHAR(300),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_log_habit FOREIGN KEY (habit_id) REFERENCES habits(id) ON DELETE CASCADE,
    CONSTRAINT uq_habit_date UNIQUE (habit_id, log_date)
);

-- Indexes for optimal analytics query performance
CREATE INDEX IF NOT EXISTS idx_habits_user_active ON habits(user_id, is_active);
CREATE INDEX IF NOT EXISTS idx_logs_habit_date ON habit_logs(habit_id, log_date);
CREATE INDEX IF NOT EXISTS idx_logs_date ON habit_logs(log_date);
