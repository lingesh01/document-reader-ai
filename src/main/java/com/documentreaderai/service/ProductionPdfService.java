package com.documentreaderai.service;

import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import technology.tabula.*;
import technology.tabula.extractors.SpreadsheetExtractionAlgorithm;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Production-Grade PDF Processing Service
 * 
 * Features:
 * 1. Intelligent text extraction (native + OCR fallback)
 * 2. Image-based PDF detection
 * 3. Advanced table extraction (Tabula)
 * 4. Page-by-page processing with progress
 * 5. Quality metrics and reporting
 */
@Service
@Slf4j
public class ProductionPdfService {

    private final Tesseract tesseract;
    private boolean ocrAvailable = false;
    
    // QUALITY THRESHOLDS
    private static final int MIN_TEXT_PER_PAGE = 50;  // chars per page
    private static final double IMAGE_PDF_THRESHOLD = 0.7;  // 70% low-text pages

    public ProductionPdfService() {
        this.tesseract = new Tesseract();
        initializeTesseract();
    }

    private void initializeTesseract() {
        try {
            // Try common Tesseract data locations
            String[] tessdataPaths = {
                "/opt/homebrew/share/tessdata",      // Homebrew M1/M2/M3/M4
                "/usr/local/share/tessdata",         // Standard macOS
                "/usr/share/tesseract-ocr/tessdata", // Linux
                System.getenv("TESSDATA_PREFIX")     // Environment variable
            };
            
            for (String path : tessdataPaths) {
                if (path != null && new File(path).exists()) {
                    tesseract.setDatapath(path);
                    tesseract.setLanguage("eng");
                    tesseract.setPageSegMode(1);  // Auto page segmentation with OSD
                    tesseract.setOcrEngineMode(1); // Neural nets LSTM engine
                    ocrAvailable = true;
                    log.info("✓ OCR initialized with tessdata: {}", path);
                    return;
                }
            }
            
            log.warn("⚠️ Tesseract data not found - OCR disabled");
            log.warn("To enable OCR on M4 Mac: brew install tesseract");
            
        } catch (Exception e) {
            log.error("Failed to initialize Tesseract", e);
            ocrAvailable = false;
        }
    }

    /**
     * Main extraction method - intelligently chooses best strategy
     */
    public ExtractionResult extractWithIntelligence(String filePath) {
        log.info("=== INTELLIGENT PDF EXTRACTION ===");
        log.info("File: {}", filePath);
        
        try (PDDocument document = Loader.loadPDF(new File(filePath))) {
            int pageCount = document.getNumberOfPages();
            log.info("Pages: {}", pageCount);
            
            // Phase 1: Try native text extraction
            ExtractionResult nativeResult = extractNativeText(document, filePath);
            
            // Phase 2: Analyze quality
            DocumentQuality quality = analyzeQuality(nativeResult, pageCount);
            log.info("Quality: {} - {}", quality.level, quality.description);
            
            // Phase 3: Decide strategy
            if (quality.level == QualityLevel.HIGH || quality.level == QualityLevel.MEDIUM) {
                // Native text is good enough
                log.info("✓ Using native text extraction");
                return nativeResult;
//            } else if (ocrAvailable && quality.level == QualityLevel.LOW) {
            } else if (false) { 
                // Need OCR
                log.info("→ Switching to OCR extraction (image-based PDF detected)");
                return extractWithOCR(document, filePath, nativeResult);
            }
            else {
                // OCR not available
                log.warn("⚠️ Poor text quality but OCR not available");
                nativeResult.addWarning("Document may be image-based. Install Tesseract for OCR: brew install tesseract");
                return nativeResult;
            }
            
        } catch (Exception e) {
            log.error("PDF extraction failed", e);
            throw new RuntimeException("Failed to extract PDF: " + e.getMessage());
        }
    }

