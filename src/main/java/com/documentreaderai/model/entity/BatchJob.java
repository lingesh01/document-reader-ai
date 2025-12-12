package com.documentreaderai.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "batch_jobs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchJob {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false)
    private String jobName;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BatchStatus status;
    
    @Column(name = "analysis_template", columnDefinition = "TEXT")
    private String analysisTemplate;
    
    @Column(name = "total_documents")
    private Integer totalDocuments;
    
    @Column(name = "processed_count")
    @Builder.Default
    private Integer processedCount = 0;
    
    @Column(name = "success_count")
    @Builder.Default
    private Integer successCount = 0;
    
    @Column(name = "failure_count")
    @Builder.Default
    private Integer failureCount = 0;
    
    @Column(name = "started_at")
    private LocalDateTime startedAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Relationship with documents
    @OneToMany(mappedBy = "batchJob", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Document> documents = new ArrayList<>();
    
    public enum BatchStatus {
        PENDING,     // Created, not started
        QUEUED,      // In queue
        RUNNING,     // Processing
        PAUSED,      // Temporarily paused
        COMPLETED,   // All done
        FAILED,      // Critical failure
        CANCELLED    // User cancelled
    }
    
    // Helper methods
    public double getProgressPercentage() {
        if (totalDocuments == null || totalDocuments == 0) return 0.0;
        return (processedCount * 100.0) / totalDocuments;
    }
    
    public String getProgressText() {
        return String.format("%d/%d (%.1f%%)", 
            processedCount, totalDocuments, getProgressPercentage());
    }
    
    public Long getEstimatedTimeRemaining() {
        if (startedAt == null || processedCount == 0) return null;
        
        long elapsed = java.time.Duration.between(startedAt, LocalDateTime.now()).toSeconds();
        double avgTimePerDoc = (double) elapsed / processedCount;
        int remaining = totalDocuments - processedCount;
        
        return (long) (avgTimePerDoc * remaining);
    }
}