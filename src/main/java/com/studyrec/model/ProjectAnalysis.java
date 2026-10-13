package com.studyrec.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "project_analyses")
@Data
@NoArgsConstructor
public class ProjectAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_session_id", nullable = false)
    private InterviewSession interviewSession;

    @Column(name = "project_title", nullable = false)
    private String projectTitle;

    @Column(name = "tech_stack", nullable = false)
    private String techStack;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String features;

    @Column(name = "architecture_explanation", columnDefinition = "TEXT")
    private String architectureExplanation;

    @Column(name = "analysis_report", columnDefinition = "TEXT")
    private String analysisReport;
}
