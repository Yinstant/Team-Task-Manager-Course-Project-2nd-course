package com.teamtask.team_task_manager.controller;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;

import com.teamtask.team_task_manager.model.Role;
import com.teamtask.team_task_manager.model.User;
import com.teamtask.team_task_manager.repository.RoleRepository;
import com.teamtask.team_task_manager.repository.UserRepository;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;


@Controller
@RequestMapping("/register")
public class RegisterController {
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private RoleRepository roleRepository;

    @GetMapping({"", "/"})
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }
    
    @PostMapping({"", "/"})
    public String registerUser(@Valid @ModelAttribute User user, BindingResult result) {
        if (result.hasErrors()){
            return "register";
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        Role userRole = roleRepository.findByName("ROLE_EXECUTOR")
            .orElseThrow(() -> new RuntimeException("Role not found"));
        
        user.setRoles(Set.of(userRole));
        userRepository.save(user);

        return "redirect:/login";
    }
}
