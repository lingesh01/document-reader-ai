package com.documentreaderai.service;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.documentreaderai.model.entity.Document;
import com.documentreaderai.model.entity.Document.DocumentStatus;
import com.documentreaderai.repository.DocumentRepository;

import org.springframework.scheduling.annotation.Async;
import java.util.concurrent.CompletableFuture;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {
    
    private final DocumentRepository documentRepository;
    private final PdfService pdfService;
    private final OllamaService ollamaService;  // ADD THIS
    
    private static final String UPLOAD_DIR = "./uploads/";
    
    @Transactional
    public Document uploadDocument(MultipartFile file) {
        try {
            // Validate file
            if (file.isEmpty()) {
                throw new RuntimeException("File is empty");
            }
            
            if (!file.getOriginalFilename().toLowerCase().endsWith(".pdf")) {
                throw new RuntimeException("Only PDF files are allowed");
            }
            
            // Create upload directory if not exists
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            
            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String uniqueFilename = UUID.randomUUID() + "_" + originalFilename;
            Path filePath = uploadPath.resolve(uniqueFilename);
            
            // Save file to disk
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            
            log.info("File saved: {}", filePath);
            
            // Create document entity
            Document document = Document.builder()
                    .filename(originalFilename)
                    .filePath(filePath.toString())
                    .fileSize(file.getSize())
                    .status(Document.DocumentStatus.UPLOADED)
                    .build();
            
            document = documentRepository.save(document);
            
            // Process PDF immediately
            processDocument(document.getId());
            
            return document;
            
        } catch (IOException e) {
            log.error("Error uploading file", e);
            throw new RuntimeException("Failed to upload file: " + e.getMessage());
        }
    }
    
    @Transactional
    public void processDocument(UUID documentId) {
        try {
            Document document = documentRepository.findById(documentId)
                    .orElseThrow(() -> new RuntimeException("Document not found"));
            
            document.setStatus(Document.DocumentStatus.PROCESSING);
            documentRepository.save(document);
            
            // Extract text from PDF
            String extractedText = pdfService.extractTextFromPdf(document.getFilePath());
            
            // Get page count
            int pageCount = pdfService.getPageCount(document.getFilePath());
            
            // Update document
            document.setExtractedText(extractedText);
            document.setTotalPages(pageCount);
            document.setStatus(Document.DocumentStatus.READY);
            documentRepository.save(document);
            
            log.info("Document processed successfully: {}", documentId);
            
        } catch (Exception e) {
            log.error("Error processing document: " + documentId, e);
            Document document = documentRepository.findById(documentId).orElse(null);
            if (document != null) {
                document.setStatus(Document.DocumentStatus.FAILED);
                documentRepository.save(document);
            }
        }
    }
    
    public Document getDocumentById(UUID id) {
        return documentRepository.findById(id).orElse(null);
    }
    
    public List<Document> getAllDocuments() {
        return documentRepository.findAll();
    }
    
    @Transactional
    public void deleteDocument(UUID id) {
        Document document = getDocumentById(id);
        
        // Delete file from disk
        try {
            Path filePath = Paths.get(document.getFilePath());
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.error("Error deleting file", e);
        }
        
        documentRepository.delete(document);
    }
    
    
    @Async
    public CompletableFuture<Document> analyzeDocumentAsync(UUID documentId, String prompt) {
        log.info("Starting async analysis for document: {}", documentId);
        
        // Get document
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));
        
        // Get extracted text
        String text = document.getExtractedText();
        
        if (text == null || text.isEmpty()) {
            throw new RuntimeException("No text extracted from document");
        }
        
        // Update status to ANALYZING
        document.setStatus(DocumentStatus.ANALYZING);
        documentRepository.save(document);
        
        try {
            log.info("Calling Mistral AI for document: {}", documentId);
            
            // Call Mistral AI (this takes 20-30 seconds)
            String analysis = ollamaService.analyzeDocument(text, prompt);
            
            // Save analysis
            document.setAiAnalysis(analysis);
            document.setStatus(DocumentStatus.ANALYZED);
            documentRepository.save(document);
            
            log.info("Analysis completed for document: {}", documentId);
            
            return CompletableFuture.completedFuture(document);
            
        } catch (Exception e) {
            log.error("Analysis failed for document: {}", documentId, e);
            
            document.setStatus(DocumentStatus.FAILED);
            document.setAiAnalysis("Error: " + e.getMessage());
            documentRepository.save(document);
            
            throw new RuntimeException("Analysis failed: " + e.getMessage());
        }
    }

    // Keep the old synchronous method for backward compatibility (optional)
    public Document analyzeDocument(UUID documentId, String prompt) {
        try {
            return analyzeDocumentAsync(documentId, prompt).get();
        } catch (Exception e) {
            throw new RuntimeException("Analysis failed: " + e.getMessage());
        }
    }
    
    
   
}