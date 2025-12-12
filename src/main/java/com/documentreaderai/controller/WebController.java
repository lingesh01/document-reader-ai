package com.documentreaderai.controller;


import com.documentreaderai.model.entity.BatchJob;
import com.documentreaderai.model.entity.Document;
import com.documentreaderai.service.BatchProcessingService;
import com.documentreaderai.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class WebController {
	
	  
    private final DocumentService documentService;
    
    private final BatchProcessingService batchProcessingService;
    
    // Home page
    @GetMapping("/")
    public String index() {
        return "index";
    }
    
    // Documents list page
    @GetMapping("/documents")
    public String documents(Model model) {
        List<Document> documents = documentService.getAllDocuments();
        model.addAttribute("documents", documents);
        return "documents";
    }
    
    // Upload page
    @GetMapping("/upload")
    public String uploadPage() {
        return "index";
    }
    
    // Handle file upload
    @PostMapping("/upload")
    public String uploadFile(
            @RequestParam("file") MultipartFile file,
            RedirectAttributes redirectAttributes) {
        
        try {
            Document document = documentService.uploadDocument(file);
            redirectAttributes.addFlashAttribute("success", 
                "File uploaded successfully: " + document.getFilename());
            return "redirect:/documents";
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", 
                "Upload failed: " + e.getMessage());
            return "redirect:/";
        }
    }
    
    // Analyze page
    @GetMapping("/analyze/{id}")
    public String analyzePage(@PathVariable UUID id, Model model) {
        Document document = documentService.getDocumentById(id);
        
        if (document == null) {
            return "redirect:/documents";
        }
        
        model.addAttribute("document", document);
        return "analyze";
    }
    
    @PostMapping("/analyze/{id}")
    public String analyzeDocument(
            @PathVariable UUID id,
            @RequestParam("prompt") String prompt,
            RedirectAttributes redirectAttributes) {

        try {
            documentService.analyzeDocumentAsync(id, prompt);
            
            // Redirect with document ID so modal can track it
            return "redirect:/documents?analyzing=" + id;
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                "Analysis failed: " + e.getMessage());
            return "redirect:/analyze/" + id;
        }
    }
    
 // Add this method to WebController
    @GetMapping("/viewer/{id}")
    public String viewDocument(@PathVariable UUID id, Model model) {
        Document document = documentService.getDocumentById(id);
        
        if (document == null) {
            return "redirect:/documents";
        }
        
        model.addAttribute("document", document);
        return "document-viewer";
    }
    
    
    @GetMapping("/batch")
    public String batchUploadPage() {
        return "batch-upload";
    }

    @GetMapping("/batch/jobs")
    public String batchJobsPage(Model model) {
        List<BatchJob> batchJobs = batchProcessingService.getAllBatchJobs();
        model.addAttribute("batchJobs", batchJobs);
        return "batch-jobs";
    }

    @GetMapping("/batch/{id}")
    public String batchJobDetailPage(@PathVariable UUID id, Model model) {
        BatchJob batchJob = batchProcessingService.getBatchJobById(id);
        model.addAttribute("batchJob", batchJob);
        return "batch-detail";
    }

}
