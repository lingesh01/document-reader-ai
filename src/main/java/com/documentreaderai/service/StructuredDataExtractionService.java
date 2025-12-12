package com.documentreaderai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Intelligent Structured Data Extraction
 * 
 * Combines AI analysis with regex patterns for maximum accuracy
 * Specialized for fund agreements but extensible to other document types
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class StructuredDataExtractionService {

    private final MultiModelOllamaService aiService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Extract structured data from fund agreement
     */
    public FundAgreementData extractFundAgreementData(String documentText) {
        log.info("=== STRUCTURED DATA EXTRACTION ===");
        
        FundAgreementData data = new FundAgreementData();
        
        // Phase 1: Pattern-based extraction (fast, accurate for known formats)
        extractWithPatterns(documentText, data);
        
        // Phase 2: AI-assisted extraction (for missing or complex fields)
        enhanceWithAI(documentText, data);
        
        // Phase 3: Validation and confidence scoring
        validateAndScore(data);
        
        log.info("Extraction complete - Confidence: {}%", data.getOverallConfidence());
        
        return data;
    }

    /**
     * Phase 1: Pattern-based extraction
     */
    private void extractWithPatterns(String text, FundAgreementData data) {
        log.debug("Extracting with patterns...");
        
        // Extract names
        data.setContributorName(extractName(text));
        
        // Extract PAN
        data.setPanNumber(extractPAN(text));
        
        // Extract amounts
        data.setCapitalCommitment(extractCapitalCommitment(text));
        
        // Extract dates
        data.setAgreementDate(extractDates(text));
        
        // Extract periods
        data.setLockInPeriod(extractLockInPeriod(text));
        
        // Extract fees
        data.setManagementFee(extractManagementFee(text));
        data.setCarriedInterest(extractCarriedInterest(text));
    }

    /**
     * Phase 2: AI enhancement
     */
    private void enhanceWithAI(String text, FundAgreementData data) {
        log.debug("Enhancing with AI...");
        
        // Build focused prompt for missing fields
        List<String> missingFields = data.getMissingFields();
        
        if (!missingFields.isEmpty()) {
            String prompt = buildEnhancementPrompt(missingFields);
            
            try {
                String aiResponse = aiService.analyzeDocument(text, prompt, false);
                parseAIResponse(aiResponse, data);
            } catch (Exception e) {
                log.error("AI enhancement failed", e);
            }
        }
    }

    /**
     * Phase 3: Validation
     */
    private void validateAndScore(FundAgreementData data) {
        // Validate PAN format
        if (data.getPanNumber() != null) {
            if (isValidPAN(data.getPanNumber())) {
                data.setPanConfidence(100);
            } else {
                data.setPanConfidence(50);
            }
        }
        
        // Validate amounts
        if (data.getCapitalCommitment() != null) {
            data.setAmountConfidence(data.getCapitalCommitment().contains("Rs.") || 
                                    data.getCapitalCommitment().contains("INR") ? 90 : 70);
        }
        
        // Calculate overall confidence
        data.calculateOverallConfidence();
    }

    // ==================== PATTERN EXTRACTORS ====================

    private String extractName(String text) {
        // Pattern 1: "Name: [NAME]"
        Pattern p1 = Pattern.compile("(?i)Name\\s*:?\\s*([A-Z][a-z]+(?:\\s+[A-Z][a-z]+)+)");
        Matcher m1 = p1.matcher(text);
        if (m1.find()) {
            return m1.group(1).trim();
        }
        
        // Pattern 2: "Mr./Ms./Mrs. [NAME]"
        Pattern p2 = Pattern.compile("(Mr\\.|Ms\\.|Mrs\\.)\\s+([A-Z][a-z]+(?:\\s+[A-Z][a-z]+){1,3})");
        Matcher m2 = p2.matcher(text);
        if (m2.find()) {
            return m2.group(2).trim();
        }
        
        // Pattern 3: Between "Investor" and next line
        Pattern p3 = Pattern.compile("(?i)Investor[:\\s]+([A-Z][a-z]+(?:\\s+[A-Z][a-z]+)+)");
        Matcher m3 = p3.matcher(text);
        if (m3.find()) {
            return m3.group(1).trim();
        }
        
        return null;
    }

    private String extractPAN(String text) {
        // PAN format: XXXXX1234X (5 letters, 4 digits, 1 letter)
        Pattern pattern = Pattern.compile("\\b([A-Z]{5}[0-9]{4}[A-Z])\\b");
        Matcher matcher = pattern.matcher(text);
        
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        // Try with spacing
        Pattern pattern2 = Pattern.compile("\\b([A-Z]{5})\\s*([0-9]{4})\\s*([A-Z])\\b");
        Matcher matcher2 = pattern2.matcher(text);
        if (matcher2.find()) {
            return matcher2.group(1) + matcher2.group(2) + matcher2.group(3);
        }
        
        return null;
    }

    private String extractCapitalCommitment(String text) {
        // Look for amounts with currency symbols
        List<String> patterns = Arrays.asList(
            "(?i)(?:capital\\s+commitment|commitment\\s+amount|investment\\s+amount)\\s*:?\\s*(?:Rs\\.?|INR|₹)\\s*([0-9,]+(?:\\.[0-9]{2})?)",
            "(?i)(?:Rs\\.?|INR|₹)\\s*([0-9,]+)\\s*(?:crore|lakh|lac|million)",
            "(?i)amount\\s+of\\s+(?:Rs\\.?|INR|₹)\\s*([0-9,]+)"
        );
        
        for (String patternStr : patterns) {
            Pattern pattern = Pattern.compile(patternStr);
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                String amount = matcher.group(0);
                return amount.trim();
            }
        }
        
        return null;
    }

    private String extractDates(String text) {
        List<String> foundDates = new ArrayList<>();
        
        // Pattern 1: DD/MM/YYYY or DD-MM-YYYY
        Pattern p1 = Pattern.compile("\\b([0-3]?[0-9])[/-]([0-1]?[0-9])[/-](20[0-9]{2})\\b");
        Matcher m1 = p1.matcher(text);
        while (m1.find()) {
            foundDates.add(m1.group(0));
        }
        
        // Pattern 2: DD Month YYYY
        Pattern p2 = Pattern.compile("\\b([0-3]?[0-9])\\s+(January|February|March|April|May|June|July|August|September|October|November|December)\\s+(20[0-9]{2})\\b");
        Matcher m2 = p2.matcher(text);
        while (m2.find()) {
            foundDates.add(m2.group(0));
        }
        
        return foundDates.isEmpty() ? null : String.join(", ", foundDates);
    }

    private String extractLockInPeriod(String text) {
        // Pattern: "lock-in period: X years"
        Pattern pattern = Pattern.compile("(?i)lock[- ]?in\\s+period\\s*:?\\s*([0-9]+)\\s*(year|month)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        
        if (matcher.find()) {
            return matcher.group(1) + " " + matcher.group(2) + "(s)";
        }
        
        return null;
    }

    private String extractManagementFee(String text) {
        // Pattern: "management fee: X%"
        Pattern pattern = Pattern.compile("(?i)management\\s+fee\\s*:?\\s*([0-9.]+)\\s*%");
        Matcher matcher = pattern.matcher(text);
        
        if (matcher.find()) {
            return matcher.group(1) + "%";
        }
        
        return null;
    }

    private String extractCarriedInterest(String text) {
        // Pattern: "carried interest: X%"
        Pattern pattern = Pattern.compile("(?i)carried\\s+interest\\s*:?\\s*([0-9.]+)\\s*%");
        Matcher matcher = pattern.matcher(text);
        
        if (matcher.find()) {
            return matcher.group(1) + "%";
        }
        
        return null;
    }

    // ==================== AI HELPERS ====================

    private String buildEnhancementPrompt(List<String> missingFields) {
        return String.format("""
            The following fields are missing. Extract them if present:
            %s
            
            For each field found:
            - Quote the exact text
            - Indicate which page (if known)
            
            Format as JSON:
            {
                "contributor_name": "...",
                "pan_number": "...",
                ...
            }
            
            If a field is not found, use null.
            """,
            String.join("\n", missingFields)
        );
    }

    private void parseAIResponse(String aiResponse, FundAgreementData data) {
        try {
            // Try to extract JSON from AI response
            String json = extractJSON(aiResponse);
            if (json != null) {
                @SuppressWarnings("unchecked")
                Map<String, String> extracted = objectMapper.readValue(json, Map.class);
                
                // Fill in missing fields
                if (data.getContributorName() == null && extracted.containsKey("contributor_name")) {
                    data.setContributorName(extracted.get("contributor_name"));
                }
                if (data.getPanNumber() == null && extracted.containsKey("pan_number")) {
                    data.setPanNumber(extracted.get("pan_number"));
                }
                // ... etc for other fields
            }
        } catch (Exception e) {
            log.warn("Failed to parse AI JSON response", e);
        }
    }

    private String extractJSON(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        
        if (start != -1 && end != -1 && end > start) {
            return text.substring(start, end + 1);
        }
        
        return null;
    }

    // ==================== VALIDATION ====================

    private boolean isValidPAN(String pan) {
        if (pan == null || pan.length() != 10) {
            return false;
        }
        
        Pattern pattern = Pattern.compile("[A-Z]{5}[0-9]{4}[A-Z]");
        return pattern.matcher(pan).matches();
    }

    // ==================== DATA CLASS ====================

    @Data
    public static class FundAgreementData {
        // Core fields
        private String contributorName;
        private String panNumber;
        private String capitalCommitment;
        private String lockInPeriod;
        private String managementFee;
        private String carriedInterest;
        private String agreementDate;
        
        // Additional fields
        private String fundName;
        private String address;
        private String email;
        private String phone;
        
        // Confidence scores
        private int nameConfidence = 0;
        private int panConfidence = 0;
        private int amountConfidence = 0;
        private int overallConfidence = 0;
        
        public List<String> getMissingFields() {
            List<String> missing = new ArrayList<>();
            
            if (contributorName == null) missing.add("Contributor Name");
            if (panNumber == null) missing.add("PAN Number");
            if (capitalCommitment == null) missing.add("Capital Commitment");
            if (lockInPeriod == null) missing.add("Lock-in Period");
            if (managementFee == null) missing.add("Management Fee");
            if (carriedInterest == null) missing.add("Carried Interest");
            
            return missing;
        }
        
        public void calculateOverallConfidence() {
            int total = 0;
            int count = 0;
            
            if (contributorName != null) { total += nameConfidence > 0 ? nameConfidence : 80; count++; }
            if (panNumber != null) { total += panConfidence; count++; }
            if (capitalCommitment != null) { total += amountConfidence; count++; }
            if (lockInPeriod != null) { total += 75; count++; }
            if (managementFee != null) { total += 75; count++; }
            if (carriedInterest != null) { total += 75; count++; }
            
            overallConfidence = count > 0 ? total / count : 0;
        }
        
        public String toFormattedString() {
            StringBuilder sb = new StringBuilder();
            sb.append("=== FUND AGREEMENT DATA (Confidence: ").append(overallConfidence).append("%) ===\n\n");
            
            if (contributorName != null) sb.append("Contributor: ").append(contributorName).append("\n");
            if (panNumber != null) sb.append("PAN: ").append(panNumber).append("\n");
            if (capitalCommitment != null) sb.append("Capital Commitment: ").append(capitalCommitment).append("\n");
            if (lockInPeriod != null) sb.append("Lock-in Period: ").append(lockInPeriod).append("\n");
            if (managementFee != null) sb.append("Management Fee: ").append(managementFee).append("\n");
            if (carriedInterest != null) sb.append("Carried Interest: ").append(carriedInterest).append("\n");
            if (agreementDate != null) sb.append("Agreement Date: ").append(agreementDate).append("\n");
            
            List<String> missing = getMissingFields();
            if (!missing.isEmpty()) {
                sb.append("\nMissing Fields:\n");
                missing.forEach(f -> sb.append("- ").append(f).append("\n"));
            }
            
            return sb.toString();
        }
    }
}