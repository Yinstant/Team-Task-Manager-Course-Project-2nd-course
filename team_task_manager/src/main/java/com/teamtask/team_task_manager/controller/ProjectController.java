package com.teamtask.team_task_manager.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.teamtask.team_task_manager.model.Goal;
import com.teamtask.team_task_manager.model.Project;
import com.teamtask.team_task_manager.model.Task;
import com.teamtask.team_task_manager.model.User;
import com.teamtask.team_task_manager.repository.GoalRepository;
import com.teamtask.team_task_manager.repository.ProjectRepository;
import com.teamtask.team_task_manager.repository.TaskRepository;
import com.teamtask.team_task_manager.repository.UserRepository;
import com.teamtask.team_task_manager.service.ProjectService;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestParam;


@Controller
@RequestMapping("/projects")
public class ProjectController {
    private final ProjectRepository projectRepository;
    private final GoalRepository goalRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    @Autowired ProjectService projectService;

    public ProjectController(ProjectRepository projectRepository,
        GoalRepository goalRepository,
        TaskRepository taskRepository,
        UserRepository userRepository
    ){
        this.projectRepository = projectRepository;
        this.goalRepository = goalRepository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    // Отображение списка проектов
    @GetMapping({"/", ""})
    public String ListProjects(Model model){
        model.addAttribute("projects", projectService.GetUserProjects());
        model.addAttribute("isKanban", false);
        return "task-list";
    }

    // Просмотр проекта деревом целей
    @GetMapping("/{id}")
    public String ShowProject(@PathVariable Long id, Model model){
        Project project = projectService.GetProjectIfAccesible(id);
            
        model.addAttribute("currentProject", project);
        model.addAttribute("projects", projectService.GetUserProjects());

        model.addAttribute("tasksByGoal", projectService.GetTasksByGoal(id));
        model.addAttribute("tasksWithoutGoal", taskRepository.findByProjectIdAndGoalIsNull(id));

        model.addAttribute("isKanban", false);
        
        return "task-list";
    }

    // Просмотр проекта канбан-доской
    @GetMapping("/{id}/kanban")
    public String ShowProjectKanban(@PathVariable Long id, Model model){
        Project project = projectService.GetProjectIfAccesible(id);
            
        model.addAttribute("currentProject", project);
        model.addAttribute("projects", projectService.GetUserProjects());

        model.addAttribute("tasksByStatus", projectService.GetTasksByStatus(id));
        
        model.addAttribute("isKanban", true);

        return "kanban";
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

        User currentUser = projectService.GetCurrentUser();
        project.setOwner(currentUser);
        project.getMembers().add(currentUser);

        Project saved = projectRepository.save(project);
        redirectAttributes.addFlashAttribute("message", "Проект успешно добавлен!");        
        return "redirect:/projects/" + saved.getId();
    }

    // Показать форму редактирования проекта
    @GetMapping("/{id}/edit")
    public String ShowEditForm(@PathVariable Long id, Model model){
        if (!projectService.HasAccess(id)){
            throw new AccessDeniedException("У вас нет доступа к этому проекту");
        }

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
        if (!projectService.HasAccess(id)){
            throw new AccessDeniedException("У вас нет доступа к этому проекту");
        }

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
        if (!projectService.HasAccess(id)){
            throw new AccessDeniedException("У вас нет доступа к этому проекту");
        }
            
        projectRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("message", "Проект успешно удален!");
        return "redirect:/projects";
    }

    // Приглашение пользователя
    @GetMapping("/{id}/invite")
    public String ShowInviteForm(@PathVariable Long id, Model model){
        if (!projectService.HasAccess(id)){
            throw new AccessDeniedException("У вас нет доступа к этому проекту");
        }

        Project project = projectRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Invalid project id: " + id));
        model.addAttribute("project", project);

        return "invite-form";
    }

    @PostMapping("/{id}/invite")
    public String InviteUser(@PathVariable Long id,
            @RequestParam String username,
            RedirectAttributes redirectAttributes) {
        if (!projectService.HasAccess(id)){
            throw new AccessDeniedException("У вас нет доступа к этому проекту");
        }

        Project currentProject = projectRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Project not found"));
            
        User currentUser = projectService.GetCurrentUser();

        if (!currentProject.getOwner().getId().equals(currentUser.getId())){
            redirectAttributes.addFlashAttribute("error", "Только владелец проекта может добавлять участников");
            return "redirect:/projects/" + id;
        }

        User newMember = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));

        if (currentProject.getMembers().contains(newMember)){
            redirectAttributes.addFlashAttribute("error", "Участник уже есть в проекте");
            return "redirect:/projects/" + id;
        }

        currentProject.getMembers().add(newMember);
        projectRepository.save(currentProject);

        redirectAttributes.addFlashAttribute("message", "Пользователь" + newMember.getUsername() + "добавлен в проект!");
        return "redirect:/projects/" + id;
    }
}
