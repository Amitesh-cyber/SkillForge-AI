package com.studyrec.repository;

import com.studyrec.model.ProjectAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ProjectAnalysisRepository extends JpaRepository<ProjectAnalysis, Long> {
    Optional<ProjectAnalysis> findByInterviewSessionId(Long sessionId);
}
