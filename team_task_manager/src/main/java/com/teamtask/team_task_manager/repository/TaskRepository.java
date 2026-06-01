package com.teamtask.team_task_manager.repository;

import java.util.List;
import java.util.Map;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.teamtask.team_task_manager.model.Status;
import com.teamtask.team_task_manager.model.Task;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByGoalId(Long goalId);
    List<Task> findByProjectIdAndGoalIsNull(Long projectId);
    List<Task> findByProjectId(Long projectId);
    List<Task> findByAssigneeId(Long userId);
    List<Task> findByAssigneeIdAndStatus(Long userId, Status status);
    List<Task> findByProjectIdAndStatus(Long projectId, Status status);

    @Query("SELECT t.status, COUNT(t) FROM Task t WHERE t.project.id = :projectId GROUP BY t.status")
    List<Object[]> countByProjectIdGroupByStatus(@Param("projectId") Long projectId);

    @Query("SELECT t.assignee.username, COUNT(t) FROM Task t WHERE t.project.id = :projectId GROUP BY t.assignee")
    List<Object[]> countTasksByExecutor(@Param("projectId") Long projectId);

    Long countByProjectId(Long projectId);

    void deleteByProjectId(Long projectId);
}
