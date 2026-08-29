package com.habify.repository;

import com.habify.entity.Habit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * HabitRepository
 * 
 * Data access layer for Habit entity.
 * Provides user-scoped queries for habit management.
 */
@Repository
public interface HabitRepository extends JpaRepository<Habit, Long> {

    List<Habit> findByUserIdAndIsActiveTrue(Long userId);

    List<Habit> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Habit> findByIdAndUserId(Long id, Long userId);

    long countByUserIdAndIsActiveTrue(Long userId);

    long countByUserId(Long userId);
}
