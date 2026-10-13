package com.studyrec.repository;

import com.studyrec.model.AptitudeResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AptitudeResultRepository extends JpaRepository<AptitudeResult, Long> {
    List<AptitudeResult> findByInterviewSessionId(Long sessionId);
}
