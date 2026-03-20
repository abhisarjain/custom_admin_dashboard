package com.customadmindashboard.auth.repository;

import com.customadmindashboard.auth.entity.Invitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface InvitationRepository extends JpaRepository<Invitation, Long> {
    Optional<Invitation> findByToken(String token);
    List<Invitation> findAllByProjectId(Long projectId);
    Optional<Invitation> findByEmailAndProjectId(String email, Long projectId);
    List<Invitation> findAllByEmailAndProjectId(String email, Long projectId);
    List<Invitation> findAllByEmail(String email);
    void deleteAllByRoleId(Long roleId);
    void deleteAllByProjectId(Long projectId);
}
