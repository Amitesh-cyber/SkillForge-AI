package com.studyrec.repository;

import com.studyrec.model.InterviewSession;
import com.studyrec.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface InterviewSessionRepository extends JpaRepository<InterviewSession, Long> {
    List<InterviewSession> findByUser(User user);
    List<InterviewSession> findByUserId(Long userId);
}