    /**
     * Native text extraction (fast, no OCR)
     */
    private ExtractionResult extractNativeText(PDDocument document, String filePath) throws IOException {
        log.debug("Extracting native text...");
        long startTime = System.currentTimeMillis();
        
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setSortByPosition(true);
        stripper.setAddMoreFormatting(true);
        
        StringBuilder fullText = new StringBuilder();
        List<PageInfo> pages = new ArrayList<>();
        
        // Extract page by page for progress tracking
        for (int i = 1; i <= document.getNumberOfPages(); i++) {
            stripper.setStartPage(i);
            stripper.setEndPage(i);
            
            String pageText = stripper.getText(document);
            fullText.append(pageText);
            fullText.append("\n\n=== END OF PAGE ").append(i).append(" ===\n\n");
            
            pages.add(new PageInfo(i, pageText.length(), pageText.trim().length() < MIN_TEXT_PER_PAGE));
        }
        
        // Extract tables if document looks structured
        String tableData = extractTables(filePath);
        if (!tableData.isEmpty()) {
            fullText.append("\n\n=== EXTRACTED TABLES ===\n\n").append(tableData);
        }
        
        long elapsed = System.currentTimeMillis() - startTime;
        log.info("Native extraction completed in {}ms", elapsed);
        
        return new ExtractionResult(fullText.toString(), pages, elapsed, false);
    }

    /**
     * OCR extraction for image-based PDFs
     */
    private ExtractionResult extractWithOCR(PDDocument document, String filePath, ExtractionResult nativeResult) {
        log.info("Starting OCR extraction...");
        long startTime = System.currentTimeMillis();
        
        try {
            PDFRenderer renderer = new PDFRenderer(document);
            StringBuilder ocrText = new StringBuilder();
            
            // Add native text as context (sometimes headers/footers are selectable)
            if (nativeResult.getText().trim().length() > 100) {
                ocrText.append("=== NATIVE TEXT (HEADERS/FOOTERS) ===\n\n");
                ocrText.append(nativeResult.getText());
                ocrText.append("\n\n=== OCR TEXT (MAIN CONTENT) ===\n\n");
            }
            
            List<PageInfo> ocrPages = new ArrayList<>();
            
            for (int i = 0; i < document.getNumberOfPages(); i++) {
                try {
                    log.debug("OCR processing page {}/{}...", i + 1, document.getNumberOfPages());
                    
                    // Render page to image at 300 DPI (good quality)
                    BufferedImage image = renderer.renderImageWithDPI(i, 300);
                    
                    // OCR the image
                    String pageText = tesseract.doOCR(image);
                    
                    ocrText.append(pageText);
                    ocrText.append("\n\n=== END OF PAGE ").append(i + 1).append(" ===\n\n");
                    
                    ocrPages.add(new PageInfo(i + 1, pageText.length(), false));
                    
                } catch (TesseractException e) {
                    log.error("OCR failed for page {}", i + 1, e);
                    ocrText.append("[OCR failed for page ").append(i + 1).append("]\n\n");
                    ocrPages.add(new PageInfo(i + 1, 0, true));
                }
            }
            
            long elapsed = System.currentTimeMillis() - startTime;
            log.info("✓ OCR extraction completed in {}ms ({} seconds)", elapsed, elapsed / 1000);
            
            ExtractionResult result = new ExtractionResult(ocrText.toString(), ocrPages, elapsed, true);
            result.addInfo("OCR processing took " + (elapsed / 1000) + " seconds");
            
            return result;
            
        } catch (Exception e) {
            log.error("OCR extraction failed", e);
            nativeResult.addWarning("OCR extraction failed: " + e.getMessage());
            return nativeResult;
        }
    }

    /**
     * Extract tables using Tabula
     */
    private String extractTables(String filePath) {
        try {
            log.debug("Attempting table extraction...");
            
            PDDocument document = Loader.loadPDF(new File(filePath));
            SpreadsheetExtractionAlgorithm extractor = new SpreadsheetExtractionAlgorithm();
            
            StringBuilder tables = new StringBuilder();
            int tableCount = 0;
            
            for (int pageNum = 0; pageNum < document.getNumberOfPages(); pageNum++) {
                Page page = new ObjectExtractor(document).extract(pageNum + 1);
                List<Table> pageTables = extractor.extract(page);
                
                for (Table table : pageTables) {
                    if (table.getRows().size() > 1) {  // At least 2 rows
                        tableCount++;
                        tables.append("TABLE ").append(tableCount)
                              .append(" (Page ").append(pageNum + 1).append("):\n");
                        
                        for (List<RectangularTextContainer> row : table.getRows()) {
                            for (RectangularTextContainer cell : row) {
                                tables.append(cell.getText()).append(" | ");
                            }
                            tables.append("\n");
                        }
                        tables.append("\n");
                    }
                }
            }
            
            document.close();
            
            if (tableCount > 0) {
                log.info("✓ Extracted {} tables", tableCount);
            }
            
            return tables.toString();
            
        } catch (Exception e) {
            log.debug("Table extraction failed (not a structured document): {}", e.getMessage());
            return "";
        }
    }

