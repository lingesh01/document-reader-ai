package com.documentreaderai.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.documentreaderai.model.entity.Document;
import com.documentreaderai.model.entity.Document.DocumentStatus;
import com.documentreaderai.service.DocumentService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")  
@RequiredArgsConstructor
public class DocumentController {
	
	 private final DocumentService documentService;
	    
	    @PostMapping("/upload")
	    public ResponseEntity<Document> uploadDocument(@RequestParam("file") MultipartFile file) {
	        Document document = documentService.uploadDocument(file);
	        return ResponseEntity.status(HttpStatus.CREATED).body(document);
	    }
	    
	    @GetMapping("/{id}")
	    public ResponseEntity<Document> getDocumentById(@PathVariable UUID id) {
	        Document document = documentService.getDocumentById(id);
	        return ResponseEntity.ok(document);
	    }
	    
	    @GetMapping
	    public ResponseEntity<List<Document>> getAllDocuments() {
	        List<Document> documents = documentService.getAllDocuments();
	        return ResponseEntity.ok(documents);
	    }
	    
	    @DeleteMapping("/{id}")
	    public ResponseEntity<Void> deleteDocument(@PathVariable UUID id) {
	        documentService.deleteDocument(id);
	        return ResponseEntity.noContent().build();
	    }
	    
	    @PostMapping("/{id}/analyze")
	    public ResponseEntity<Map<String, String>> analyzeDocument(
	            @PathVariable UUID id,
	            @RequestBody Map<String, String> request) {
	        
	        String prompt = request.get("prompt");
	        
	        if (prompt == null || prompt.isEmpty()) {
	            return ResponseEntity.badRequest()
	                    .body(Map.of("error", "Prompt is required"));
	        }
	        
	        // Start async processing
	        documentService.analyzeDocumentAsync(id, prompt);
	        
	        // Return immediately with status
	        return ResponseEntity.accepted()
	                .body(Map.of(
	                    "message", "Analysis started",
	                    "documentId", id.toString(),
	                    "status", "ANALYZING",
	                    "checkStatusAt", "/api/documents/" + id
	                ));
	    }
	    
	    
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
	        
	        // If analysis is done, include it
	        if (document.getStatus() == DocumentStatus.ANALYZED) {
	            status.put("analysisComplete", true);
	            status.put("analysis", document.getAiAnalysis());
	        } else if (document.getStatus() == DocumentStatus.ANALYZING) {
	            status.put("analysisComplete", false);
	            status.put("message", "Analysis in progress...");
	        } else if (document.getStatus() == DocumentStatus.FAILED) {
	            status.put("analysisComplete", false);
	            status.put("error", document.getAiAnalysis());
	        }
	        
	        return ResponseEntity.ok(status);
	    }

}
