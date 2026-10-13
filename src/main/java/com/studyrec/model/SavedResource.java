package com.studyrec.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "saved_resources")
@Data
@NoArgsConstructor
public class SavedResource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Column(name = "video_title")
    private String title;
    
    @Column(name = "video_url")
    private String resourceUrl;
    
    @Column(name = "resource_type")
    private String resourceType; // e.g., VIDEO, PLAYLIST, WEBSITE
    
    @Column(name = "thumbnail_url")
    private String thumbnailUrl;
    
    @Column(name = "channel_name")
    private String channelName;
    
    @Column(name = "topic_name")
    private String topic;

    @Column(name = "playlist_name")
    private String playlistName;

    private String difficulty;

    @Column(name = "saved_at")
    private LocalDateTime savedAt;

    @PrePersist
    protected void onSave() {
        savedAt = LocalDateTime.now();
    }
}
