package com.ffenf.app.ai;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ffenf.app.config.OpenAiConfig;

@Service
public class OpenAiFileService {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final OpenAiConfig openAiConfig;

    @Autowired
    public OpenAiFileService(OpenAiConfig openAiConfig) {
        this.openAiConfig = openAiConfig;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(60))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Upload a PDF file to OpenAI and get a file ID
     */
    public String uploadPdfToOpenAI(MultipartFile file) throws IOException {
        System.out.println("Uploading PDF to OpenAI: " + file.getOriginalFilename());
        
        if (!openAiConfig.isConfigured()) {
            throw new RuntimeException("OpenAI API key not configured. Please set OPENAI_API_KEY environment variable.");
        }
        
        String apiKey = openAiConfig.getKey();
        
        // Create multipart form data
        String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
        
        StringBuilder formData = new StringBuilder();
        formData.append("--").append(boundary).append("\r\n");
        formData.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(file.getOriginalFilename()).append("\"\r\n");
        formData.append("Content-Type: application/pdf\r\n\r\n");
        
        // Add purpose parameter
        String purposeData = "\r\n--" + boundary + "\r\n" +
                           "Content-Disposition: form-data; name=\"purpose\"\r\n\r\n" +
                           "assistants";
        
        // Convert file to bytes and append
        byte[] fileBytes = file.getBytes();
        String formDataStr = formData.toString();
        byte[] formDataBytes = formDataStr.getBytes();
        byte[] purposeBytes = purposeData.getBytes();
        
        // Create the complete multipart body
        byte[] boundaryBytes = ("\r\n--" + boundary + "--\r\n").getBytes();
        byte[] completeBody = new byte[formDataBytes.length + fileBytes.length + purposeBytes.length + boundaryBytes.length];
        
        int offset = 0;
        System.arraycopy(formDataBytes, 0, completeBody, offset, formDataBytes.length);
        offset += formDataBytes.length;
        System.arraycopy(fileBytes, 0, completeBody, offset, fileBytes.length);
        offset += fileBytes.length;
        System.arraycopy(purposeBytes, 0, completeBody, offset, purposeBytes.length);
        offset += purposeBytes.length;
        System.arraycopy(boundaryBytes, 0, completeBody, offset, boundaryBytes.length);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/files"))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(completeBody))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Request interrupted", e);
        }
        
        System.out.println("OpenAI File Upload Response Status: " + response.statusCode());
        System.out.println("OpenAI File Upload Response Body: " + response.body());

        if (response.statusCode() == 200) {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = objectMapper.readValue(response.body(), Map.class);
            String fileId = (String) result.get("id");
            System.out.println("File uploaded successfully with ID: " + fileId);
            return fileId;
        } else {
            throw new RuntimeException("Failed to upload file to OpenAI: " + response.body());
        }
    }

    /**
     * Upload a local PDF file to OpenAI and get a file ID
     */
    public String uploadPdfToOpenAI(File file) throws IOException {
        System.out.println("Uploading PDF to OpenAI: " + file.getName());
        
        if (!openAiConfig.isConfigured()) {
            throw new RuntimeException("OpenAI API key not configured. Please set OPENAI_API_KEY environment variable.");
        }
        
        String apiKey = openAiConfig.getKey();
        
        // Read file bytes
        byte[] fileBytes = Files.readAllBytes(file.toPath());
        
        // Create multipart form data
        String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
        
        StringBuilder formData = new StringBuilder();
        formData.append("--").append(boundary).append("\r\n");
        formData.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(file.getName()).append("\"\r\n");
        formData.append("Content-Type: application/pdf\r\n\r\n");
        
        // Add purpose parameter
        String purposeData = "\r\n--" + boundary + "\r\n" +
                           "Content-Disposition: form-data; name=\"purpose\"\r\n\r\n" +
                           "assistants";
        
        // Create the complete multipart body
        String formDataStr = formData.toString();
        byte[] formDataBytes = formDataStr.getBytes();
        byte[] purposeBytes = purposeData.getBytes();
        byte[] boundaryBytes = ("\r\n--" + boundary + "--\r\n").getBytes();
        
        byte[] completeBody = new byte[formDataBytes.length + fileBytes.length + purposeBytes.length + boundaryBytes.length];
        
        int offset = 0;
        System.arraycopy(formDataBytes, 0, completeBody, offset, formDataBytes.length);
        offset += formDataBytes.length;
        System.arraycopy(fileBytes, 0, completeBody, offset, fileBytes.length);
        offset += fileBytes.length;
        System.arraycopy(purposeBytes, 0, completeBody, offset, purposeBytes.length);
        offset += purposeBytes.length;
        System.arraycopy(boundaryBytes, 0, completeBody, offset, boundaryBytes.length);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/files"))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(completeBody))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Request interrupted", e);
        }
        
        System.out.println("OpenAI File Upload Response Status: " + response.statusCode());
        System.out.println("OpenAI File Upload Response Body: " + response.body());

        if (response.statusCode() == 200) {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = objectMapper.readValue(response.body(), Map.class);
            String fileId = (String) result.get("id");
            System.out.println("File uploaded successfully with ID: " + fileId);
            return fileId;
        } else {
            throw new RuntimeException("Failed to upload file to OpenAI: " + response.body());
        }
    }

    /**
     * Delete a file from OpenAI
     */
    public void deleteFileFromOpenAI(String fileId) {
        try {
            if (!openAiConfig.isConfigured()) {
                System.err.println("OpenAI API key not configured for file deletion");
                return;
            }
            
            String apiKey = openAiConfig.getKey();
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.openai.com/v1/files/" + fileId))
                    .header("Authorization", "Bearer " + apiKey)
                    .DELETE()
                    .build();

            HttpResponse<String> response;
            try {
                response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            System.out.println("File deletion response: " + response.statusCode());
        } catch (Exception e) {
            System.err.println("Error deleting file from OpenAI: " + e.getMessage());
        }
    }
}
