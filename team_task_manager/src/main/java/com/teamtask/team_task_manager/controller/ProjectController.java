package com.teamtask.team_task_manager.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
import com.teamtask.team_task_manager.model.Membership;
import com.teamtask.team_task_manager.model.Project;
import com.teamtask.team_task_manager.model.Role;
import com.teamtask.team_task_manager.model.Task;
import com.teamtask.team_task_manager.model.User;
import com.teamtask.team_task_manager.repository.EvaluationRepository;
import com.teamtask.team_task_manager.repository.GoalRepository;
import com.teamtask.team_task_manager.repository.MembershipRepository;
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
    private final MembershipRepository membershipRepository;
    private final EvaluationRepository evaluationRepository;

    @Autowired ProjectService projectService;

    public ProjectController(ProjectRepository projectRepository,
        GoalRepository goalRepository,
        TaskRepository taskRepository,
        UserRepository userRepository,
        MembershipRepository membershipRepository,
        EvaluationRepository evaluationRepository
    ){
        this.projectRepository = projectRepository;
        this.goalRepository = goalRepository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.evaluationRepository = evaluationRepository;
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

        List<User> members = membershipRepository.findAllByProjectId(id).stream()
            .map(Membership::getUser).collect(Collectors.toList());
        model.addAttribute("members", members);

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
        
        List<User> members = membershipRepository.findAllByProjectId(id).stream()
            .map(Membership::getUser).collect(Collectors.toList());
        model.addAttribute("members", members);

        model.addAttribute("isKanban", true);

        return "kanban";
    }

    // Просмотр личных задач проекта
    @GetMapping("/{id}/my-tasks")
    public String ShowProjectMyTasks(@PathVariable Long id, Model model){
        if (!projectService.IsCurrentExecutor(id)){
            throw new AccessDeniedException("Только у исполнителя есть личные задачи");
        }

        Project project = projectService.GetProjectIfAccesible(id);
            
        model.addAttribute("currentProject", project);
        model.addAttribute("projects", projectService.GetUserProjects());

        model.addAttribute("myTasks", projectService.GetPersonalTasksByProject(id));
        
        List<User> members = membershipRepository.findAllByProjectId(id).stream()
            .map(Membership::getUser).collect(Collectors.toList());
        model.addAttribute("members", members);

        model.addAttribute("isKanban", false);

        return "my-tasks";
    }

    // Переход на страницу оценки задач проекта
    @GetMapping("/{id}/review")
    public String ShowProjectReview(@PathVariable Long id, Model model){
        if (!projectService.IsCurrentTester(id)){
            throw new AccessDeniedException("Только тестировщик может оценивать задачи");
        }

        Project project = projectService.GetProjectIfAccesible(id);

        model.addAttribute("currentProject", project);
        model.addAttribute("projects", projectService.GetUserProjects());

        model.addAttribute("reviewTasks", projectService.GetReviewTasksWithoutEvaluation(id));

        User curUser = projectService.GetCurrentUser();
        model.addAttribute("myEvaluations", evaluationRepository.findByReviewerId(curUser.getId()));
        
        List<User> members = membershipRepository.findAllByProjectId(id).stream()
            .map(Membership::getUser).collect(Collectors.toList());
        model.addAttribute("members", members);

        model.addAttribute("isKanban", false);

        return "review";
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

        Set<Role> roles = new HashSet<>(Arrays.asList(Role.values()));

        // Тут мы добавляем лидера в члены проекта
        Membership membership = new Membership(null, 
            project, currentUser, roles);

        project.getMembers().add(membership);

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

        if (!projectService.IsCurrentLeader(id)){
            throw new AccessDeniedException("Только лидер проекта может изменять проект");
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

        if (!projectService.IsCurrentLeader(id)){
            throw new AccessDeniedException("Только лидер проекта может изменять проект");
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
            
        if (!projectService.IsCurrentLeader(id)){
            throw new AccessDeniedException("Только лидер проекта может удалять проект");
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

        if (!projectService.IsCurrentLeader(id) && !projectService.IsCurrentManager(id)){
            throw new AccessDeniedException("Только лидер или менеджер проекта может приглашать в проект");
        }

        Project project = projectRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Invalid project id: " + id));
        model.addAttribute("project", project);

        Role[] availableRoles = {Role.EXECUTOR, Role.MANAGER, Role.TESTER};
        model.addAttribute("availableRoles", availableRoles);

        return "invite-form";
    }

    @PostMapping("/{id}/invite")
    public String InviteUser(@PathVariable Long id,
            @RequestParam String username,
            @RequestParam(required = false) List<String> checkedRoles,
            RedirectAttributes redirectAttributes) {
        if (!projectService.HasAccess(id)){
            throw new AccessDeniedException("У вас нет доступа к этому проекту");
        }

        if (!projectService.IsCurrentLeader(id) && !projectService.IsCurrentManager(id)){
            redirectAttributes.addFlashAttribute("error", "Только лидер или менеджер проекта может приглашать участников в проект");
            return "redirect:/projects/" + id;
        }

        Project currentProject = projectRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Project not found"));

        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));
            
        if (membershipRepository.findByProjectIdAndUserId(id, user.getId()).isPresent()){
            redirectAttributes.addFlashAttribute("error", "Участник уже есть в проекте");
            return "redirect:/projects/" + id;
        }

        Set<Role> roles = new HashSet<>();
        if (checkedRoles != null){
            roles = checkedRoles.stream()
            .map(Role::valueOf)
            .collect(Collectors.toSet());
        }
            
        Membership newMember = new Membership(null, currentProject, user, roles);

        membershipRepository.save(newMember);
        // currentProject.getMembers().add(newMember);
        // projectRepository.save(currentProject);

        redirectAttributes.addFlashAttribute("message", "Пользователь " + newMember.getUser().getUsername() + " добавлен в проект!");
        return "redirect:/projects/" + id;
    }

    // Исключение участника
    @GetMapping("/{id}/exclude")
    public String ExcludeMember(
            @PathVariable Long id,
            @RequestParam String username,
            RedirectAttributes redirectAttributes){
        if (!projectService.HasAccess(id)){
            throw new AccessDeniedException("У вас нет доступа к этому проекту");
        }
        
        if (!projectService.IsCurrentLeader(id) && !projectService.IsCurrentManager(id)){
            throw new AccessDeniedException("Только лидер или менеджер проекта может исключать участника из проекта");
        }

        User excludeUser = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        Membership membership = membershipRepository.findByProjectIdAndUserId(id, excludeUser.getId())
            .orElseThrow(() -> new RuntimeException("Membership not found"));
        
        if (membership.getRoles().contains(Role.LEADER)){
            throw new AccessDeniedException("Лидер проекта не может исключить себя");
        }

        Project currentProject = projectRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Project not found"));
        
        currentProject.getMembers().remove(membership);
        membershipRepository.deleteById(membership.getId());
        return "redirect:/projects/" + id;
    }

    // Показать форму изменения ролей
    @GetMapping("/{id}/editRoles")
    public String ShowEditRolesForm(@PathVariable Long id, 
            @RequestParam String username,
            Model model){
        if (!projectService.HasAccess(id)){
            throw new AccessDeniedException("У вас нет доступа к этому проекту");
        }

        if (!projectService.IsCurrentLeader(id)){
            throw new AccessDeniedException("Только лидер проекта может изменять роли участников проекта");
        }

        User editUser = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        Membership membership = membershipRepository.findByProjectIdAndUserId(id, editUser.getId())
            .orElseThrow(() -> new RuntimeException("Membership not found"));
        
        if (membership.getRoles().contains(Role.LEADER)){
            throw new AccessDeniedException("Лидер проекта не может изменить свои роли");
        }

        Project project = projectRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Invalid project id: " + id));
        model.addAttribute("project", project);

        Role[] availableRoles = {Role.EXECUTOR, Role.MANAGER, Role.TESTER};

        model.addAttribute("availableRoles", availableRoles);
        model.addAttribute("username", username);
        model.addAttribute("checkedRoles", membership.getRoles());

        return "invite-form";
    }

    // Обновление ролей участника проекта
    @PostMapping("/{id}/editRoles")
    public String UpdateRoles(@PathVariable Long id,
            @RequestParam String username,
            @RequestParam(required = false) List<String> checkedRoles,
            RedirectAttributes redirectAttributes) {
        if (!projectService.HasAccess(id)){
            throw new AccessDeniedException("У вас нет доступа к этому проекту");
        }
            
        if (!projectService.IsCurrentLeader(id)){
            redirectAttributes.addFlashAttribute("error", "Только лидер проекта может изменять роли участников проекта");
            return "redirect:/projects/" + id;
        }

        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        Set<Role> roles = new HashSet<>();
        if (checkedRoles != null){
            roles = checkedRoles.stream()
            .map(Role::valueOf)
            .collect(Collectors.toSet());
        }
            
        Membership editMember = membershipRepository.findByProjectIdAndUserId(id, user.getId())
            .orElseThrow(() -> new RuntimeException("Membership not found"));

        editMember.setRoles(roles);

        membershipRepository.save(editMember);

        return "redirect:/projects/" + id;
    }
}
