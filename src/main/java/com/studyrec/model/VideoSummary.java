package com.studyrec.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "video_summaries")
@Data
@NoArgsConstructor
public class VideoSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Column(name = "video_id")
    private String videoId;

    @Column(name = "summary_text", columnDefinition = "TEXT")
    private String summaryText;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "practical_examples", columnDefinition = "TEXT")
    private String practicalExamples;
}
