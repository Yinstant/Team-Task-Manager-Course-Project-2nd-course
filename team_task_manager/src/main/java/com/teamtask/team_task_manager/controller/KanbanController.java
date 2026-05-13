package com.teamtask.team_task_manager.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.teamtask.team_task_manager.service.ProjectService;


@Controller
@RequestMapping("/kanban")
public class KanbanController {
    private final ProjectService projectService;

    public KanbanController(ProjectService projectService){
        this.projectService = projectService;
    }

    // Отображение списка проектов
    @GetMapping({"/", ""})
    public String ListProjects(Model model){
        model.addAttribute("projects", projectService.GetUserProjects());
        model.addAttribute("isKanban", true);
        return "kanban";
    }
}
