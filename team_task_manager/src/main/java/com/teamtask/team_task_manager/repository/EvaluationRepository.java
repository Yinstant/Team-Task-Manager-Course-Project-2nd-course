package com.teamtask.team_task_manager.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.teamtask.team_task_manager.model.Evaluation;

public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {
    List<Evaluation> findByReviewerId(Long reviewerId);
    List<Evaluation> findByTaskId(Long taskId);
}
