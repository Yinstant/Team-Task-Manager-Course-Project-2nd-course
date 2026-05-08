package com.teamtask.team_task_manager.controller;

import com.teamtask.team_task_manager.repository.GoalRepository;
import com.teamtask.team_task_manager.repository.ProjectRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.teamtask.team_task_manager.model.Task;
import com.teamtask.team_task_manager.repository.TaskRepository;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/tasks")
public class TaskController {
    private final GoalRepository goalRepository;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;

    public TaskController(TaskRepository taskRepository, ProjectRepository projectRepository, GoalRepository goalRepository){
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.goalRepository = goalRepository;
    }

    // Показать форму добавления задачи
    @GetMapping("/new")
    public String ShowAddForm(@RequestParam Long projectId,
        @RequestParam(required = false) Long goalId,
        Model model){
        Task task = new Task();
        task.setProject(projectRepository.getReferenceById(projectId));
        if (goalId != null){
            task.setGoal(goalRepository.getReferenceById(goalId));
        }

        model.addAttribute("task", task);
        model.addAttribute("goals", goalRepository.findByProjectId(projectId));
        model.addAttribute("projectId", projectId);

        return "task-form";
    }

    // Сохранение добавленной задачи
    @PostMapping
    public String AddTask(@Valid @ModelAttribute Task task,
            BindingResult result,
            @RequestParam Long projectId,
            @RequestParam(required = false) Long goalId,
            RedirectAttributes redirectAttributes) {
        if (result.hasErrors())
            return "task-form";

        task.setProject(projectRepository.getReferenceById(projectId));
        if (goalId != null){
            task.setGoal(goalRepository.getReferenceById(goalId));
        }

        taskRepository.save(task);
        redirectAttributes.addFlashAttribute("message", "Задача успешно добавлена!");       

        return "redirect:/projects/" + projectId;
    }

    // Показать форму редактирования задачи
    @GetMapping("/{id}/edit")
    public String ShowEditForm(@PathVariable Long id, 
        @RequestParam Long projectId,
        Model model){
        Task task = taskRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Invalid task id: " + id));
        
        model.addAttribute("task", task);
        model.addAttribute("projectId", projectId);
        model.addAttribute("goals", goalRepository.findByProjectId(projectId));
        return "task-form";
    }

    // Обновление задачи
    @PostMapping("/{id}")
    public String UpdateTask(@PathVariable Long id,
            @Valid @ModelAttribute Task task,
            BindingResult result,
            @RequestParam Long projectId,
            @RequestParam(required = false) Long goalId,
            RedirectAttributes redirectAttributes) {
        if (result.hasErrors())
            return "task-form";
        
        task.setId(id);
        task.setProject(projectRepository.getReferenceById(projectId));
        if (goalId != null){
            task.setGoal(goalRepository.getReferenceById(goalId));
        }

        taskRepository.save(task);
        redirectAttributes.addFlashAttribute("message", "Задача успешно обновлена!");
        
        return "redirect:/projects/" + projectId;
    }            

    // Удаление задачи
    @GetMapping("/{id}/delete")
    public String DeleteTask(
            @PathVariable Long id,
            @RequestParam Long projectId,
            RedirectAttributes redirectAttributes){
        taskRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("message", "Задача успешно удалена!");
        return "redirect:/projects/" + projectId;
    }
}
