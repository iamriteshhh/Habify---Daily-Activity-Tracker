package com.habify.repository;

import com.habify.entity.HabitLog;
import com.habify.entity.HabitStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * HabitLogRepository
 * 
 * Data access layer for daily activity tracking logs.
 * Enables analytics queries, streak computations, and daily logs retrieval.
 */
@Repository
public interface HabitLogRepository extends JpaRepository<HabitLog, Long> {

    Optional<HabitLog> findByHabitIdAndLogDate(Long habitId, LocalDate logDate);

    List<HabitLog> findByHabitIdOrderByLogDateDesc(Long habitId);

    List<HabitLog> findByHabitIdAndLogDateBetweenOrderByLogDateAsc(Long habitId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT l FROM HabitLog l WHERE l.habit.user.id = :userId AND l.logDate = :logDate")
    List<HabitLog> findByUserIdAndLogDate(@Param("userId") Long userId, @Param("logDate") LocalDate logDate);

    @Query("SELECT l FROM HabitLog l WHERE l.habit.user.id = :userId AND l.logDate BETWEEN :startDate AND :endDate ORDER BY l.logDate ASC")
    List<HabitLog> findByUserIdAndLogDateBetween(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT COUNT(l) FROM HabitLog l WHERE l.habit.user.id = :userId AND l.logDate = :logDate AND l.status = :status")
    long countByUserIdAndLogDateAndStatus(
            @Param("userId") Long userId,
            @Param("logDate") LocalDate logDate,
            @Param("status") HabitStatus status
    );

    @Query("SELECT l FROM HabitLog l WHERE l.habit.id = :habitId AND l.logDate <= :date ORDER BY l.logDate DESC")
    List<HabitLog> findPastLogsByHabitId(@Param("habitId") Long habitId, @Param("date") LocalDate date);
}