    /**
     * Analyze extraction quality
     */
    private DocumentQuality analyzeQuality(ExtractionResult result, int pageCount) {
        int lowTextPages = (int) result.getPages().stream()
                .filter(PageInfo::isLowText)
                .count();
        
        double lowTextRatio = (double) lowTextPages / pageCount;
        int avgCharsPerPage = result.getText().length() / pageCount;
        
        log.debug("Quality metrics - Low text pages: {}/{} ({}%), Avg chars/page: {}", 
                lowTextPages, pageCount, (int)(lowTextRatio * 100), avgCharsPerPage);
        
        if (lowTextRatio >= IMAGE_PDF_THRESHOLD) {
            return new DocumentQuality(
                QualityLevel.LOW,
                "Image-based PDF detected (" + (int)(lowTextRatio * 100) + "% pages with minimal text)"
            );
        } else if (avgCharsPerPage < MIN_TEXT_PER_PAGE) {
            return new DocumentQuality(
                QualityLevel.MEDIUM,
                "Low text density (avg " + avgCharsPerPage + " chars/page)"
            );
        } else {
            return new DocumentQuality(
                QualityLevel.HIGH,
                "Good text extraction (avg " + avgCharsPerPage + " chars/page)"
            );
        }
    }

    /**
     * Get page count
     */
    public int getPageCount(String filePath) {
        try (PDDocument document = Loader.loadPDF(new File(filePath))) {
            return document.getNumberOfPages();
        } catch (IOException e) {
            log.error("Failed to get page count", e);
            return 0;
        }
    }

    /**
     * Check if OCR is available
     */
    public boolean isOcrAvailable() {
        return ocrAvailable;
    }

    // ========================== RESULT CLASSES ==========================

    public static class ExtractionResult {
        private final String text;
        private final List<PageInfo> pages;
        private final long processingTimeMs;
        private final boolean usedOcr;
        private final List<String> warnings = new ArrayList<>();
        private final List<String> info = new ArrayList<>();

        public ExtractionResult(String text, List<PageInfo> pages, long processingTimeMs, boolean usedOcr) {
            this.text = text;
            this.pages = pages;
            this.processingTimeMs = processingTimeMs;
            this.usedOcr = usedOcr;
        }

        public String getText() { return text; }
        public List<PageInfo> getPages() { return pages; }
        public long getProcessingTimeMs() { return processingTimeMs; }
        public boolean usedOcr() { return usedOcr; }
        public List<String> getWarnings() { return warnings; }
        public List<String> getInfo() { return info; }

        public void addWarning(String warning) { warnings.add(warning); }
        public void addInfo(String info) { this.info.add(info); }

        public String getSummary() {
            StringBuilder summary = new StringBuilder();
            summary.append("Extraction completed in ").append(processingTimeMs).append("ms\n");
            summary.append("Method: ").append(usedOcr ? "OCR" : "Native text").append("\n");
            summary.append("Pages: ").append(pages.size()).append("\n");
            summary.append("Total characters: ").append(text.length()).append("\n");
            
            if (!warnings.isEmpty()) {
                summary.append("\nWarnings:\n");
                warnings.forEach(w -> summary.append("- ").append(w).append("\n"));
            }
            
            if (!info.isEmpty()) {
                summary.append("\nInfo:\n");
                info.forEach(i -> summary.append("- ").append(i).append("\n"));
            }
            
            return summary.toString();
        }
    }

    public static class PageInfo {
        private final int pageNumber;
        private final int charCount;
        private final boolean lowText;

        public PageInfo(int pageNumber, int charCount, boolean lowText) {
            this.pageNumber = pageNumber;
            this.charCount = charCount;
            this.lowText = lowText;
        }

        public int getPageNumber() { return pageNumber; }
        public int getCharCount() { return charCount; }
        public boolean isLowText() { return lowText; }
    }

    private static class DocumentQuality {
        private final QualityLevel level;
        private final String description;

        public DocumentQuality(QualityLevel level, String description) {
            this.level = level;
            this.description = description;
        }
    }

    private enum QualityLevel {
        HIGH, MEDIUM, LOW
    }
}