package com.teamtask.team_task_manager.controller;

import com.teamtask.team_task_manager.repository.EvaluationRepository;
import com.teamtask.team_task_manager.repository.GoalRepository;
import com.teamtask.team_task_manager.repository.MembershipRepository;
import com.teamtask.team_task_manager.repository.ProjectRepository;

import com.teamtask.team_task_manager.repository.UserRepository;
import java.util.List;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.teamtask.team_task_manager.model.Evaluation;
import com.teamtask.team_task_manager.model.Membership;
import com.teamtask.team_task_manager.model.Project;
import com.teamtask.team_task_manager.model.Role;
import com.teamtask.team_task_manager.model.Status;
import com.teamtask.team_task_manager.model.Task;
import com.teamtask.team_task_manager.model.User;
import com.teamtask.team_task_manager.repository.TaskRepository;
import com.teamtask.team_task_manager.service.ProjectService;
import com.teamtask.team_task_manager.service.TaskService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/tasks")
public class TaskController {
    private final UserRepository userRepository;
    private final GoalRepository goalRepository;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final MembershipRepository membershipRepository;
    private final EvaluationRepository evaluationRepository;

    private final ProjectService projectService;
    private final TaskService taskService;

    public TaskController(TaskRepository taskRepository, 
        ProjectRepository projectRepository, 
        GoalRepository goalRepository,
        ProjectService projectService,
        TaskService taskService,
        MembershipRepository membershipRepository, 
        EvaluationRepository evaluationRepository,
        UserRepository userRepository){
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.goalRepository = goalRepository;
        this.projectService = projectService;
        this.taskService = taskService;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.evaluationRepository = evaluationRepository;
    }

    // Показать форму добавления задачи
    @GetMapping("/new")
    public String ShowAddForm(@RequestParam Long projectId,
        @RequestParam(required = false) Long goalId,
        Model model){
        if (!projectService.HasAccess(projectId)){
            throw new AccessDeniedException("У вас нет доступа к этому проекту");
        }

        if (!projectService.IsCurrentManager(projectId)){
            throw new AccessDeniedException("Только менеджер проекта может добавлять задачу");
        }

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
        if (!projectService.HasAccess(projectId)){
            throw new AccessDeniedException("У вас нет доступа к этому проекту");
        }

        if (!projectService.IsCurrentManager(projectId)){
            throw new AccessDeniedException("Только менеджер проекта может добавлять задачу");
        }

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
        if (!projectService.HasAccess(projectId)){
            throw new AccessDeniedException("У вас нет доступа к этому проекту");
        }
        
        if (!projectService.IsCurrentManager(projectId)){
            throw new AccessDeniedException("Только менеджер проекта может редактировать задачу");
        }

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
        if (!projectService.HasAccess(projectId)){
            throw new AccessDeniedException("У вас нет доступа к этому проекту");
        }

        if (!projectService.IsCurrentManager(projectId)){
            throw new AccessDeniedException("Только менеджер проекта может редактировать задачу");
        }

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
        if (!projectService.HasAccess(projectId)){
            throw new AccessDeniedException("У вас нет доступа к этому проекту");
        }
        
        if (!projectService.IsCurrentManager(projectId)){
            throw new AccessDeniedException("Только менеджер проекта может удалять задачу");
        }

        taskRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("message", "Задача успешно удалена!");
        return "redirect:/projects/" + projectId;
    }

    // Изменение статуса задачи
    @PostMapping("/{id}/status")
    public String ChangeStatus(@PathVariable Long id,
        @RequestParam String oldStatus,
        @RequestParam Long projectId,
        RedirectAttributes redirectAttributes
    ){
        if (!projectService.HasAccess(projectId)){
            throw new AccessDeniedException("У вас нет доступа к этому проекту");
        }
        
        Task task = taskRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Invalid task id: " + id));

        switch(oldStatus){
            case "ToDo":
                if (!projectService.IsCurrentManager(projectId)){
                    throw new AccessDeniedException("Только менеджер проекта может отправлять задачу на выполнение");
                }

                redirectAttributes.addAttribute("projectId", projectId);
                return "redirect:/tasks/" + id + "/assign";

                // task.setStatus(Status.InProgress);
                // taskRepository.save(task);
                // break;
                
            case "InProgress":
                if (!projectService.IsCurrentExecutor(projectId)){
                    throw new AccessDeniedException("Только исполнитель проекта может отправлять задачу на проверку");
                }

                task.setStatus(Status.Review);

                int oldReviewRounds = task.getCurrentReviewRound();
                task.setCurrentReviewRound(oldReviewRounds + 1);

                taskRepository.save(task);
                break;

            case "Review":
                if (!projectService.IsCurrentTester(projectId)){
                    throw new AccessDeniedException("Только проверяющий проекта может отправлять задачу на рассмотрение");
                }

                task.setStatus(Status.Checking);
                taskRepository.save(task);
                break;

            case "Checking":
                if (!projectService.IsCurrentManager(projectId)){
                    throw new AccessDeniedException("Только менеджер проекта может отправлять задачу из рассмотрения");
                }

                redirectAttributes.addAttribute("projectId", projectId);
                return "redirect:/tasks/" + id + "/checking";

                // task.setStatus(Status.Completed);
                // taskRepository.save(task);
                // break;

            case "Completed":
                if (!projectService.IsCurrentManager(projectId)){
                    throw new AccessDeniedException("Только менеджер проекта может отправлять задачу из выполненных");
                }

                task.setStatus(Status.ToDo);
                taskRepository.save(task);
                break;
        }
        return "redirect:/projects/" + projectId + "/kanban";
    }

