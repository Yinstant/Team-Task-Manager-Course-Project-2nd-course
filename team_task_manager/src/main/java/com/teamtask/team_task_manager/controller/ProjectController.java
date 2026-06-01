package com.teamtask.team_task_manager.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
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
import com.teamtask.team_task_manager.model.Priority;
import com.teamtask.team_task_manager.model.Project;
import com.teamtask.team_task_manager.model.Role;
import com.teamtask.team_task_manager.model.Status;
import com.teamtask.team_task_manager.model.Task;
import com.teamtask.team_task_manager.model.User;
import com.teamtask.team_task_manager.repository.EvaluationRepository;
import com.teamtask.team_task_manager.repository.GoalRepository;
import com.teamtask.team_task_manager.repository.MembershipRepository;
import com.teamtask.team_task_manager.repository.ProjectRepository;
import com.teamtask.team_task_manager.repository.TaskRepository;
import com.teamtask.team_task_manager.repository.UserRepository;
import com.teamtask.team_task_manager.service.ProjectService;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;


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
        Project project = projectService.GetProjectIfAccessible(id);
            
        model.addAttribute("currentProject", project);
        model.addAttribute("projects", projectService.GetUserProjects());

        model.addAttribute("tasksByGoal", projectService.GetTasksByGoal(id));
        model.addAttribute("tasksWithoutGoal", taskRepository.findByProjectIdAndGoalIsNull(id));

        List<Membership> members = projectService.GetMembers(id);
        model.addAttribute("members", members);

        model.addAttribute("isKanban", false);
        
        return "task-list";
    }

    // Просмотр проекта канбан-доской
    @GetMapping("/{id}/kanban")
    public String ShowProjectKanban(@PathVariable Long id, Model model){
        Project project = projectService.GetProjectIfAccessible(id);
            
        model.addAttribute("currentProject", project);
        model.addAttribute("projects", projectService.GetUserProjects());

        model.addAttribute("tasksByStatus", projectService.GetTasksByStatus(id));
        
        List<Membership> members = projectService.GetMembers(id);
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

        Project project = projectService.GetProjectIfAccessible(id);
            
        model.addAttribute("currentProject", project);
        model.addAttribute("projects", projectService.GetUserProjects());

        model.addAttribute("myTasks", projectService.GetPersonalTasksByProject(id));
        
        List<Membership> members = projectService.GetMembers(id);
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

        Project project = projectService.GetProjectIfAccessible(id);

        model.addAttribute("currentProject", project);
        model.addAttribute("projects", projectService.GetUserProjects());

        model.addAttribute("reviewTasks", projectService.GetReviewTasksWithoutEvaluation(id));

        User curUser = projectService.GetCurrentUser();
        model.addAttribute("myEvaluations", evaluationRepository.findByReviewerId(curUser.getId()));
        
        List<Membership> members = projectService.GetMembers(id);
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
    @Transactional
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
    @Transactional
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
    @Transactional
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

        goalRepository.deleteByProjectId(id);
        taskRepository.deleteByProjectId(id);
        membershipRepository.deleteByProjectId(id);
        
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

    @Transactional
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
    @Transactional
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
    @Transactional
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

    // Перейти на страницу отчётов
    @GetMapping("/{id}/reports")
    public String ShowReports(@PathVariable Long id, Model model){
        Project currentProject = projectService.GetProjectIfAccessible(id);
        model.addAttribute("currentProject", currentProject);

        // Статистика по статусам
        List<Object[]> rows = taskRepository.countByProjectIdGroupByStatus(id);
        Map<Status, Long> countByStatus = new HashMap<>();
        for (Object[] row : rows) {
            Status status = (Status) row[0];
            Long count = (Long) row[1];
            countByStatus.put(status, count);
        }
        model.addAttribute("countByStatus", countByStatus);

        // Общее количество задач
        long totalTasks = taskRepository.countByProjectId(id);
        model.addAttribute("totalTasks", totalTasks);

        // Процент выполнения
        long completed = countByStatus.getOrDefault(Status.Completed, 0L);
        model.addAttribute("percentComplete", totalTasks == 0 ? 0 : (completed * 100 / totalTasks));
        
        model.addAttribute("projects", projectService.GetUserProjects());
        
        List<Membership> members = projectService.GetMembers(id);
        model.addAttribute("members", members);
        model.addAttribute("isKanban", false);

        return "reports";
    }

    @GetMapping("/{id}/export")
    public void exportCsv(@PathVariable Long id, HttpServletResponse response) throws IOException {
        Project project = projectService.GetProjectIfAccessible(id);
        List<Task> tasks = taskRepository.findByProjectId(id);
        
        // 1. Устанавливаем тип контента и кодировку
        response.setContentType("text/csv; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=tasks_" + project.getName() + ".csv");
        
        // 2. Добавляем BOM (Byte Order Mark) для Excel
        OutputStream out = response.getOutputStream();
        out.write(0xEF); // BOM
        out.write(0xBB);
        out.write(0xBF);
        
        // 3. Создаём PrintWriter с UTF-8
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
        
        // 4. Заголовки (на русском, но могут быть и английские, разницы нет, главное кодировка)
        writer.println("\"ID\",\"Название\",\"Описание\",\"Дедлайн\",\"Статус\",\"StoryPoints\",\"Приоритет\",\"Время создания\",\"Время обновления\",\"Цель\",\"Ответственный\"");
        
        // 5. Данные
        for (Task t : tasks) {
            writer.printf("%d,\"%s\",\"%s\",%s,%s,%d,%s,%s,%s,\"%s\",\"%s\"\n",
                    t.getId(),
                    escapeCsv(t.getName()),
                    escapeCsv(t.getDescription() != null ? t.getDescription() : ""),
                    t.getDeadline(),
                    t.getStatus(),
                    t.getStoryPoints(),
                    t.getPriority(),
                    t.getCreationTime(),
                    t.getUpdatedTime(),
                    escapeCsv(t.getGoal() == null ? "" : t.getGoal().getName()),
                    escapeCsv(t.getAssignee() == null ? "" : t.getAssignee().getUsername()));
        }
        writer.flush();
        writer.close();
    }

    private String escapeCsv(String s) {
        if (s == null) return "";
        return s.replace("\"", "\"\"");
    }

    @PostMapping("/{id}/import")
    public String importTasks(@PathVariable Long id,
                            @RequestParam("file") MultipartFile file,
                            RedirectAttributes redirectAttributes) {
        if (!projectService.IsCurrentManager(id)){
            throw new AccessDeniedException("Только менеджер проекта может импортировать задачи в проект");
        }

        Project project = projectService.GetProjectIfAccessible(id);
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line = reader.readLine();
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");

                if (parts.length < 5) {
                    continue;
                }

                String title = trimQuotes(parts[0]);
                String description = trimQuotes(parts[1]);
                LocalDate deadline = LocalDate.parse(trimQuotes(parts[2]));
                Integer storyPoints = Integer.parseInt(trimQuotes(parts[3]));
                String priorityStr = trimQuotes(parts[4]);
                Priority priority = Priority.valueOf(priorityStr);

                Task task = new Task();

                task.setName(title);
                task.setDescription(description);
                task.setDeadline(deadline);
                task.setStatus(Status.ToDo);
                task.setStoryPoints(storyPoints);
                task.setPriority(priority);
                task.setProject(project);

                taskRepository.save(task);
            }
            redirectAttributes.addFlashAttribute("message", "Импорт успешно завершён");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка импорта: " + e.getMessage());
        }
        return "redirect:/projects/" + id + "/reports";
    }

    private String trimQuotes(String s) {
        if (s == null) return null;
        s = s.trim();
        if (s.startsWith("\"") && s.endsWith("\"")) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }
}
