package com.teamtask.team_task_manager.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.teamtask.team_task_manager.model.Goal;

public interface GoalRepository extends JpaRepository<Goal, Long> { 
    List<Goal> findByProjectId(Long projectId);
}
