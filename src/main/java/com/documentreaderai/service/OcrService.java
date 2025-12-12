package com.documentreaderai.service;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class OcrService {

    private static final String TESSDATA_PATH = "/opt/homebrew/share/tessdata";
    private static final int OCR_TIMEOUT_SECONDS = 30;
    private static final int DPI = 300;
    
    /**
     * Check if OCR is available on the system
     */
    public boolean isOcrAvailable() {
        try {
            Process process = Runtime.getRuntime().exec("tesseract --version");
            process.waitFor(5, TimeUnit.SECONDS);
            boolean available = process.exitValue() == 0;
            log.info("OCR availability: {}", available);
            return available;
        } catch (Exception e) {
            log.warn("Tesseract not available: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Perform OCR on a single page
     */
    public String performOcrOnPage(PDDocument document, int pageIndex) {
        if (!isOcrAvailable()) {
            log.warn("OCR not available, skipping page {}", pageIndex + 1);
            return null;
        }
        
        Path tempImagePath = null;
        Path tempTextPath = null;
        
        try {
            log.info("Starting OCR for page {}", pageIndex + 1);
            
            // Render page to image
            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage image = renderer.renderImageWithDPI(pageIndex, DPI, ImageType.RGB);
            
            // Save to temp file
            tempImagePath = Files.createTempFile("ocr-page-", ".png");
            ImageIO.write(image, "PNG", tempImagePath.toFile());
            
            // Prepare output path (Tesseract adds .txt automatically)
            tempTextPath = Files.createTempFile("ocr-text-", "");
            String outputBase = tempTextPath.toString();
            
            // Build Tesseract command
            List<String> command = new ArrayList<>();
            command.add("tesseract");
            command.add(tempImagePath.toString());
            command.add(outputBase);
            command.add("-l");
            command.add("eng");
            command.add("--psm");
            command.add("1");  // Automatic page segmentation with OSD
            command.add("--oem");
            command.add("1");  // LSTM neural network only
            
            // Execute OCR
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.environment().put("TESSDATA_PREFIX", TESSDATA_PATH);
            pb.redirectErrorStream(true);
            
            Process process = pb.start();
            
            // Capture output
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            
            // Wait for completion
            boolean completed = process.waitFor(OCR_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            
            if (!completed) {
                process.destroyForcibly();
                log.error("OCR timeout on page {}", pageIndex + 1);
                return null;
            }
            
            if (process.exitValue() != 0) {
                log.error("OCR failed on page {}: {}", pageIndex + 1, output);
                return null;
            }
            
            // Read result (Tesseract creates .txt file)
            Path resultFile = Path.of(outputBase + ".txt");
            if (Files.exists(resultFile)) {
                String text = Files.readString(resultFile);
                log.info("OCR extracted {} chars from page {}", text.length(), pageIndex + 1);
                Files.deleteIfExists(resultFile);
                return text;
            }
            
            return null;
            
        } catch (Exception e) {
            log.error("OCR error on page {}: {}", pageIndex + 1, e.getMessage());
            return null;
        } finally {
            // Cleanup temp files
            try {
                if (tempImagePath != null) Files.deleteIfExists(tempImagePath);
                if (tempTextPath != null) Files.deleteIfExists(tempTextPath);
            } catch (Exception e) {
                log.warn("Failed to cleanup temp files: {}", e.getMessage());
            }
        }
    }
    
    /**
     * Clean and post-process OCR text
     */
    public String cleanOcrText(String ocrText) {
        if (ocrText == null || ocrText.isEmpty()) {
            return "";
        }
        
        return ocrText
            // Remove excessive whitespace
            .replaceAll("\\s{3,}", " ")
            // Fix common OCR errors
            .replaceAll("\\b0\\b", "O")  // Zero to O in context
            .replaceAll("\\bl\\b", "I")  // lowercase L to I in context
            // Normalize line breaks
            .replaceAll("\\r\\n", "\n")
            .replaceAll("\\n{3,}", "\n\n")
            .trim();
    }
}