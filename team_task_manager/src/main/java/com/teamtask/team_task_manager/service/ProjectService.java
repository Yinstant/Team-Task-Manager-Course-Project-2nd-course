package com.teamtask.team_task_manager.service;

import com.teamtask.team_task_manager.repository.MembershipRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.teamtask.team_task_manager.model.Goal;
import com.teamtask.team_task_manager.model.Membership;
import com.teamtask.team_task_manager.model.Project;
import com.teamtask.team_task_manager.model.Role;
import com.teamtask.team_task_manager.model.Status;
import com.teamtask.team_task_manager.model.Task;
import com.teamtask.team_task_manager.model.User;
import com.teamtask.team_task_manager.repository.GoalRepository;
import com.teamtask.team_task_manager.repository.ProjectRepository;
import com.teamtask.team_task_manager.repository.TaskRepository;
import com.teamtask.team_task_manager.repository.UserRepository;

@Service
public class ProjectService {
    private final MembershipRepository membershipRepository;
    @Autowired ProjectRepository projectRepository;
    @Autowired UserRepository userRepository;
    @Autowired GoalRepository goalRepository;
    @Autowired TaskRepository taskRepository;

    ProjectService(MembershipRepository membershipRepository) {
        this.membershipRepository = membershipRepository;
    }

    public User GetCurrentUser(){
        String username = SecurityContextHolder
            .getContext()
            .getAuthentication()
            .getName();
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public boolean HasAccess(Long projectId){
        User currentUser = GetCurrentUser();

        Boolean isContains = membershipRepository.findByProjectIdAndUserId(projectId, currentUser.getId()).isPresent();

        return isContains;
    }

    public Project GetProjectIfAccessible(Long projectId){
        if (!HasAccess(projectId)){
            throw new AccessDeniedException("У вас нет доступа к этому проекту");
        }

        Project currentProject = projectRepository
            .findById(projectId)
            .orElse(null);

        return currentProject;
    }

    public List<Project> GetUserProjects(){
        String username = SecurityContextHolder
            .getContext()
            .getAuthentication()
            .getName();

        return membershipRepository.findAllByUserUsername(username).stream()
            .map(Membership::getProject).collect(Collectors.toList());
    }

    public Map<Goal, List<Task>> GetTasksByGoal(Long projectId){
        List<Goal> goals = goalRepository.findByProjectId(projectId);
        Map<Goal, List<Task>> tasksByGoal = new LinkedHashMap<>();

        for (Goal goal : goals){
            tasksByGoal.put(goal, taskRepository.findByGoalId(goal.getId()));
        }

        return tasksByGoal;
    }

    public Map<String, List<Task>> GetTasksByStatus(Long projectId){
        List<Task> tasks = taskRepository.findByProjectId(projectId);
        return tasks.stream().collect(Collectors.groupingBy(t -> t.getStatus().name()));
    }


    private boolean hasRole(Long projectId, Long userId, Role role) {
        Membership curMemb = membershipRepository.findByProjectIdAndUserId(projectId, userId)
            .orElseThrow(() -> new RuntimeException("Membership not found"));
        return curMemb != null && curMemb.getRoles().contains(role);
    }

    public boolean IsCurrentLeader(Long projectId){
        User currentUser = GetCurrentUser();

        return hasRole(projectId, currentUser.getId(), Role.LEADER);
    }

    public boolean IsCurrentManager(Long projectId){
        User currentUser = GetCurrentUser();

        return hasRole(projectId, currentUser.getId(), Role.MANAGER);
    }

    public boolean IsCurrentExecutor(Long projectId){
        User currentUser = GetCurrentUser();

        return hasRole(projectId, currentUser.getId(), Role.EXECUTOR);
    }

    public boolean IsCurrentTester(Long projectId){
        User currentUser = GetCurrentUser();

        return hasRole(projectId, currentUser.getId(), Role.TESTER);
    }

    public List<Task> GetPersonalTasksByProject(Long projectId){
        User currentUser = GetCurrentUser();

        if (!hasRole(projectId, currentUser.getId(), Role.EXECUTOR)){
            throw new AccessDeniedException("Только у исполнителя есть личные задачи");
        }


        return taskRepository.findByAssigneeIdAndStatus(currentUser.getId(), Status.InProgress);
    }

    public List<Task> GetReviewTasksWithoutEvaluation(Long projectId){
        if (!IsCurrentTester(projectId)){
            throw new AccessDeniedException("Только тестировщик может оценивать задачи");
        }

        User curUser = GetCurrentUser();

        List<Task> reviewTasks = taskRepository.findByProjectIdAndStatus(projectId, Status.Review)
            .stream().filter(t -> t.getEvaluations().stream()
                .noneMatch(eval -> eval.getReviewer().getId().equals(curUser.getId())
                    && eval.getReviewRound() == t.getCurrentReviewRound()))
            .collect(Collectors.toList());
        
        return reviewTasks;
    }

    public List<Membership> GetMembers(Long projectId){
        return membershipRepository.findAllByProjectId(projectId);
    }
}