    // Форма распределения задач
    @GetMapping("{id}/assign")
    public String ShowAssignForm(@PathVariable Long id,
        @RequestParam Long projectId, Model model){
        if (!projectService.IsCurrentManager(projectId)){
            throw new AccessDeniedException("Только менеджер проекта может отправлять задачу на выполнение");
        }
        Task task = taskRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Task not found"));
        List<User> executorList = membershipRepository.findAllByProjectId(projectId).stream()
            .filter(memb -> memb.getRoles().contains(Role.EXECUTOR))
            .map(Membership::getUser)
            .collect(Collectors.toList());

        model.addAttribute("executorList", executorList);
        model.addAttribute("task", task);
        
        return "assign-form";
    }

    // Назначить ответственного по задаче
    @PostMapping("{id}/assign")
    public String AssignTask(@PathVariable Long id,
        @RequestParam Long projectId, 
        @RequestParam Long executorId, 
        RedirectAttributes redirectAttributes){
        if (!projectService.IsCurrentManager(projectId)){
            throw new AccessDeniedException("Только менеджер проекта может отправлять задачу на выполнение");
        }

        User executor = userRepository.findById(executorId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Task curTask = taskRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Task not found"));

        curTask.setStatus(Status.InProgress);
        curTask.setAssignee(executor);
        taskRepository.save(curTask);

        redirectAttributes.addFlashAttribute("message", "Задача отправлена на выполнение!");

        return "redirect:/projects/" + projectId + "/kanban";
    }

    // Добавить задаче оценку
    @PostMapping("{id}/review")
    public String AddEvaluation(@PathVariable Long id,
        @RequestParam Long projectId, 
        @RequestParam Integer score,
        @RequestParam String comment,
        RedirectAttributes redirectAttributes){
        if (!projectService.IsCurrentTester(projectId)){
            throw new AccessDeniedException("Только проверяющий может оценивать задачу");
        }

        User reviewer = projectService.GetCurrentUser();
        
        Task curTask = taskRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Task not found"));

        Evaluation newEval = new Evaluation(null, curTask, reviewer, score, comment, null, curTask.getCurrentReviewRound());

        evaluationRepository.save(newEval);

        redirectAttributes.addFlashAttribute("message", "Задача оценена!");

        return "redirect:/projects/" + projectId + "/review";
    }

    // Форма рассмотрения задачи
    @GetMapping("{id}/checking")
    public String ShowCheckingForm(@PathVariable Long id,
        @RequestParam Long projectId, Model model){
        if (!projectService.IsCurrentManager(projectId)){
            throw new AccessDeniedException("Только менеджер проекта может принимать решение по задаче");
        }

        Task task = taskRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Task not found"));
        
        List<Evaluation> evalList = evaluationRepository.findByTaskId(task.getId());

        Status[] statusList = {Status.ToDo, Status.InProgress, Status.Review, Status.Completed};

        model.addAttribute("evalList", evalList);
        model.addAttribute("task", task);
        model.addAttribute("statusList", statusList);
        
        return "checking-form";
    }

    // Принять решение по задаче
    @PostMapping("{id}/checking")
    public String CheckTask(@PathVariable Long id,
        @RequestParam Long projectId, 
        @RequestParam Status status, 
        RedirectAttributes redirectAttributes){
        if (!projectService.IsCurrentManager(projectId)){
            throw new AccessDeniedException("Только менеджер проекта может принимать решение по задаче");
        }

        Task curTask = taskRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Task not found"));

        if (status == Status.Review){
            int oldReviewRounds = curTask.getCurrentReviewRound();
            curTask.setCurrentReviewRound(oldReviewRounds + 1);
        }

        curTask.setStatus(status);
        curTask.getEvaluations();
        taskRepository.save(curTask);

        redirectAttributes.addFlashAttribute("message", "Задача отправлена на выполнение!");

        return "redirect:/projects/" + projectId + "/kanban";
    }
}
