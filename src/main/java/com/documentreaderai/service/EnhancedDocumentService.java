package com.documentreaderai.service;

import com.documentreaderai.model.entity.Document;
import com.documentreaderai.model.entity.Document.DocumentStatus;
import com.documentreaderai.repository.DocumentRepository;
import com.documentreaderai.service.ProductionPdfService.ExtractionResult;
import com.documentreaderai.service.StructuredDataExtractionService.FundAgreementData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class EnhancedDocumentService {

    private final DocumentRepository documentRepository;
    private final ProductionPdfService pdfService;
    private final MultiModelOllamaService aiService;
    private final StructuredDataExtractionService structuredExtraction;
    
    private static final String UPLOAD_DIR = "./uploads/";

    /**
     * Upload and process document
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

            log.info("File saved: {}", filePath);

            // Create document entity
            Document document = Document.builder()
                    .filename(file.getOriginalFilename())
                    .filePath(filePath.toString())
                    .fileSize(file.getSize())
                    .status(DocumentStatus.UPLOADED)
                    .build();

            document = documentRepository.save(document);

            // Process immediately
            processDocument(document.getId());

            return document;

        } catch (IOException e) {
            log.error("Upload failed", e);
            throw new RuntimeException("Failed to upload: " + e.getMessage());
        }
    }

    /**
     * Enhanced PDF processing with intelligence
     */
    @Transactional
    public void processDocument(UUID documentId) {
        try {
            Document document = documentRepository.findById(documentId)
                    .orElseThrow(() -> new RuntimeException("Document not found"));

            document.setStatus(DocumentStatus.PROCESSING);
            documentRepository.save(document);

            // Use intelligent extraction
            ExtractionResult result = pdfService.extractWithIntelligence(document.getFilePath());

            // Build extraction summary
            StringBuilder summary = new StringBuilder();
            summary.append(result.getText());
            summary.append("\n\n=== EXTRACTION INFO ===\n\n");
            summary.append(result.getSummary());

            document.setExtractedText(summary.toString());
            document.setTotalPages(result.getPages().size());
            document.setStatus(DocumentStatus.READY);
            documentRepository.save(document);

            log.info("✓ Document processed: {}", documentId);

        } catch (Exception e) {
            log.error("Processing failed: " + documentId, e);
            Document document = documentRepository.findById(documentId).orElse(null);
            if (document != null) {
                document.setStatus(DocumentStatus.FAILED);
                document.setAiAnalysis("Processing failed: " + e.getMessage());
                documentRepository.save(document);
            }
        }
    }

    /**
     * Enhanced async analysis with multi-model AI
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CompletableFuture<Document> analyzeDocumentAsync(UUID documentId, String prompt) {
        log.info("=== ASYNC ANALYSIS STARTED ===");
        
        try {
            Document document = documentRepository.findById(documentId)
                    .orElseThrow(() -> new RuntimeException("Document not found"));
            
            document.setStatus(DocumentStatus.ANALYZING);
            documentRepository.saveAndFlush(document);
            
            // Detect if image-based
            boolean isImageBased = detectImageBased(document);
            
            // Run multi-model analysis
            String analysis = aiService.analyzeDocument(
                document.getExtractedText(),
                prompt,
                isImageBased
            );
            
            // Try structured extraction for fund agreements
            String enhancedAnalysis = analysis;
            if (isFundAgreement(prompt)) {
                try {
                    FundAgreementData structured = structuredExtraction.extractFundAgreementData(
                        document.getExtractedText()
                    );
                    enhancedAnalysis = structured.toFormattedString() + "\n\n=== AI ANALYSIS ===\n\n" + analysis;
                } catch (Exception e) {
                    log.warn("Structured extraction failed, using AI only", e);
                }
            }
            
            document.setAiAnalysis(enhancedAnalysis);
            document.setStatus(DocumentStatus.ANALYZED);
            documentRepository.saveAndFlush(document);
            
            log.info("=== ANALYSIS COMPLETED ===");
            
            return CompletableFuture.completedFuture(document);
        
        } catch (Exception e) {
            log.error("=== ANALYSIS FAILED ===", e);
            
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

    /**
     * Detect if document is image-based
     */
    private boolean detectImageBased(Document document) {
        String text = document.getExtractedText();
        
        // Check for OCR indicators
        if (text.contains("=== OCR TEXT ===") || text.contains("OCR processing")) {
            return true;
        }
        
        // Check text density
        int avgCharsPerPage = text.length() / Math.max(1, document.getTotalPages());
        return avgCharsPerPage < 100;
    }

    /**
     * Check if prompt is for fund agreement
     */
    private boolean isFundAgreement(String prompt) {
        String lower = prompt.toLowerCase();
        return lower.contains("fund") || 
               lower.contains("agreement") || 
               lower.contains("capital") || 
               lower.contains("commitment") ||
               lower.contains("investor") ||
               lower.contains("contributor");
    }

    // Basic CRUD operations
    
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
                Files.deleteIfExists(Paths.get(document.getFilePath()));
            } catch (IOException e) {
                log.error("Failed to delete file", e);
            }
            documentRepository.delete(document);
        }
    }

    /**
     * Get AI model status
     */
    public Object getAIStatus() {
        return aiService.checkModelsAvailability();
    }
}