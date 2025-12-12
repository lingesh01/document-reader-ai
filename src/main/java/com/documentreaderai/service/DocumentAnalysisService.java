package com.documentreaderai.service;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentAnalysisService {
    
    private final MultiModelOllamaService multiModelOllamaService;  // ✅ FIXED: Correct service name
    
    /**
     * Multi-pass analysis for fund agreements
     * Uses intelligent multi-model routing for best performance
     */
    public String analyzeFundAgreement(String documentText, String userPrompt) {
        log.info("Starting multi-pass fund agreement analysis");
        
        StringBuilder analysis = new StringBuilder();
        
        // Pass 1: Extract structured data
        analysis.append("═══ STRUCTURED DATA EXTRACTION ═══\n\n");
        analysis.append(extractStructuredData(documentText));
        analysis.append("\n\n");
        
        // Pass 2: Answer user query
        analysis.append("═══ SPECIFIC QUERY RESPONSE ═══\n\n");
        analysis.append(answerUserQuery(documentText, userPrompt));
        
        return analysis.toString();
    }
    
    /**
     * First pass: Extract key structured data
     * Uses Power Model (7B) for comprehensive extraction
     */
    private String extractStructuredData(String documentText) {
        String structuredPrompt = """
            Analyze this fund agreement document and extract the following in a structured format:
            
            1. PARTIES:
               - Contributor/Investor Name(s):
               - Fund Name:
               - Investment Manager:
               - Trustee:
            
            2. FINANCIAL DETAILS:
               - Capital Commitment Amount:
               - Currency:
               - Unit Class (A/B/C/D):
               - Management Fee:
               - Carried Interest:
            
            3. IDENTIFICATION:
               - PAN Number(s):
               - Address(es):
               - Email(s):
               - Phone Number(s):
            
            4. KEY DATES:
               - Agreement Date:
               - Signing Date:
               - Commitment Period End:
               - Any Deadlines:
            
            5. TABLES FOUND:
               - List any tables and their contents
               - Include capital calls, schedules, annexures
            
            For each field:
            - If found: Provide the exact value and page reference
            - If blank placeholder ([___]): State "BLANK - Not filled in this copy"
            - If not found: State "Not mentioned in document"
            
            Format response as clear bullet points.
            """;
        
        try {
            // ✅ FIXED: Use correct method signature with boolean parameter
            return multiModelOllamaService.analyzeDocument(documentText, structuredPrompt, false);
        } catch (Exception e) {
            log.error("Structured extraction failed", e);
            return "[Error extracting structured data: " + e.getMessage() + "]";
        }
    }
    
    /**
     * Second pass: Answer specific user question
     * Intelligently routes to Fast Model (simple) or Power Model (complex)
     */
    private String answerUserQuery(String documentText, String userPrompt) {
        String enhancedPrompt = String.format("""
            Based on the fund agreement document provided, answer this question:
            
            %s
            
            Guidelines:
            1. Search ALL pages including tables and forms
            2. If information is in a table, describe the table structure
            3. If field is blank ([___]), explicitly state it's blank
            4. Provide page numbers when referencing information
            5. If answer involves numbers/amounts, quote exactly as written
            6. If question asks for something not in document, say so clearly
            
            Be specific and cite sources.
            """, userPrompt);
        
        try {
            // ✅ FIXED: Use correct method signature
            // System automatically routes to Fast Model (simple query) or Power Model (complex)
            return multiModelOllamaService.analyzeDocument(documentText, enhancedPrompt, false);
        } catch (Exception e) {
            log.error("User query analysis failed", e);
            return "[Error analyzing query: " + e.getMessage() + "]";
        }
    }
    
    /**
     * Analyze tables specifically
     * Uses Power Model for detailed table analysis
     */
    public String analyzeTablesOnly(String documentText) {
        String tablePrompt = """
            Find and analyze ALL TABLES in this document.
            
            For each table found:
            1. Describe what the table shows
            2. Extract key data points
            3. Identify any amounts, percentages, or important values
            4. Note which page it's on
            
            Format:
            TABLE 1 (Page X):
            Purpose: [What this table shows]
            Key Data: [Important values]
            
            If no tables found, state: "No structured tables detected"
            """;
        
        try {
            return multiModelOllamaService.analyzeDocument(documentText, tablePrompt, false);
        } catch (Exception e) {
            log.error("Table analysis failed", e);
            return "[Error analyzing tables: " + e.getMessage() + "]";
        }
    }
    
    /**
     * Simple analysis - fast routing
     * Automatically uses Fast Model (1B) for simple queries
     */
    public String quickAnalyze(String documentText, String simpleQuery) {
        try {
            // Will automatically route to Fast Model (1-2 seconds)
            return multiModelOllamaService.analyzeDocument(documentText, simpleQuery, false);
        } catch (Exception e) {
            log.error("Quick analysis failed", e);
            return "[Error: " + e.getMessage() + "]";
        }
    }
    
    /**
     * Force image-based analysis
     * Uses Vision Model + OCR for scanned documents
     */
    public String analyzeImageDocument(String documentText, String prompt) {
        try {
            // Force vision model usage
            return multiModelOllamaService.analyzeDocument(documentText, prompt, true);
        } catch (Exception e) {
            log.error("Image document analysis failed", e);
            return "[Error analyzing image document: " + e.getMessage() + "]";
        }
    }
}