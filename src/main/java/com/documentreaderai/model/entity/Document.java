package com.documentreaderai.model.entity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "documents")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Document {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false)
    private String filename;
    
    @Column(name = "file_path", nullable = false)
    private String filePath;
    
    @Column(name = "file_size")
    private Long fileSize;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private DocumentStatus status;
    
    @Column(name = "total_pages")
    private Integer totalPages;
    
    @Column(name = "extracted_text", columnDefinition = "TEXT")
    private String extractedText;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
   
    
    @Column(columnDefinition = "TEXT")
    private String aiAnalysis;
    
    
    @ManyToOne
    @JoinColumn(name = "batch_job_id")
    @JsonIgnore 
    private BatchJob batchJob;
    
   
    
    public enum DocumentStatus {
        UPLOADED,        // Just uploaded
        PROCESSING,      // Extracting text
        READY,          // Text extracted, ready for analysis
        ANALYZING,      // AI analyzing (NEW!)
        ANALYZED,       // AI analysis complete (NEW!)
        FAILED          // Something went wrong
    }
}
