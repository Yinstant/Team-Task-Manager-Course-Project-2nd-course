package com.teamtask.team_task_manager.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.teamtask.team_task_manager.model.Status;
import com.teamtask.team_task_manager.model.Task;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByGoalId(Long goalId);
    List<Task> findByProjectIdAndGoalIsNull(Long projectId);
    List<Task> findByProjectId(Long projectId);
    List<Task> findByAssigneeId(Long userId);
    List<Task> findByAssigneeIdAndStatus(Long userId, Status status);
    List<Task> findByProjectIdAndStatus(Long projectId, Status status);
}
