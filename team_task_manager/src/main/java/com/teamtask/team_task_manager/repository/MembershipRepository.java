package com.teamtask.team_task_manager.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.teamtask.team_task_manager.model.Membership;

public interface MembershipRepository extends JpaRepository<Membership, Long> {
    Optional<Membership> findByProjectIdAndUserId(Long projectId, Long userId);
    List<Membership> findAllByUserUsername(String username);
    List<Membership> findAllByProjectId(Long id);
    void deleteByProjectId(Long projectId);
}
