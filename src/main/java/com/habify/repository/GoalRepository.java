package com.habify.repository;

import com.habify.entity.Goal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GoalRepository extends JpaRepository<Goal, Long> {

    List<Goal> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Goal> findByIdAndUserId(Long id, Long userId);

    List<Goal> findByUserIdAndStatus(Long userId, com.habify.entity.GoalStatus status);

    long countByUserId(Long userId);
}
