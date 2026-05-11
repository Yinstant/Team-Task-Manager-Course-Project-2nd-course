package com.teamtask.team_task_manager.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.teamtask.team_task_manager.model.Role;
import com.teamtask.team_task_manager.repository.RoleRepository;

@Component
public class DataInitializer implements CommandLineRunner{
    @Autowired private RoleRepository roleRepository;

    @Override
    public void run(String... args) throws Exception{
        if (roleRepository.count() == 0){
            roleRepository.save(new Role(null, "ROLE_EXECUTOR"));
        }
    }
}
