package com.documentreaderai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.scheduling.annotation.Async;

import com.documentreaderai.model.entity.Document;
import com.documentreaderai.model.entity.Document.DocumentStatus;
import com.documentreaderai.repository.DocumentRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * FAST Document Service
 * 
 * Uses:
 * - FastOcrPdfService for text extraction (with OCR)
 * - FastDirectAnswerService for AI answers
 * 
 * Optimized for 3-4 page documents on M3 Mac
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FastDocumentService {

    private final DocumentRepository documentRepository;
    private final FastOcrPdfService pdfService;
    private final FastDirectAnswerService aiService;

    private static final String UPLOAD_DIR = "./uploads/";
    private static final int MAX_PAGES = 10;  // Process up to 10 pages

    /**
     * Upload and extract text
     */
    @Transactional
    public Document uploadDocument(MultipartFile file) {
        try {
            // Validate
            if (file.isEmpty() || !file.getOriginalFilename().toLowerCase().endsWith(".pdf")) {
                throw new RuntimeException("Only PDF files allowed");
            }

            // Save file
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String uniqueFilename = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path filePath = uploadPath.resolve(uniqueFilename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            log.info("✓ File saved: {}", filePath);

            // Create document
            Document document = Document.builder()
                    .filename(file.getOriginalFilename())
                    .filePath(filePath.toString())
                    .fileSize(file.getSize())
                    .status(DocumentStatus.UPLOADED)
                    .build();

            document = documentRepository.save(document);

            // Extract text immediately
            processDocument(document.getId());

            return document;

        } catch (IOException e) {
            log.error("Upload failed", e);
            throw new RuntimeException("Upload failed: " + e.getMessage());
        }
    }

    /**
     * Extract text with OCR support
     */
    @Transactional
    public void processDocument(UUID documentId) {
        try {
            Document document = documentRepository.findById(documentId)
                    .orElseThrow(() -> new RuntimeException("Document not found"));

            document.setStatus(DocumentStatus.PROCESSING);
            documentRepository.saveAndFlush(document);

            // Extract text (with OCR fallback)
            String extractedText = pdfService.extractText(document.getFilePath(), MAX_PAGES);

            document.setExtractedText(extractedText);
            document.setTotalPages(MAX_PAGES);  // We only process 4 pages
            document.setStatus(DocumentStatus.READY);
            documentRepository.saveAndFlush(document);

            log.info("✓ Text extracted for: {}", documentId);

        } catch (Exception e) {
            log.error("Processing failed: " + documentId, e);
            Document document = documentRepository.findById(documentId).orElse(null);
            if (document != null) {
                document.setStatus(DocumentStatus.FAILED);
                document.setAiAnalysis("Extraction failed: " + e.getMessage());
                documentRepository.saveAndFlush(document);
            }
        }
    }

    /**
     * FAST AI analysis - direct answer only
     */
    @Async
    @Transactional
    public CompletableFuture<Document> analyzeDocumentAsync(UUID documentId, String question) {
        log.info("=== STARTING FAST ANALYSIS ===");
        log.info("Question: {}", question);

        try {
            Document document = documentRepository.findById(documentId)
                    .orElseThrow(() -> new RuntimeException("Document not found"));

            document.setStatus(DocumentStatus.ANALYZING);
            documentRepository.saveAndFlush(document);

            // Get direct answer
            String answer = aiService.getDirectAnswer(document.getExtractedText(), question);

            document.setAiAnalysis(answer);
            document.setStatus(DocumentStatus.ANALYZED);
            documentRepository.saveAndFlush(document);

            log.info("✓ Analysis complete");

            return CompletableFuture.completedFuture(document);

        } catch (Exception e) {
            log.error("Analysis failed", e);

            try {
                Document document = documentRepository.findById(documentId).orElse(null);
                if (document != null) {
                    document.setStatus(DocumentStatus.FAILED);
                    document.setAiAnalysis("Error: " + e.getMessage());
                    documentRepository.saveAndFlush(document);
                }
            } catch (Exception ex) {
                log.error("Failed to update error status", ex);
            }

            throw new RuntimeException("Analysis failed: " + e.getMessage());
        }
    }

    // CRUD operations
    public Document getDocumentById(UUID id) {
        return documentRepository.findById(id).orElse(null);
    }

    public List<Document> getAllDocuments() {
        return documentRepository.findAll();
    }

    @Transactional
    public void deleteDocument(UUID id) {
        Document document = getDocumentById(id);
        if (document != null) {
            try {
                Path filePath = Paths.get(document.getFilePath());
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                log.error("Error deleting file", e);
            }
            documentRepository.delete(document);
        }
    }

    /**
     * System status
     */
    public Object getSystemStatus() {
        return Map.of(
            "ocrAvailable", pdfService.isOcrReady(),
            "aiAvailable", aiService.isAvailable(),
            "maxPages", MAX_PAGES,
            "model", "llama3.2:3b"
        );
    }
}