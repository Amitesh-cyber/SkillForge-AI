package com.studyrec.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "learning_preferences")
@Data
@NoArgsConstructor
public class LearningPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String preferredLevel; // Beginner, Intermediate, Advanced
    private String preferredTopics; // Comma-separated or JSON
    private String theme; // Light, Dark
}
