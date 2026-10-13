package com.studyrec.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false)
    private String name;

    @Column(nullable = true)
    private String password;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "profile_image")
    private String profileImage;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<SearchHistory> searchHistory;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<SavedResource> savedResources;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private LearningPreference preferences;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
