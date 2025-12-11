package com.documentreaderai.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

@Service
@Slf4j
public class PdfService {

    /**
     * Extract text from PDF (OCR disabled for stability)
     */
    public String extractTextFromPdf(String filePath) {
        try (PDDocument document = Loader.loadPDF(new File(filePath))) {

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            stripper.setAddMoreFormatting(true);
            
            String text = stripper.getText(document);
            
            int totalPages = document.getNumberOfPages();
            log.info("Extracted {} characters from {} pages", text.length(), totalPages);
            
            // Warn if very little text (might be image-based)
            if (text.trim().length() < 100 && totalPages > 0) {
                log.warn("⚠️ Very little text extracted - document may contain images");
                text += "\n\n[Note: Some pages may contain images. Text from images not extracted.]";
            }
            
            return text;

        } catch (IOException e) {
            log.error("Error extracting text from PDF: {}", filePath, e);
            throw new RuntimeException("Failed to extract text from PDF: " + e.getMessage());
        }
    }

    /**
     * Get page count
     */
    public int getPageCount(String filePath) {
        try (PDDocument document = Loader.loadPDF(new File(filePath))) {
            int pages = document.getNumberOfPages();
            log.info("PDF has {} pages", pages);
            return pages;
        } catch (IOException e) {
            log.error("Error getting page count: {}", filePath, e);
            return 0;
        }
    }
}