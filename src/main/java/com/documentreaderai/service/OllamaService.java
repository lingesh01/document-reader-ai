package com.documentreaderai.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class OllamaService {

    private final WebClient webClient;
    
    // OPTIMIZED FOR MACBOOK AIR M4 - UNIVERSAL DOCUMENT PROCESSING
    private static final String MODEL = "llama3.2:3b";
    private static final int MAX_CONTEXT_TOKENS = 8192;
    private static final int MAX_CHARS = 40000;  // Increased for better coverage

    public OllamaService() {
        this.webClient = WebClient.builder()
                .baseUrl("http://localhost:11434")
                .codecs(configurer -> configurer
                    .defaultCodecs()
                    .maxInMemorySize(32 * 1024 * 1024))
                .build();
    }

    public String analyzeDocument(String documentText, String prompt) {
        try {
            log.info("=== OLLAMA SERVICE CALLED ===");
            log.info("Model: {} (Universal Document Processor)", MODEL);
            log.info("Original text length: {} characters", documentText.length());
            
            // Universal smart extraction
            String processedText = extractUniversalContent(documentText);
            
            log.info("Processing text length: {} characters", processedText.length());

            String fullPrompt = String.format("""
                You are analyzing a document. Extract information accurately.

                <document>
                %s
                </document>

                Task: %s

                INSTRUCTIONS:
                1. Extract ACTUAL VALUES from the document (ignore blank fields like [___] or [____])
                2. Be specific - include page references if possible
                3. For monetary amounts: look for Rs., INR, ₹ symbols and actual numbers
                4. For names: look in headers, signature blocks, and party details
                5. For dates: check multiple formats (DD/MM/YYYY, written form, etc.)
                6. For IDs (PAN, Aadhar, etc.): look for specific patterns
                7. If a field is blank or not found, explicitly state: "Not found in document"
                8. Provide context for each extracted value

                Format your response clearly with headings and bullet points.
                """, processedText, prompt);

            Map<String, Object> requestBody = Map.of(
                "model", MODEL,
                "prompt", fullPrompt,
                "stream", false,
                "options", Map.of(
                    "num_ctx", MAX_CONTEXT_TOKENS,
                    "temperature", 0.1,
                    "num_thread", 4
                )
            );
            
            log.info("Sending request with 8K context...");
            long startTime = System.currentTimeMillis();

            Map<String, Object> response = webClient.post()
                    .uri("/api/generate")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            
            long elapsed = System.currentTimeMillis() - startTime;
            log.info("=== RESPONDED in {}ms ({} seconds) ===", elapsed, elapsed/1000);

            String result = (String) response.get("response");
            log.info("Response length: {} characters", result != null ? result.length() : 0);

            return result;

        } catch (Exception e) {
            log.error("=== ERROR ===", e);
            throw new RuntimeException("Failed to analyze: " + e.getMessage());
        }
    }
    
    /**
     * Universal extraction logic - works for any document type
     */
    private String extractUniversalContent(String fullText) {
        if (fullText.length() <= MAX_CHARS) {
            log.info("Document fits, using full text");
            return fullText;
        }
        
        log.info("Applying universal smart extraction...");
        
        StringBuilder result = new StringBuilder();
        
        // Part 1: Document beginning (header, parties, introduction)
        int part1Size = 15000;
        result.append("=== DOCUMENT START ===\n");
        result.append(fullText.substring(0, Math.min(part1Size, fullText.length())));
        result.append("\n\n");
        
        // Part 2: Extract important lines (data-rich content)
        result.append("=== KEY INFORMATION ===\n");
        result.append(extractImportantLines(fullText));
        result.append("\n\n");
        
        // Part 3: Document end (signatures, dates, execution)
        int part3Start = Math.max(fullText.length() - 15000, part1Size);
        result.append("=== DOCUMENT END ===\n");
        result.append(fullText.substring(part3Start));
        
        String extracted = result.toString();
        log.info("Universal extraction: {} chars from {} chars ({}%)", 
                extracted.length(), fullText.length(), 
                (extracted.length() * 100 / fullText.length()));
        
        return extracted;
    }
    
    /**
     * Extract lines that likely contain important information
     */
    private String extractImportantLines(String fullText) {
        StringBuilder important = new StringBuilder();
        
        // Patterns that indicate important information
        String[] importantIndicators = {
            // Identity
            "name", "NAME", "Name:", "Mr.", "Ms.", "Mrs.", 
            
            // Financial
            "amount", "Amount", "AMOUNT", "Rs.", "INR", "₹", "rupees", "Rupees",
            "lakhs", "Lakh", "crores", "Crore", "million",
            "commitment", "Commitment", "contribution", "Contribution",
            "payment", "Payment", "fee", "Fee",
            
            // Identification
            "PAN", "pan", "Permanent Account", "Aadhar", "AADHAR",
            "passport", "Passport", "DL", "Driving License",
            
            // Dates & Time
            "date", "Date", "DATE", "dated", "Dated",
            "day of", "executed on", "signed on",
            
            // Addresses
            "address", "Address", "ADDRESS", "residence", "Residence",
            "located at", "situated at",
            
            // Contact
            "email", "Email", "phone", "Phone", "mobile", "Mobile",
            "contact", "Contact",
            
            // Agreement specifics
            "party", "Party", "PARTY", "between", "Between",
            "whereas", "Whereas", "WHEREAS",
            "witnesseth", "Witnesseth", "WITNESSETH",
            
            // Execution
            "signature", "Signature", "signed", "Signed",
            "witness", "Witness", "seal", "Seal"
        };
        
        // Number patterns
        Pattern numberPattern = Pattern.compile("\\d+[,\\d]*(?:\\.\\d+)?");
        
        // PAN pattern
        Pattern panPattern = Pattern.compile("\\b[A-Z]{5}[0-9]{4}[A-Z]\\b");
        
        // Date patterns
        Pattern datePattern = Pattern.compile(
            "\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}|" +  // DD/MM/YYYY or DD-MM-YYYY
            "\\d{1,2}\\s+(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec|January|February|March|April|May|June|July|August|September|October|November|December)\\s+\\d{4}"  // DD Month YYYY
        );
        
        String[] lines = fullText.split("\n");
        int extracted = 0;
        int maxLines = 200;  // Limit to avoid too much content
        
        for (String line : lines) {
            if (extracted >= maxLines) break;
            
            String trimmed = line.trim();
            if (trimmed.length() < 5) continue;
            
            // Skip blank placeholders
            if (trimmed.contains("[___]") || trimmed.contains("[____]") || 
                trimmed.contains("[_]") || trimmed.matches(".*_{5,}.*")) {
                continue;
            }
            
            boolean isImportant = false;
            
            // Check for important indicators
            for (String indicator : importantIndicators) {
                if (trimmed.toLowerCase().contains(indicator.toLowerCase())) {
                    isImportant = true;
                    break;
                }
            }
            
            // Check for numbers
            if (!isImportant && numberPattern.matcher(trimmed).find()) {
                isImportant = true;
            }
            
            // Check for PAN
            if (!isImportant && panPattern.matcher(trimmed).find()) {
                isImportant = true;
            }
            
            // Check for dates
            if (!isImportant && datePattern.matcher(trimmed).find()) {
                isImportant = true;
            }
            
            // Check for email addresses
            if (!isImportant && trimmed.matches(".*[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}.*")) {
                isImportant = true;
            }
            
            if (isImportant) {
                important.append(trimmed).append("\n");
                extracted++;
            }
        }
        
        log.info("Extracted {} important lines", extracted);
        
        return important.length() > 0 ? 
            important.toString() : 
            "[No specific important lines identified - using full sections]";
    }
}