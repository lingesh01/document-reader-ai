//package com.documentreaderai.model.entity;
//
//
//import jakarta.persistence.*;
//import lombok.AllArgsConstructor;
//import lombok.Builder;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//import org.hibernate.annotations.CreationTimestamp;
//import org.hibernate.annotations.UpdateTimestamp;
//
//import java.time.LocalDateTime;
//import java.util.UUID;
//
//@Entity
//@Table(name = "users")
//@Data
//@NoArgsConstructor
//@AllArgsConstructor
//@Builder
//public class User {
//    
//    @Id
//    @GeneratedValue(strategy = GenerationType.UUID)
//    private UUID id;
//    
//    @Column(unique = true, nullable = false)
//    private String email;
//    
//    @Column(name = "first_name")
//    private String firstName;
//    
//    @Column(name = "last_name")
//    private String lastName;
//    
//    @Column(length = 50)
//    private String role; // ADMIN, USER
//    
//    @Column(name = "is_active")
//    private Boolean isActive = true;
//    
//    @CreationTimestamp
//    @Column(name = "created_at", updatable = false)
//    private LocalDateTime createdAt;
//    
//    @UpdateTimestamp
//    @Column(name = "updated_at")
//    private LocalDateTime updatedAt;
//}
