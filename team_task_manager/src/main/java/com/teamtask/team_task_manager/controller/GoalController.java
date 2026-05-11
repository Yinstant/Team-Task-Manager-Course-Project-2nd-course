package com.teamtask.team_task_manager.controller;

import com.teamtask.team_task_manager.repository.ProjectRepository;
import com.teamtask.team_task_manager.service.ProjectService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
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

import com.teamtask.team_task_manager.model.Goal;
import com.teamtask.team_task_manager.repository.GoalRepository;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/goals")
public class GoalController {
    private final ProjectRepository projectRepository;
    private final GoalRepository goalRepository;

    @Autowired ProjectService projectService;

    public GoalController(GoalRepository goalRepository, ProjectRepository projectRepository){
        this.goalRepository = goalRepository;
        this.projectRepository = projectRepository;
    }

    // Показать форму добавления цели
    @GetMapping("/new")
    public String ShowAddForm(@RequestParam Long projectId,
        Model model){
        if (!projectService.HasAccess(projectId)){
            throw new AccessDeniedException("У вас нет доступа к этому проекту");
        }

        Goal goal = new Goal();
        goal.setProject(projectRepository.getReferenceById(projectId));
    
        model.addAttribute("goal", goal);
        model.addAttribute("projectId", projectId);
        return "goal-form";
    }

    // Сохранение добавленной цели
    @PostMapping
    public String AddGoal(@Valid @ModelAttribute Goal goal,
            BindingResult result,
            @RequestParam Long projectId,
            RedirectAttributes redirectAttributes) {
        if (!projectService.HasAccess(projectId)){
            throw new AccessDeniedException("У вас нет доступа к этому проекту");
        }

        if (result.hasErrors())
            return "goal-form";

        goal.setProject(projectRepository.getReferenceById(projectId));
        goalRepository.save(goal);
        redirectAttributes.addFlashAttribute("message", "Цель успешно добавлена!");        
        
        return "redirect:/projects/" + projectId;
    }

    // Показать форму редактирования цели
    @GetMapping("/{id}/edit")
    public String ShowEditForm(@PathVariable Long id, 
        @RequestParam Long projectId,
        Model model){
        if (!projectService.HasAccess(projectId)){
            throw new AccessDeniedException("У вас нет доступа к этому проекту");
        }
        
        Goal goal = goalRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Invalid goal id: " + id));
        
        model.addAttribute("goal", goal);
        model.addAttribute("projectId", projectId);
        
        return "goal-form";
    }

    // Обновление цели
    @PostMapping("/{id}")
    public String UpdateGoal(@PathVariable Long id,
            @Valid @ModelAttribute Goal goal,
            BindingResult result,
            @RequestParam Long projectId,
            RedirectAttributes redirectAttributes) {
        if (!projectService.HasAccess(projectId)){
            throw new AccessDeniedException("У вас нет доступа к этому проекту");
        }

        if (result.hasErrors())
            return "goal-form";
        
        goal.setId(id);
        goal.setProject(projectRepository.getReferenceById(projectId));
        
        goalRepository.save(goal);
        redirectAttributes.addFlashAttribute("message", "Цель успешно обновлена!");
        
        return "redirect:/projects/" + projectId;
    }            

    // Удаление цели
    @GetMapping("/{id}/delete")
    public String DeleteGoal(
            @PathVariable Long id,
            @RequestParam Long projectId,
            RedirectAttributes redirectAttributes){
        if (!projectService.HasAccess(projectId)){
            throw new AccessDeniedException("У вас нет доступа к этому проекту");
        }
        
        goalRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("message", "Цель успешно удалена!");
        return "redirect:/projects/" + projectId;
    }
}
