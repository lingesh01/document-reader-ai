package com.documentreaderai.service;

import com.documentreaderai.model.entity.BatchJob;
import com.documentreaderai.model.entity.BatchJob.BatchStatus;
import com.documentreaderai.model.entity.Document;
import com.documentreaderai.model.entity.Document.DocumentStatus;
import com.documentreaderai.repository.BatchJobRepository;
import com.documentreaderai.repository.DocumentRepository;
import com.documentreaderai.service.ProductionPdfService.ExtractionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BatchProcessingService {

    private final BatchJobRepository batchJobRepository;
    private final DocumentRepository documentRepository;
    private final ProductionPdfService pdfService;  // ✅ FIXED: Use ProductionPdfService
    private final DocumentAnalysisService documentAnalysisService;
    
    private static final String UPLOAD_DIR = "./uploads/";
    private static final int MAX_CONCURRENT_WORKERS = 4; // Optimized for MacBook Air M4
    
    // Thread pool for parallel processing
    private final ExecutorService executorService = Executors.newFixedThreadPool(MAX_CONCURRENT_WORKERS);
    
    /**
     * Create a new batch job from multiple files
     */
    @Transactional
    public BatchJob createBatchJob(String jobName, String description, 
                                   String analysisTemplate, List<MultipartFile> files) {
        
        log.info("Creating batch job '{}' with {} files", jobName, files.size());
        
        // Validate
        if (files.isEmpty()) {
            throw new RuntimeException("No files provided");
        }
        
        if (files.size() > 1000) {
            throw new RuntimeException("Maximum 1000 files allowed per batch");
        }
        
        // Create batch job
        BatchJob batchJob = BatchJob.builder()
                .jobName(jobName)
                .description(description)
                .analysisTemplate(analysisTemplate)
                .status(BatchStatus.PENDING)
                .totalDocuments(files.size())
                .processedCount(0)
                .successCount(0)
                .failureCount(0)
                .build();
        
        batchJob = batchJobRepository.save(batchJob);
        
        // Upload all files
        List<Document> documents = new ArrayList<>();
        for (MultipartFile file : files) {
            try {
                Document document = uploadFile(file, batchJob);
                documents.add(document);
            } catch (Exception e) {
                log.error("Failed to upload file: {}", file.getOriginalFilename(), e);
                // Continue with other files
            }
        }
        
        batchJob.setDocuments(documents);
        batchJob = batchJobRepository.save(batchJob);
        
        log.info("Batch job created with {} documents", documents.size());
        
        return batchJob;
    }
    
    /**
     * Upload single file as part of batch
     */
    private Document uploadFile(MultipartFile file, BatchJob batchJob) throws IOException {
        // Validate
        if (!file.getOriginalFilename().toLowerCase().endsWith(".pdf")) {
            throw new RuntimeException("Only PDF files allowed: " + file.getOriginalFilename());
        }
        
        // Create upload directory
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        // Save file
        String uniqueFilename = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path filePath = uploadPath.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        
        // Create document entity
        Document document = Document.builder()
                .filename(file.getOriginalFilename())
                .filePath(filePath.toString())
                .fileSize(file.getSize())
                .status(DocumentStatus.UPLOADED)
                .batchJob(batchJob)
                .build();
        
        return documentRepository.save(document);
    }
    
    /**
     * Start processing batch job (parallel execution)
     */
    @Async
    @Transactional
    public void processBatchJob(UUID batchJobId) {
        log.info("=== BATCH PROCESSING STARTED: {} ===", batchJobId);
        
        BatchJob batchJob = batchJobRepository.findById(batchJobId)
                .orElseThrow(() -> new RuntimeException("Batch job not found"));
        
        // Update status
        batchJob.setStatus(BatchStatus.RUNNING);
        batchJob.setStartedAt(LocalDateTime.now());
        batchJobRepository.saveAndFlush(batchJob);
        
        // Get all documents in this batch
        List<Document> documents = batchJob.getDocuments();
        log.info("Processing {} documents with {} workers", documents.size(), MAX_CONCURRENT_WORKERS);
        
        // Create futures for parallel processing
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        for (Document document : documents) {
        	CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
        	    try {
        	        processDocument(document, batchJobId);
        	    } catch (Exception e) {
        	        BatchJob bj = batchJobRepository.findById(batchJobId).orElseThrow();
        	        markDocumentFailed(document, bj, e.getMessage());
        	    }
        	}, executorService);

            futures.add(future);
        }
        
        // Wait for all to complete
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } catch (Exception e) {
            log.error("Batch processing error", e);
        }
        
        // Update batch job status
        batchJob = batchJobRepository.findById(batchJobId).orElseThrow();
        batchJob.setStatus(BatchStatus.COMPLETED);
        batchJob.setCompletedAt(LocalDateTime.now());
        batchJobRepository.saveAndFlush(batchJob);
        
        log.info("=== BATCH PROCESSING COMPLETED: {} ===", batchJobId);
        log.info("Results - Success: {}, Failed: {}", batchJob.getSuccessCount(), batchJob.getFailureCount());
    }
    
    /**
     * Process single document in batch
     */
    @Transactional
    public void processDocument(Document document, UUID batchJobId) {

        try {
            log.info("Processing: {}", document.getFilename());
            
            // Step 1: Extract text with intelligence (OCR if needed)
            document.setStatus(DocumentStatus.PROCESSING);
            documentRepository.saveAndFlush(document);
            
            // ✅ FIXED: Use ProductionPdfService with intelligent extraction
            ExtractionResult result = pdfService.extractWithIntelligence(document.getFilePath());
            
            // Build full text with extraction info
            StringBuilder fullText = new StringBuilder();
            fullText.append(result.getText());
            fullText.append("\n\n=== EXTRACTION INFO ===\n\n");
            fullText.append(result.getSummary());
            
            document.setExtractedText(fullText.toString());
            document.setTotalPages(result.getPages().size());
            document.setStatus(DocumentStatus.READY);
            documentRepository.saveAndFlush(document);
            
            log.info("✓ Text extracted: {} (Method: {}, {}ms)", 
                    document.getFilename(), 
                    result.usedOcr() ? "OCR" : "Native",
                    result.getProcessingTimeMs());
            
            BatchJob batchJob = batchJobRepository.findById(batchJobId).orElseThrow();

            
            // Step 2: Analyze with AI (if template provided)
            if (batchJob.getAnalysisTemplate() != null && !batchJob.getAnalysisTemplate().isEmpty()) {
                document.setStatus(DocumentStatus.ANALYZING);
                documentRepository.saveAndFlush(document);
                
                String analysis = documentAnalysisService.analyzeFundAgreement(
                    document.getExtractedText(), 
                    batchJob.getAnalysisTemplate()
                );
                
                document.setAiAnalysis(analysis);
                document.setStatus(DocumentStatus.ANALYZED);
                documentRepository.saveAndFlush(document);
                
                log.info("✓ AI analysis complete: {}", document.getFilename());
            }
            
            // Update batch progress
            updateBatchProgress(batchJob.getId(), true);
            
            log.info("✅ Completed: {}", document.getFilename());
            
        } catch (Exception e) {
            log.error("❌ Failed: {}", document.getFilename(), e);
            throw e;
        }
    }
    
    /**
     * Mark document as failed
     */
    @Transactional
    public void markDocumentFailed(Document document, BatchJob batchJob, String error) {
        document.setStatus(DocumentStatus.FAILED);
        document.setAiAnalysis("Error: " + error);
        documentRepository.saveAndFlush(document);
        
        updateBatchProgress(batchJob.getId(), false);
    }
    
    /**
     * Update batch job progress (synchronized for thread safety)
     */
    @Transactional
    public synchronized void updateBatchProgress(UUID batchJobId, boolean success) {
        BatchJob batchJob = batchJobRepository.findById(batchJobId).orElseThrow();
        
        batchJob.setProcessedCount(batchJob.getProcessedCount() + 1);
        
        if (success) {
            batchJob.setSuccessCount(batchJob.getSuccessCount() + 1);
        } else {
            batchJob.setFailureCount(batchJob.getFailureCount() + 1);
        }
        
        batchJobRepository.saveAndFlush(batchJob);
        
        log.info("Batch progress: {}/{} ({}% complete)", 
            batchJob.getProcessedCount(), 
            batchJob.getTotalDocuments(),
            (int) batchJob.getProgressPercentage());
    }
    
    /**
     * Get all batch jobs
     */
    public List<BatchJob> getAllBatchJobs() {
        return batchJobRepository.findAllByOrderByCreatedAtDesc();
    }
    
    /**
     * Get batch job by ID
     */
    public BatchJob getBatchJobById(UUID id) {
        return batchJobRepository.findById(id).orElse(null);
    }
    
    /**
     * Cancel batch job
     */
    @Transactional
    public void cancelBatchJob(UUID batchJobId) {
        BatchJob batchJob = batchJobRepository.findById(batchJobId).orElseThrow();
        batchJob.setStatus(BatchStatus.CANCELLED);
        batchJob.setCompletedAt(LocalDateTime.now());
        batchJobRepository.save(batchJob);
        
        log.info("Batch job cancelled: {}", batchJobId);
    }
    
    /**
     * Get batch results summary with real-time progress
     */
    public Map<String, Object> getBatchSummary(UUID batchJobId) {
        BatchJob batchJob = batchJobRepository.findById(batchJobId).orElseThrow();
        
        Map<String, Object> summary = new HashMap<>();
        summary.put("jobName", batchJob.getJobName());
        summary.put("status", batchJob.getStatus());
        summary.put("totalDocuments", batchJob.getTotalDocuments());
        summary.put("processedCount", batchJob.getProcessedCount());
        summary.put("successCount", batchJob.getSuccessCount());
        summary.put("failureCount", batchJob.getFailureCount());
        summary.put("progressPercentage", batchJob.getProgressPercentage());
        summary.put("estimatedTimeRemaining", batchJob.getEstimatedTimeRemaining());
        summary.put("startedAt", batchJob.getStartedAt());
        summary.put("completedAt", batchJob.getCompletedAt());
        
        // Get document statuses
        List<Document> documents = batchJob.getDocuments();
        summary.put("documents", documents.stream()
            .map(doc -> Map.of(
                "id", doc.getId(),
                "filename", doc.getFilename(),
                "status", doc.getStatus(),
                "pages", doc.getTotalPages() != null ? doc.getTotalPages() : 0,
                "fileSize", doc.getFileSize()
            ))
            .collect(Collectors.toList()));
        
        return summary;
    }
}