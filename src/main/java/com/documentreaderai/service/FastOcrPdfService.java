package com.documentreaderai.service;

import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.io.File;

/**
 * FAST OCR-Enabled PDF Service
 * 
 * Reads text from BOTH:
 * 1. Native text (normal PDFs)
 * 2. Images with text (scanned/image PDFs)
 * 
 * Optimized for 3-4 pages on M3 Mac
 */
@Service
@Slf4j
public class FastOcrPdfService {

    private final Tesseract tesseract;
    private boolean ocrReady = false;
    
    public FastOcrPdfService() {
        this.tesseract = new Tesseract();
        setupTesseract();
    }

    /**
     * Setup Tesseract OCR
     */
    private void setupTesseract() {
        try {
            // M3 Mac Homebrew path
            String tessdata = "/opt/homebrew/share/tessdata";
            
            if (new File(tessdata).exists()) {
                tesseract.setDatapath(tessdata);
                tesseract.setLanguage("eng");
                tesseract.setPageSegMode(1);  // Auto
                tesseract.setOcrEngineMode(1); // LSTM neural net
                ocrReady = true;
                log.info("✓ OCR ready (Tesseract at {})", tessdata);
            } else {
                log.warn("⚠️ OCR not available - install: brew install tesseract");
            }
        } catch (Exception e) {
            log.error("OCR setup failed", e);
        }
    }

    /**
     * Extract text - tries native first, falls back to OCR
     */
    public String extractText(String pdfPath, int maxPages) {
        log.info("=== EXTRACTING TEXT ===");
        log.info("File: {}, Max pages: {}", pdfPath, maxPages);
        long startTime = System.currentTimeMillis();
        
        try (PDDocument document = Loader.loadPDF(new File(pdfPath))) {
            int totalPages = document.getNumberOfPages();
            int pagesToProcess = Math.min(totalPages, maxPages);
            
            log.info("Processing {} of {} pages", pagesToProcess, totalPages);
            
            // Try native text first
            String nativeText = extractNativeText(document, pagesToProcess);
            
            // Check quality
            int avgCharsPerPage = nativeText.length() / pagesToProcess;
            log.info("Native extraction: {} chars/page", avgCharsPerPage);
            
            if (avgCharsPerPage > 100) {
                // Good native text
                long elapsed = System.currentTimeMillis() - startTime;
                log.info("✓ Native extraction: {}ms", elapsed);
                return nativeText;
            }
            
            // Native text poor - try OCR
            if (ocrReady) {
                log.info("→ Low native text detected, switching to OCR...");
                String ocrText = extractWithOcr(document, pagesToProcess);
                long elapsed = System.currentTimeMillis() - startTime;
                log.info("✓ OCR extraction: {}ms", elapsed);
                return ocrText;
            } else {
                log.warn("⚠️ Poor text quality but OCR not available");
                return nativeText + "\n\n[WARNING: Document may be image-based. Install Tesseract: brew install tesseract]";
            }
            
        } catch (Exception e) {
            log.error("PDF extraction failed", e);
            return "[ERROR: Could not extract text - " + e.getMessage() + "]";
        }
    }

    /**
     * Extract native text (fast)
     */
    private String extractNativeText(PDDocument document, int maxPages) throws Exception {
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setStartPage(1);
        stripper.setEndPage(maxPages);
        stripper.setSortByPosition(true);
        
        return stripper.getText(document);
    }

    /**
     * Extract with OCR (slower but reads images)
     */
    private String extractWithOcr(PDDocument document, int maxPages) throws Exception {
        PDFRenderer renderer = new PDFRenderer(document);
        StringBuilder ocrText = new StringBuilder();
        
        for (int i = 0; i < maxPages; i++) {
            try {
                log.info("OCR page {}/{}...", i + 1, maxPages);
                
                // Render page at 300 DPI (good quality)
                BufferedImage image = renderer.renderImageWithDPI(i, 300);
                
                // OCR the image
                String pageText = tesseract.doOCR(image);
                
                ocrText.append("=== PAGE ").append(i + 1).append(" ===\n");
                ocrText.append(pageText).append("\n\n");
                
            } catch (TesseractException e) {
                log.error("OCR failed for page {}", i + 1, e);
                ocrText.append("[OCR failed for page ").append(i + 1).append("]\n\n");
            }
        }
        
        return ocrText.toString();
    }

    /**
     * Check if OCR is ready
     */
    public boolean isOcrReady() {
        return ocrReady;
    }
}