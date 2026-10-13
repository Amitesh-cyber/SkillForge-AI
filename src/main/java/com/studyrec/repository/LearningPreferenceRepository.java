package com.studyrec.repository;

import com.studyrec.model.LearningPreference;
import com.studyrec.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface LearningPreferenceRepository extends JpaRepository<LearningPreference, Long> {
    Optional<LearningPreference> findByUser(User user);
}
