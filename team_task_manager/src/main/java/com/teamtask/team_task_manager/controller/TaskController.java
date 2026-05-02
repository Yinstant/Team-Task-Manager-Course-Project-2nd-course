package com.teamtask.team_task_manager.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.teamtask.team_task_manager.model.Task;
import com.teamtask.team_task_manager.repository.TaskRepository;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/tasks")
public class TaskController {
    private final TaskRepository taskRepository;

    public TaskController(TaskRepository taskRepository){
        this.taskRepository = taskRepository;
    }

    // Отображение списка задач
    @GetMapping
    public String ListTasks(Model model){
        model.addAttribute("tasks", taskRepository.findAll());
        return "task-list";
    }

    // Показать форму добавления задачи
    @GetMapping("/add")
    public String ShowAddForm(Model model){
        model.addAttribute("task", new Task());
        return "task-form";
    }

    // Сохранение добавленной задачи
    @PostMapping
    public String AddTask(@Valid @ModelAttribute Task task,
            BindingResult result,
            RedirectAttributes redirectAttributes) {
        if (result.hasErrors())
            return "task-form";
        taskRepository.save(task);
        redirectAttributes.addFlashAttribute("message", "Задача успешно добавлена!");        
        return "redirect:/tasks";
    }

    // Показать форму редактирования задачи
    @GetMapping("/edit/{id}")
    public String ShowEditForm(@PathVariable Long id, Model model){
        Task task = taskRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Invalid task id: " + id));
        model.addAttribute("task", task);
        return "task-form";
    }

    // Обновление задачи
    @PostMapping("/update/{id}")
    public String UpdateTask(@PathVariable Long id,
            @Valid @ModelAttribute Task task,
            BindingResult result,
            RedirectAttributes redirectAttributes) {
        if (result.hasErrors())
            return "task-form";
        task.setId(id);
        taskRepository.save(task);
        redirectAttributes.addFlashAttribute("message", "Задача успешно обновлена!");
        return "redirect:/tasks";
    }            

    // Удаление задачи
    @GetMapping("/delete/{id}")
    public String DeleteTask(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes){
        taskRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("message", "Задача успешно удалена!");
        return "redirect:/tasks";
    }
}
