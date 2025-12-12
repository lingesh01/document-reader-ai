package com.documentreaderai.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;

/**
 * ULTRA-FAST Direct Answer Service
 * 
 * Optimized for MacBook Air M3:
 * - Single model (llama3.2:1b) - FAST (10-30 seconds)
 * - Direct answers ONLY - no structured formatting
 * - 3-4 pages max
 * - Simple, focused responses
 * 
 * NO multi-pass, NO complex routing, NO unnecessary processing
 */
@Service
@Slf4j
public class FastDirectAnswerService {

    private final WebClient webClient;
    
    // SINGLE MODEL STRATEGY - UPGRADED FOR 8-10 PAGES
    private static final String MODEL = "llama3.2:3b";  // 3B model - better quality, still fast
    private static final int CONTEXT = 16384;            // 16K context (handles 8-10 pages)
    private static final Duration TIMEOUT = Duration.ofSeconds(90); // 90 seconds max
    
    public FastDirectAnswerService() {
        this.webClient = WebClient.builder()
                .baseUrl("http://localhost:11434")
                .codecs(configurer -> configurer
                    .defaultCodecs()
                    .maxInMemorySize(32 * 1024 * 1024))  // 32MB
                .build();
    }

    /**
     * Get direct answer - NO formatting, just the answer
     */
    public String getDirectAnswer(String documentText, String userQuestion) {
        log.info("=== FAST DIRECT ANSWER ===");
        log.info("Question: {}", userQuestion);
        long startTime = System.currentTimeMillis();
        
        try {
            // Truncate if too long (keep first 15000 chars for 8-10 pages)
            String processedText = documentText.length() > 15000 
                ? documentText.substring(0, 15000) + "\n[Document truncated to fit context]"
                : documentText;
            
            // CRITICAL: Simple, direct prompt
            String prompt = buildDirectPrompt(processedText, userQuestion);
            
            // Call AI
            String answer = callOllama(prompt);
            
            long elapsed = System.currentTimeMillis() - startTime;
            log.info("✓ Answer received in {}ms ({} seconds)", elapsed, elapsed/1000);
            
            return cleanAnswer(answer);
            
        } catch (Exception e) {
            log.error("Fast answer failed", e);
            return "Error: " + e.getMessage() + "\n\nPlease check:\n1. Ollama is running (ollama serve)\n2. Model installed: ollama pull llama3.2:1b";
        }
    }

    /**
     * Build ultra-simple prompt for direct answers
     */
    private String buildDirectPrompt(String documentText, String userQuestion) {
        return String.format("""
            You are a helpful assistant. Answer ONLY the user's question. Be direct and concise.
            
            DOCUMENT:
            %s
            
            QUESTION: %s
            
            ANSWER (direct, no extra formatting):
            """, 
            documentText, 
            userQuestion
        );
    }

    /**
     * Call Ollama with streaming for faster response
     */
    private String callOllama(String prompt) {
        try {
            Map<String, Object> requestBody = Map.of(
                "model", MODEL,
                "prompt", prompt,
                "stream", true,  // Streaming for faster perception
                "options", Map.of(
                    "num_ctx", CONTEXT,
                    "temperature", 0.1,      // Low temp = more focused
                    "num_predict", 1024,     // Max 1024 tokens output (longer answers)
                    "num_thread", 10,        // Use all M3 cores
                    "num_gpu", 1,            // GPU acceleration
                    "top_p", 0.9,            // Focused sampling
                    "repeat_penalty", 1.1    // Avoid repetition
                )
            );
            
            log.debug("→ Sending to Ollama...");
            
            // Stream response
            StringBuilder response = new StringBuilder();
            webClient.post()
                .uri("/api/generate")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(Map.class)
                .timeout(TIMEOUT)
                .doOnNext(chunk -> {
                    if (chunk.containsKey("response")) {
                        response.append((String) chunk.get("response"));
                    }
                })
                .doOnError(e -> log.error("Stream error", e))
                .blockLast();
            
            return response.toString();
            
        } catch (Exception e) {
            log.error("Ollama call failed", e);
            throw new RuntimeException("AI failed: " + e.getMessage());
        }
    }

    /**
     * Clean up AI response - remove markdown, extra formatting
     */
    private String cleanAnswer(String rawAnswer) {
        String cleaned = rawAnswer
            .replaceAll("```json", "")
            .replaceAll("```", "")
            .replaceAll("\\*\\*", "")      // Remove bold
            .replaceAll("__", "")           // Remove underline
            .trim();
        
        // Remove "Answer:" prefix if AI added it
        if (cleaned.toLowerCase().startsWith("answer:")) {
            cleaned = cleaned.substring(7).trim();
        }
        
        return cleaned;
    }

    /**
     * Check if Ollama is available
     */
    public boolean isAvailable() {
        try {
            webClient.get()
                .uri("/api/tags")
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(5))
                .block();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}