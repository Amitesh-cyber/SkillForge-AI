package com.studyrec.repository;

import com.studyrec.model.DebuggingSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface DebuggingSessionRepository extends JpaRepository<DebuggingSession, Long> {
    Optional<DebuggingSession> findByInterviewSessionId(Long sessionId);
}
