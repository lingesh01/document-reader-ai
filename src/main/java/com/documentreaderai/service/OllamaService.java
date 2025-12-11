package com.documentreaderai.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
@Slf4j
public class OllamaService {
    
    private final WebClient webClient;
    
    public OllamaService() {
        this.webClient = WebClient.builder()
                .baseUrl("http://localhost:11434")
                .build();
    }
    
    public String analyzeDocument(String documentText, String prompt) {
        try {
            log.info("Calling Ollama/Mistral (local AI)...");
            
            String fullPrompt = String.format("""
                You are analyzing a document. Here is the text:
                
                <document>
                %s
                </document>
                
                Task: %s
                
                Provide detailed analysis.
                """, documentText, prompt);
            
            Map<String, Object> requestBody = Map.of(
                "model", "mistral",
                "prompt", fullPrompt,
                "stream", false
            );
            
            Map<String, Object> response = webClient.post()
                    .uri("/api/generate")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            
            String result = (String) response.get("response");
            log.info("Ollama response received: {} characters", result.length());
            
            return result;
            
        } catch (Exception e) {
            log.error("Error calling Ollama", e);
            throw new RuntimeException("Failed to analyze: " + e.getMessage());
        }
    }
}