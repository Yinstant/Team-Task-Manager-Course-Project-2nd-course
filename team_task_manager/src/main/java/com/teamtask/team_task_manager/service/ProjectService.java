package com.teamtask.team_task_manager.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.teamtask.team_task_manager.model.Project;
import com.teamtask.team_task_manager.model.User;
import com.teamtask.team_task_manager.repository.ProjectRepository;
import com.teamtask.team_task_manager.repository.UserRepository;

@Service
public class ProjectService {
    @Autowired ProjectRepository projectRepository;
    @Autowired UserRepository userRepository;

    public User GetCurrentUser(){
        String username = SecurityContextHolder
            .getContext()
            .getAuthentication()
            .getName();
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public boolean HasAccess(Long projectId){
        Project currenProject = projectRepository.findById(projectId)
            .orElseThrow(() -> new RuntimeException("Project not found"));
        User currentUser = GetCurrentUser();

        return currenProject.getMembers()
            .contains(currentUser);
    }

    public Project GetProjectIfAccesible(Long projectId){
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
        
        return projectRepository.findAllByMembersUsername(username);
    }
}
