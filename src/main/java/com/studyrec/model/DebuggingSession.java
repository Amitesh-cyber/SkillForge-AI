package com.studyrec.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "debugging_sessions")
@Data
@NoArgsConstructor
public class DebuggingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_session_id", nullable = false)
    private InterviewSession interviewSession;

    @Column(name = "bug_type", nullable = false)
    private String bugType;

    @Column(name = "bug_description", columnDefinition = "TEXT")
    private String bugDescription;

    @Column(name = "buggy_code", columnDefinition = "TEXT")
    private String buggyCode;

    @Column(name = "user_explanation", columnDefinition = "TEXT")
    private String userExplanation;

    @Column(name = "user_fix_code", columnDefinition = "TEXT")
    private String userFixCode;

    private Boolean solved = false;

    @Column(name = "ai_feedback", columnDefinition = "TEXT")
    private String aiFeedback;
}
