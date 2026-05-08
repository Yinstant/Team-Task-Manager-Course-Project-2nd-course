package com.teamtask.team_task_manager.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.teamtask.team_task_manager.model.Goal;
import com.teamtask.team_task_manager.model.Project;
import com.teamtask.team_task_manager.model.Task;
import com.teamtask.team_task_manager.repository.GoalRepository;
import com.teamtask.team_task_manager.repository.ProjectRepository;
import com.teamtask.team_task_manager.repository.TaskRepository;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/projects")
public class ProjectController {
    private final ProjectRepository projectRepository;
    private final GoalRepository goalRepository;
    private final TaskRepository taskRepository;

    public ProjectController(ProjectRepository projectRepository,
        GoalRepository goalRepository,
        TaskRepository taskRepository
    ){
        this.projectRepository = projectRepository;
        this.goalRepository = goalRepository;
        this.taskRepository = taskRepository;
    }

    // Отображение списка проектов
    @GetMapping({"/", ""})
    public String ListProjects(Model model){
        model.addAttribute("projects", projectRepository.findAll());
        return "task-list";
    }

    // Просмотр проекта
    @GetMapping("/{id}")
    public String ShowProject(@PathVariable Long id, Model model){
        Project project = projectRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Invalid project id" + id));
        model.addAttribute("currentProject", project);
        model.addAttribute("projects", projectRepository.findAll());
        
        List<Goal> goals = goalRepository.findByProjectId(id);
        Map<Goal, List<Task>> tasksByGoal = new LinkedHashMap<>();

        for (Goal goal : goals){
            tasksByGoal.put(goal, taskRepository.findByGoalId(goal.getId()));
        }

        model.addAttribute("tasksByGoal", tasksByGoal);
        model.addAttribute("tasksWithoutGoal", taskRepository.findByProjectIdAndGoalIsNull(id));
        
        return "task-list";
    }

    // Показать форму добавления проекта
    @GetMapping("/new")
    public String ShowAddForm(Model model){
        model.addAttribute("project", new Project());
        return "project-form";
    }

    // Сохранение добавленного проекта
    @PostMapping
    public String AddProject(@Valid @ModelAttribute Project project,
            BindingResult result,
            RedirectAttributes redirectAttributes) {
        if (result.hasErrors())
            return "project-form";
        Project saved = projectRepository.save(project);
        redirectAttributes.addFlashAttribute("message", "Проект успешно добавлен!");        
        return "redirect:/projects/" + saved.getId();
    }

    // Показать форму редактирования проекта
    @GetMapping("/{id}/edit")
    public String ShowEditForm(@PathVariable Long id, Model model){
        Project project = projectRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Invalid project id: " + id));
        model.addAttribute("project", project);
        return "project-form";
    }

    // Обновление проекта
    @PostMapping("/{id}")
    public String UpdateProject(@PathVariable Long id,
            @Valid @ModelAttribute Project project,
            BindingResult result,
            RedirectAttributes redirectAttributes) {
        if (result.hasErrors())
            return "project-form";
        project.setId(id);
        Project saved = projectRepository.save(project);
        redirectAttributes.addFlashAttribute("message", "Проект успешно обновлен!");
        return "redirect:/projects/" + saved.getId();
    }            

    // Удаление проекта
    @GetMapping("/{id}/delete")
    public String DeleteProject(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes){
        projectRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("message", "Проект успешно удален!");
        return "redirect:/projects";
    }
}
