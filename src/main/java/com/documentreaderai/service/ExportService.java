package com.documentreaderai.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import com.documentreaderai.model.entity.BatchJob;
import com.documentreaderai.model.entity.Document;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExportService {

    /**
     * Export batch results to Excel
     */
    public byte[] exportBatchToExcel(BatchJob batchJob) throws IOException {
        log.info("Exporting batch job to Excel: {}", batchJob.getJobName());
        
        try (Workbook workbook = new XSSFWorkbook()) {
            
            // Sheet 1: Summary
            createSummarySheet(workbook, batchJob);
            
            // Sheet 2: Document List
            createDocumentListSheet(workbook, batchJob);
            
            // Sheet 3: Extracted Data
            createExtractedDataSheet(workbook, batchJob);
            
            // Sheet 4: AI Analysis
            createAnalysisSheet(workbook, batchJob);
            
            // Write to byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            
            log.info("Excel export completed");
            return outputStream.toByteArray();
        }
    }
    
    /**
     * Create summary sheet
     */
    private void createSummarySheet(Workbook workbook, BatchJob batchJob) {
        Sheet sheet = workbook.createSheet("Summary");
        
        // Create styles
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dataStyle = workbook.createCellStyle();
        
        int rowNum = 0;
        
        // Title
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Batch Job Report");
        titleCell.setCellStyle(headerStyle);
        
        rowNum++; // Empty row
        
        // Job Details
        createDataRow(sheet, rowNum++, "Job Name:", batchJob.getJobName());
        createDataRow(sheet, rowNum++, "Job ID:", batchJob.getId().toString());
        createDataRow(sheet, rowNum++, "Status:", batchJob.getStatus().toString());
        createDataRow(sheet, rowNum++, "Total Documents:", String.valueOf(batchJob.getTotalDocuments()));
        createDataRow(sheet, rowNum++, "Successful:", String.valueOf(batchJob.getSuccessCount()));
        createDataRow(sheet, rowNum++, "Failed:", String.valueOf(batchJob.getFailureCount()));
        createDataRow(sheet, rowNum++, "Created At:", batchJob.getCreatedAt().toString());
        
        if (batchJob.getStartedAt() != null) {
            createDataRow(sheet, rowNum++, "Started At:", batchJob.getStartedAt().toString());
        }
        if (batchJob.getCompletedAt() != null) {
            createDataRow(sheet, rowNum++, "Completed At:", batchJob.getCompletedAt().toString());
        }
        
        // Auto-size columns
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }
    
    /**
     * Create document list sheet
     */
    private void createDocumentListSheet(Workbook workbook, BatchJob batchJob) {
        Sheet sheet = workbook.createSheet("Documents");
        
        CellStyle headerStyle = createHeaderStyle(workbook);
        
        // Header row
        Row headerRow = sheet.createRow(0);
        String[] headers = {"#", "Filename", "Status", "Pages", "File Size (KB)"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        // Data rows
        List<Document> documents = batchJob.getDocuments();
        for (int i = 0; i < documents.size(); i++) {
            Document doc = documents.get(i);
            Row row = sheet.createRow(i + 1);
            
            row.createCell(0).setCellValue(i + 1);
            row.createCell(1).setCellValue(doc.getFilename());
            row.createCell(2).setCellValue(doc.getStatus().toString());
            row.createCell(3).setCellValue(doc.getTotalPages() != null ? doc.getTotalPages() : 0);
            row.createCell(4).setCellValue(doc.getFileSize() / 1024.0);
        }
        
        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }
    
    /**
     * Create extracted data sheet
     */
    private void createExtractedDataSheet(Workbook workbook, BatchJob batchJob) {
        Sheet sheet = workbook.createSheet("Extracted Data");
        
        CellStyle headerStyle = createHeaderStyle(workbook);
        
        // Header row
        Row headerRow = sheet.createRow(0);
        String[] headers = {"Filename", "Names", "PAN Numbers", "Amounts", "Dates"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        // Extract data from each document
        List<Document> documents = batchJob.getDocuments();
        for (int i = 0; i < documents.size(); i++) {
            Document doc = documents.get(i);
            Row row = sheet.createRow(i + 1);
            
            row.createCell(0).setCellValue(doc.getFilename());
            
            // Extract structured data
            if (doc.getAiAnalysis() != null) {
                row.createCell(1).setCellValue(extractField(doc.getAiAnalysis(), "name"));
                row.createCell(2).setCellValue(extractField(doc.getAiAnalysis(), "PAN"));
                row.createCell(3).setCellValue(extractField(doc.getAiAnalysis(), "amount|commitment"));
                row.createCell(4).setCellValue(extractField(doc.getAiAnalysis(), "date"));
            }
        }
        
        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }
    
    /**
     * Create AI analysis sheet
     */
    private void createAnalysisSheet(Workbook workbook, BatchJob batchJob) {
        Sheet sheet = workbook.createSheet("AI Analysis");
        
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle wrapStyle = workbook.createCellStyle();
        wrapStyle.setWrapText(true);
        
        // Header row
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Filename");
        headerRow.createCell(1).setCellValue("AI Analysis");
        headerRow.getCell(0).setCellStyle(headerStyle);
        headerRow.getCell(1).setCellStyle(headerStyle);
        
        // Data rows
        List<Document> documents = batchJob.getDocuments();
        for (int i = 0; i < documents.size(); i++) {
            Document doc = documents.get(i);
            Row row = sheet.createRow(i + 1);
            
            row.createCell(0).setCellValue(doc.getFilename());
            Cell analysisCell = row.createCell(1);
            analysisCell.setCellValue(doc.getAiAnalysis() != null ? doc.getAiAnalysis() : "N/A");
            analysisCell.setCellStyle(wrapStyle);
            
            row.setHeightInPoints(100);
        }
        
        sheet.setColumnWidth(0, 8000);
        sheet.setColumnWidth(1, 20000);
    }
    
    /**
     * Export batch results to CSV
     */
    public byte[] exportBatchToCSV(BatchJob batchJob) {
        StringBuilder csv = new StringBuilder();
        
        // Header
        csv.append("Filename,Status,Pages,Names,PAN,Amounts,Dates\n");
        
        // Data rows
        for (Document doc : batchJob.getDocuments()) {
            csv.append(escapeCsv(doc.getFilename())).append(",");
            csv.append(doc.getStatus()).append(",");
            csv.append(doc.getTotalPages() != null ? doc.getTotalPages() : 0).append(",");
            
            if (doc.getAiAnalysis() != null) {
                csv.append(escapeCsv(extractField(doc.getAiAnalysis(), "name"))).append(",");
                csv.append(escapeCsv(extractField(doc.getAiAnalysis(), "PAN"))).append(",");
                csv.append(escapeCsv(extractField(doc.getAiAnalysis(), "amount|commitment"))).append(",");
                csv.append(escapeCsv(extractField(doc.getAiAnalysis(), "date")));
            }
            
            csv.append("\n");
        }
        
        return csv.toString().getBytes();
    }
    
    /**
     * Helper: Create header style
     */
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }
    
    /**
     * Helper: Create data row
     */
    private void createDataRow(Sheet sheet, int rowNum, String label, String value) {
        Row row = sheet.createRow(rowNum);
        row.createCell(0).setCellValue(label);
        row.createCell(1).setCellValue(value);
    }
    
    /**
     * Helper: Extract field from AI analysis
     */
    private String extractField(String analysis, String fieldPattern) {
        Pattern pattern = Pattern.compile("(?i)" + fieldPattern + "\\s*:?\\s*([^\n]+)", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(analysis);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "N/A";
    }
    
    /**
     * Helper: Escape CSV field
     */
    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}