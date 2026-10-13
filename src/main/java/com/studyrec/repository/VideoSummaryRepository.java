package com.studyrec.repository;

import com.studyrec.model.VideoSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface VideoSummaryRepository extends JpaRepository<VideoSummary, Long> {
    Optional<VideoSummary> findByUserIdAndVideoId(Long userId, String videoId);
}
