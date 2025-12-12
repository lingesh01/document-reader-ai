package com.documentreaderai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.documentreaderai.model.entity.Document;
import com.documentreaderai.model.entity.Document.DocumentStatus;
import com.documentreaderai.repository.DocumentRepository;
import com.documentreaderai.service.ProductionPdfService.ExtractionResult;

import org.springframework.scheduling.annotation.Async;
import java.util.concurrent.CompletableFuture;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

	private final DocumentRepository documentRepository;
	private final ProductionPdfService pdfService; // ✅ FIXED: Use ProductionPdfService
	private final DocumentAnalysisService documentAnalysisService;
	private final MultiModelOllamaService multiModelOllamaService;

	private static final String UPLOAD_DIR = "./uploads/";

	/**
	 * Upload and process document
	 */
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
			Document document = Document.builder().filename(originalFilename).filePath(filePath.toString())
					.fileSize(file.getSize()).status(DocumentStatus.UPLOADED).build();

			document = documentRepository.save(document);

			// Process PDF immediately
			processDocument(document.getId());

			return document;

		} catch (IOException e) {
			log.error("Error uploading file", e);
			throw new RuntimeException("Failed to upload file: " + e.getMessage());
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

			// ✅ FIXED: Use ProductionPdfService with intelligent extraction
			ExtractionResult result = pdfService.extractWithIntelligence(document.getFilePath());

			// Build summary with extraction info
			StringBuilder fullText = new StringBuilder();
			fullText.append(result.getText());
			fullText.append("\n\n=== EXTRACTION INFO ===\n\n");
			fullText.append(result.getSummary());

			document.setExtractedText(fullText.toString());
			document.setTotalPages(result.getPages().size());
			document.setStatus(DocumentStatus.READY);
			documentRepository.save(document);

			log.info("✓ Document processed: {} (Method: {}, Time: {}ms)", documentId,
					result.usedOcr() ? "OCR" : "Native", result.getProcessingTimeMs());

		} catch (Exception e) {
			log.error("Error processing document: " + documentId, e);
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

			// ✅ FIXED: Use DocumentAnalysisService with multi-pass analysis
			String analysis = documentAnalysisService.analyzeFundAgreement(document.getExtractedText(), prompt);

			document.setAiAnalysis(analysis);
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

	// ==================== CRUD OPERATIONS ====================

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
			// Delete file from disk
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
	 * Check AI and OCR availability
	 */
	public Object getSystemStatus() {
		return Map.of("aiModels", multiModelOllamaService.checkModelsAvailability(), "ocrAvailable",
				pdfService.isOcrAvailable());
	}
}