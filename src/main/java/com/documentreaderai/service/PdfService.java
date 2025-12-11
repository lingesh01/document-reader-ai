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

    public String extractTextFromPdf(String filePath) {
        try (PDDocument document = Loader.loadPDF(new File(filePath))) {

            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            log.info("Extracted {} characters from PDF", text.length());
            return text;

        } catch (IOException e) {
            log.error("Error extracting text from PDF: {}", filePath, e);
            throw new RuntimeException("Failed to extract text from PDF: " + e.getMessage());
        }
    }

    public int getPageCount(String filePath) {
        try (PDDocument document = Loader.loadPDF(new File(filePath))) {
            return document.getNumberOfPages();
        } catch (IOException e) {
            log.error("Error getting page count: {}", filePath, e);
            return 0;
        }
    }
}