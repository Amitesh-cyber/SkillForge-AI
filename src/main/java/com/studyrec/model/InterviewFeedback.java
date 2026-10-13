package com.studyrec.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "interview_feedback")
@Data
@NoArgsConstructor
public class InterviewFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_session_id", nullable = false)
    private InterviewSession interviewSession;

    @Column(name = "technical_score")
    private Double technicalScore;

    @Column(name = "coding_score")
    private Double codingScore;

    @Column(name = "dsa_score")
    private Double dsaScore;

    @Column(name = "communication_score")
    private Double communicationScore;

    @Column(name = "debugging_score")
    private Double debuggingScore;

    @Column(name = "aptitude_score")
    private Double aptitudeScore;

    @Column(name = "confidence_score")
    private Double confidenceScore;

    @Column(columnDefinition = "TEXT")
    private String strengths; // JSON array/string

    @Column(columnDefinition = "TEXT")
    private String weaknesses; // JSON array/string

    @Column(columnDefinition = "TEXT")
    private String suggestions; // JSON array/string

    @Column(columnDefinition = "TEXT")
    private String insights;

    @Column(name = "learning_recommendations", columnDefinition = "TEXT")
    private String learningRecommendations; // JSON array/string
}
