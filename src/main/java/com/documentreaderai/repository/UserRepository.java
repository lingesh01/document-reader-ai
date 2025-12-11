//package com.documentreaderai.repository;
//
// 
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.stereotype.Repository;
//
//import com.documentreaderai.model.entity.Document;
//
//import java.util.List;
//import java.util.Optional;
//import java.util.UUID;
//
//public interface DocumentRepository extends JpaRepository<Document, UUID> {
//    
//    List<Document> findByStatus(Document.DocumentStatus status);
//}