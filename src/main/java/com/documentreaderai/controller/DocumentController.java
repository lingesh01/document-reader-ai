package com.documentreaderai.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.documentreaderai.model.entity.Document;
import com.documentreaderai.model.entity.Document.DocumentStatus;
import com.documentreaderai.service.FastDocumentService;  // ← CHANGED: Using Fast service

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    // ✅ CRITICAL FIX: Use FastDocumentService instead of DocumentService
    private final FastDocumentService documentService;  // ← CHANGED from DocumentService

    /**
     * Upload PDF document
     */
    @PostMapping("/upload")
    public ResponseEntity<Document> uploadDocument(@RequestParam("file") MultipartFile file) {
        try {
            Document document = documentService.uploadDocument(file);
            return ResponseEntity.status(HttpStatus.CREATED).body(document);
        } catch (Exception e) {
            // Return error details
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get document by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Document> getDocumentById(@PathVariable UUID id) {
        Document document = documentService.getDocumentById(id);
        
        if (document == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(document);
    }

    /**
     * Get all documents
     */
    @GetMapping
    public ResponseEntity<List<Document>> getAllDocuments() {
        List<Document> documents = documentService.getAllDocuments();
        return ResponseEntity.ok(documents);
    }

    /**
     * Delete document
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(@PathVariable UUID id) {
        documentService.deleteDocument(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Analyze document with AI (async)
     */
    @PostMapping("/{id}/analyze")
    public ResponseEntity<Map<String, String>> analyzeDocument(
            @PathVariable UUID id,
            @RequestBody Map<String, String> request) {

        String prompt = request.get("prompt");

        if (prompt == null || prompt.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Prompt is required"));
        }

        try {
            // Start async processing with Fast service
            documentService.analyzeDocumentAsync(id, prompt);

            // Return immediately with status
            return ResponseEntity.accepted()
                    .body(Map.of(
                        "message", "Analysis started",
                        "documentId", id.toString(),
                        "status", "ANALYZING",
                        "checkStatusAt", "/api/documents/" + id
                    ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get document status (for polling during analysis)
     */
    @GetMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> getDocumentStatus(@PathVariable UUID id) {
        Document document = documentService.getDocumentById(id);

        if (document == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> status = new HashMap<>();
        status.put("id", document.getId());
        status.put("filename", document.getFilename());
        status.put("status", document.getStatus());
        status.put("totalPages", document.getTotalPages());

        // If analysis is done, include it
        if (document.getStatus() == DocumentStatus.ANALYZED) {
            status.put("analysisComplete", true);
            status.put("analysis", document.getAiAnalysis());
        } else if (document.getStatus() == DocumentStatus.ANALYZING) {
            status.put("analysisComplete", false);
            status.put("message", "Analysis in progress...");
        } else if (document.getStatus() == DocumentStatus.FAILED) {
            status.put("analysisComplete", false);
            status.put("error", document.getAiAnalysis() != null ? document.getAiAnalysis() : "Analysis failed");
        }

        return ResponseEntity.ok(status);
    }

    /**
     * ✨ NEW: Check system status (AI models + OCR availability)
     */
    @GetMapping("/system/status")
    public ResponseEntity<Map<String, Object>> getSystemStatus() {
        try {
            Object status = documentService.getSystemStatus();
            
            return ResponseEntity.ok(Map.of(
                "status", "operational",
                "capabilities", status
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of(
                        "status", "error",
                        "error", e.getMessage()
                    ));
        }
    }
}