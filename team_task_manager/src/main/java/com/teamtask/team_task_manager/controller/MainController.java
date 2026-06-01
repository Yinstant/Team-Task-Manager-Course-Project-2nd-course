package com.teamtask.team_task_manager.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.teamtask.team_task_manager.service.ProjectService;


@Controller
public class MainController {
    private final ProjectService projectService;

    public MainController(ProjectService projectService){
        this.projectService = projectService;
    }

    // Редирект с главной страницы
    @GetMapping({"/", ""})
    public String mainPageRedirect() {
        return "redirect:/projects";
    }

    // Отображение списка проектов на странице канбан-доски
    @GetMapping({"/kanban/", "/kanban"})
    public String ListProjectsKanban(Model model){
        model.addAttribute("projects", projectService.GetUserProjects());
        model.addAttribute("isKanban", true);
        return "kanban";
    }
}
