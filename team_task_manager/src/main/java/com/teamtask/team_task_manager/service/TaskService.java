package com.teamtask.team_task_manager.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.teamtask.team_task_manager.model.Goal;
import com.teamtask.team_task_manager.model.Project;
import com.teamtask.team_task_manager.model.Status;
import com.teamtask.team_task_manager.model.Task;
import com.teamtask.team_task_manager.model.User;
import com.teamtask.team_task_manager.repository.GoalRepository;
import com.teamtask.team_task_manager.repository.ProjectRepository;
import com.teamtask.team_task_manager.repository.TaskRepository;
import com.teamtask.team_task_manager.repository.UserRepository;

@Service
public class TaskService {
    @Autowired ProjectRepository projectRepository;
    @Autowired UserRepository userRepository;
    @Autowired GoalRepository goalRepository;
    @Autowired TaskRepository taskRepository;

}
