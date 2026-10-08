package com.ffenf.app.ai;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ffenf.app.config.OpenAiConfig;

@Service
public class AiSummaryService {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final OpenAiConfig openAiConfig;

    @Autowired
    public AiSummaryService(OpenAiConfig openAiConfig) {
        this.openAiConfig = openAiConfig;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(60))
                .build();
        this.objectMapper = new ObjectMapper();
    }


    /**
     * Generate summary using OpenAI Assistants API
     */
    public String generateSummaryFromFile(String fileId) {
        System.out.println("Generating summary from OpenAI file ID: " + fileId);
        
        if (!openAiConfig.isConfigured()) {
            return "OpenAI API key not configured. Please set OPENAI_API_KEY environment variable.";
        }
        
        String apiKey = openAiConfig.getKey();
        
        try {
            // Step 1: Create an assistant with enhanced instructions
            String assistantId = createAssistant(apiKey);
            System.out.println("Created assistant with ID: " + assistantId);
            
            // Step 2: Create a thread
            String threadId = createThread(apiKey);
            System.out.println("Created thread with ID: " + threadId);
            
            // Step 3: Add message with file attachment and enhanced prompt
            String enhancedPrompt = """
                Please provide a comprehensive and detailed summary of this PDF document. Your summary should:
                
                1. **Structure**: Organize the content into clear sections with headings
                2. **Completeness**: Cover all major topics, concepts, and important details
                3. **Accuracy**: Maintain factual accuracy and preserve key information
                4. **Clarity**: Use clear, concise language that's easy to understand
                5. **Key Points**: Highlight important definitions, formulas, examples, and conclusions
                6. **Context**: Provide context for technical terms and concepts
                
                Format your response as a well-structured summary with:
                - A brief overview/introduction
                - Main topics organized by sections
                - Key concepts and definitions
                - Important examples or case studies
                - Conclusions or main takeaways
                
                Make sure to capture the full depth and breadth of the content while maintaining readability.
                """;
            
            addMessageToThread(apiKey, threadId, fileId, enhancedPrompt);
            
            // Step 4: Run the assistant
            String runId = runAssistant(apiKey, threadId, assistantId);
            System.out.println("Started run with ID: " + runId);
            
            // Step 5: Wait for completion and get response with proper polling
            String summary = waitForCompletionAndGetResponse(apiKey, threadId, runId);
            
            // Step 6: Clean up - delete assistant
            deleteAssistant(apiKey, assistantId);
            
            return summary;
            
        } catch (Exception e) {
            System.err.println("OpenAI Assistants API Error: " + e.getMessage());
            e.printStackTrace();
            return "Unable to generate summary at this time. Error: " + e.getMessage();
        }
    }

    /**
     * Generate flashcards using OpenAI Assistants API
     */
    public String generateFlashcardsFromFile(String fileId) {
        System.out.println("Generating flashcards from OpenAI file ID: " + fileId);
        
        if (!openAiConfig.isConfigured()) {
            return "{\"cards\":[{\"front\":\"OpenAI API key not configured\",\"back\":\"Please set OPENAI_API_KEY environment variable.\"}]}";
        }
        
        String apiKey = openAiConfig.getKey();
        
        try {
            // Step 1: Create an assistant
            String assistantId = createAssistant(apiKey);
            System.out.println("Created assistant with ID: " + assistantId);
            
            // Step 2: Create a thread
            String threadId = createThread(apiKey);
            System.out.println("Created thread with ID: " + threadId);
            
            // Step 3: Add message with file attachment and enhanced prompt
            String enhancedPrompt = """
                Please create high-quality flashcards for this PDF document in JSON format. Generate 8-12 flashcards with 'front' and 'back' fields. 
                
                Requirements:
                1. **Coverage**: Focus on key concepts, definitions, formulas, important facts, and critical information
                2. **Difficulty**: Mix easy recall questions with more complex analytical questions
                3. **Format**: Use clear, concise language for both front and back
                4. **Structure**: Return valid JSON with this exact format:
                {
                  "cards": [
                    {
                      "front": "Question or term here",
                      "back": "Answer or definition here"
                    }
                  ]
                }
                
                Make sure the flashcards are educational, accurate, and help with learning the material effectively.
                """;
            
            addMessageToThread(apiKey, threadId, fileId, enhancedPrompt);
            
            // Step 4: Run the assistant
            String runId = runAssistant(apiKey, threadId, assistantId);
            System.out.println("Started run with ID: " + runId);
            
            // Step 5: Wait for completion and get response with proper polling
            String flashcards = waitForCompletionAndGetResponse(apiKey, threadId, runId);
            
            // Step 6: Clean up - delete assistant
            deleteAssistant(apiKey, assistantId);
            
            return flashcards;
            
        } catch (Exception e) {
            System.err.println("OpenAI Assistants API Error: " + e.getMessage());
            e.printStackTrace();
            return "{\"cards\":[{\"front\":\"Unable to generate flashcards\",\"back\":\"Please try again later. Error: " + e.getMessage() + "\"}]}";
        }
    }

    private String createAssistant(String apiKey) throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "gpt-4o");
        requestBody.put("name", "Educational Content Processor");
        requestBody.put("instructions", """
            You are an expert educational content processor specializing in creating comprehensive summaries and flashcards from academic materials. 
            
            Your capabilities include:
            - Analyzing complex academic content with precision
            - Creating structured, well-organized summaries
            - Generating educational flashcards that promote effective learning
            - Maintaining accuracy while improving readability
            - Identifying key concepts, definitions, and important details
            
            Always prioritize accuracy, completeness, and educational value in your responses.
            """);
        requestBody.put("tools", new Object[]{Map.of("type", "file_search")});

        String jsonBody = objectMapper.writeValueAsString(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/assistants"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .header("OpenAI-Beta", "assistants=v2")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Request interrupted", e);
        }

        if (response.statusCode() == 200) {
            Map<String, Object> result = objectMapper.readValue(response.body(), Map.class);
            return (String) result.get("id");
        } else {
            throw new RuntimeException("Failed to create assistant: " + response.body());
        }
    }

    private String createThread(String apiKey) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/threads"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .header("OpenAI-Beta", "assistants=v2")
                .POST(HttpRequest.BodyPublishers.ofString("{}"))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Request interrupted", e);
        }

        if (response.statusCode() == 200) {
            Map<String, Object> result = objectMapper.readValue(response.body(), Map.class);
            return (String) result.get("id");
        } else {
            throw new RuntimeException("Failed to create thread: " + response.body());
        }
    }

    private void addMessageToThread(String apiKey, String threadId, String fileId, String message) throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("role", "user");
        requestBody.put("content", message);
        requestBody.put("attachments", new Object[]{
            Map.of("file_id", fileId, "tools", new Object[]{Map.of("type", "file_search")})
        });

        String jsonBody = objectMapper.writeValueAsString(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/threads/" + threadId + "/messages"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .header("OpenAI-Beta", "assistants=v2")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Request interrupted", e);
        }

        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to add message to thread: " + response.body());
        }
    }

    private String runAssistant(String apiKey, String threadId, String assistantId) throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("assistant_id", assistantId);

        String jsonBody = objectMapper.writeValueAsString(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/threads/" + threadId + "/runs"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .header("OpenAI-Beta", "assistants=v2")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Request interrupted", e);
        }

        if (response.statusCode() == 200) {
            Map<String, Object> result = objectMapper.readValue(response.body(), Map.class);
            return (String) result.get("id");
        } else {
            throw new RuntimeException("Failed to run assistant: " + response.body());
        }
    }

    private String waitForCompletionAndGetResponse(String apiKey, String threadId, String runId) throws Exception {
        // Poll for completion with proper timeout handling
        int maxAttempts = 30; // 5 minutes max (30 * 10 seconds)
        int attempt = 0;
        
        while (attempt < maxAttempts) {
            // Check run status
            HttpRequest statusRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.openai.com/v1/threads/" + threadId + "/runs/" + runId))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("OpenAI-Beta", "assistants=v2")
                    .GET()
                    .build();

            HttpResponse<String> statusResponse;
            try {
                statusResponse = httpClient.send(statusRequest, HttpResponse.BodyHandlers.ofString());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Request interrupted", e);
            }

            if (statusResponse.statusCode() == 200) {
                Map<String, Object> statusResult = objectMapper.readValue(statusResponse.body(), Map.class);
                String status = (String) statusResult.get("status");
                
                System.out.println("Run status: " + status + " (attempt " + (attempt + 1) + ")");
                
                if ("completed".equals(status)) {
                    break;
                } else if ("failed".equals(status) || "cancelled".equals(status) || "expired".equals(status)) {
                    String error = (String) statusResult.get("last_error");
                    throw new RuntimeException("Run failed with status: " + status + ", error: " + error);
                }
                
                // Wait before next poll
                Thread.sleep(10000); // 10 seconds
                attempt++;
            } else {
                throw new RuntimeException("Failed to check run status: " + statusResponse.body());
            }
        }
        
        if (attempt >= maxAttempts) {
            throw new RuntimeException("Run did not complete within timeout period");
        }
        
        // Get messages from thread
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/threads/" + threadId + "/messages"))
                .header("Authorization", "Bearer " + apiKey)
                .header("OpenAI-Beta", "assistants=v2")
                .GET()
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Request interrupted", e);
        }

        if (response.statusCode() == 200) {
            Map<String, Object> result = objectMapper.readValue(response.body(), Map.class);
            Object dataObj = result.get("data");
            
            // Debug logging to understand response structure
            System.out.println("Response data type: " + (dataObj != null ? dataObj.getClass().getSimpleName() : "null"));
            
            // Handle both ArrayList and Object[] cases
            Object[] data;
            if (dataObj instanceof java.util.List) {
                java.util.List<?> dataList = (java.util.List<?>) dataObj;
                data = dataList.toArray();
            } else {
                data = (Object[]) dataObj;
            }
            
            // Find the assistant's response
            for (Object messageObj : data) {
                Map<String, Object> message = (Map<String, Object>) messageObj;
                if ("assistant".equals(message.get("role"))) {
                    Object contentObj = message.get("content");
                    System.out.println("Content object type: " + (contentObj != null ? contentObj.getClass().getSimpleName() : "null"));
                    
                    // Handle both ArrayList and Object[] cases for content
                    Object[] content;
                    if (contentObj instanceof java.util.List) {
                        java.util.List<?> contentList = (java.util.List<?>) contentObj;
                        content = contentList.toArray();
                    } else {
                        content = (Object[]) contentObj;
                    }
                    
                    if (content.length > 0) {
                        Map<String, Object> contentItem = (Map<String, Object>) content[0];
                        if ("text".equals(contentItem.get("type"))) {
                            Object textObj = contentItem.get("text");
                            if (textObj instanceof Map) {
                                Map<String, Object> text = (Map<String, Object>) textObj;
                                Object valueObj = text.get("value");
                                if (valueObj instanceof String) {
                                    return (String) valueObj;
                                } else {
                                    System.err.println("Expected String value but got: " + valueObj.getClass().getSimpleName());
                                    return valueObj.toString();
                                }
                            } else {
                                System.err.println("Expected Map for text but got: " + textObj.getClass().getSimpleName());
                                return textObj.toString();
                            }
                        }
                    }
                }
            }
        }
        
        throw new RuntimeException("Failed to get response from assistant");
    }

    private void deleteAssistant(String apiKey, String assistantId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.openai.com/v1/assistants/" + assistantId))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("OpenAI-Beta", "assistants=v2")
                    .DELETE()
                    .build();

            HttpResponse<String> response;
            try {
                response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            System.out.println("Assistant deletion response: " + response.statusCode());
        } catch (Exception e) {
            System.err.println("Error deleting assistant: " + e.getMessage());
        }
    }

}