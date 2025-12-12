package com.documentreaderai.controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.documentreaderai.model.entity.BatchJob;
import com.documentreaderai.service.BatchProcessingService;
import com.documentreaderai.service.ExportService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/batch")
@RequiredArgsConstructor
public class BatchController {

	private final BatchProcessingService batchProcessingService;
    private final ExportService exportService;
    
    /**
     * Create new batch job
     */
    @PostMapping("/upload")
    public ResponseEntity<BatchJob> createBatchJob(
            @RequestParam("jobName") String jobName,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "template", required = false) String template,
            @RequestParam("files") List<MultipartFile> files) {
        
        BatchJob batchJob = batchProcessingService.createBatchJob(
            jobName, description, template, files
        );
        
        return ResponseEntity.ok(batchJob);
    }
    
    /**
     * Start processing batch job
     */
    @PostMapping("/{id}/start")
    public ResponseEntity<Map<String, String>> startBatchJob(@PathVariable UUID id) {
        batchProcessingService.processBatchJob(id);
        
        return ResponseEntity.accepted().body(Map.of(
            "message", "Batch processing started",
            "batchJobId", id.toString()
        ));
    }
    
    /**
     * Get batch job status
     */
    @GetMapping("/{id}")
    public ResponseEntity<BatchJob> getBatchJob(@PathVariable UUID id) {
        BatchJob batchJob = batchProcessingService.getBatchJobById(id);
        return ResponseEntity.ok(batchJob);
    }
    
    /**
     * Get batch summary
     */
    @GetMapping("/{id}/summary")
    public ResponseEntity<Map<String, Object>> getBatchSummary(@PathVariable UUID id) {
        Map<String, Object> summary = batchProcessingService.getBatchSummary(id);
        return ResponseEntity.ok(summary);
    }
    
    /**
     * Get all batch jobs
     */
    @GetMapping
    public ResponseEntity<List<BatchJob>> getAllBatchJobs() {
        List<BatchJob> batchJobs = batchProcessingService.getAllBatchJobs();
        return ResponseEntity.ok(batchJobs);
    }
    
    /**
     * Cancel batch job
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelBatchJob(@PathVariable UUID id) {
        batchProcessingService.cancelBatchJob(id);
        return ResponseEntity.ok().build();
    }
    
    
    /**
     * Export batch results to Excel
     */
    @GetMapping("/{id}/export/excel")
    public ResponseEntity<byte[]> exportToExcel(@PathVariable UUID id) throws IOException {
        BatchJob batchJob = batchProcessingService.getBatchJobById(id);
        
        byte[] excelBytes = exportService.exportBatchToExcel(batchJob);
        
        return ResponseEntity.ok()
                .header("Content-Disposition", 
                        "attachment; filename=" + batchJob.getJobName() + ".xlsx")
                .header("Content-Type", 
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .body(excelBytes);
    }
    
    /**
     * Export batch results to CSV
     */
    @GetMapping("/{id}/export/csv")
    public ResponseEntity<byte[]> exportToCSV(@PathVariable UUID id) {
        BatchJob batchJob = batchProcessingService.getBatchJobById(id);
        
        byte[] csvBytes = exportService.exportBatchToCSV(batchJob);
        
        return ResponseEntity.ok()
                .header("Content-Disposition", 
                        "attachment; filename=" + batchJob.getJobName() + ".csv")
                .header("Content-Type", "text/csv")
                .body(csvBytes);
    }
}