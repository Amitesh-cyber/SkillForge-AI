package com.studyrec.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "interview_sessions")
@Data
@NoArgsConstructor
public class InterviewSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    private User user;

    @Column(nullable = false)
    private String mode; // PROJECT, DSA, CODING, APTITUDE, HR, SYSTEM_DESIGN, DEBUGGING

    @Column(nullable = false)
    private String company; // Google, Microsoft, Amazon, Startup

    @Column(nullable = false)
    private String status; // IN_PROGRESS, COMPLETED

    @Column(nullable = false)
    private String title;

    @Column(name = "current_question_index")
    private Integer currentQuestionIndex = 0;

    @Column(columnDefinition = "TEXT")
    private String transcript; // JSON string of conversation history

    private Double score;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (currentQuestionIndex == null) currentQuestionIndex = 0;
        if (status == null) status = "IN_PROGRESS";
    }
}
