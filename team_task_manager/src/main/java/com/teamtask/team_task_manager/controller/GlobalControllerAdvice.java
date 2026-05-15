package com.teamtask.team_task_manager.controller;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.teamtask.team_task_manager.service.ProjectService;

@ControllerAdvice
public class GlobalControllerAdvice {
    private final ProjectService projectService;

    public GlobalControllerAdvice(ProjectService projectService){
        this.projectService = projectService;
    }

    @ModelAttribute
    public ProjectService projectService(){
        return projectService;
    }
}
