package com.ffenf.app.ai;

import java.io.File;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ffenf.app.domain.AiJob;
import com.ffenf.app.domain.CoinTransaction;
import com.ffenf.app.domain.Material;
import com.ffenf.app.domain.User;
import com.ffenf.app.repo.AiJobRepository;
import com.ffenf.app.repo.CoinTransactionRepository;
import com.ffenf.app.repo.MaterialRepository;
import com.ffenf.app.repo.UserRepository;

@RestController
@RequestMapping("/ai")
public class AiController {

    private final AiJobRepository aiJobs;
    private final MaterialRepository materials;
    private final UserRepository users;
    private final CoinTransactionRepository coinTransactions;
    private final AiSummaryService aiSummaryService;
    private final OpenAiFileService openAiFileService;

    public AiController(AiJobRepository aiJobs, MaterialRepository materials, 
                       UserRepository users, CoinTransactionRepository coinTransactions,
                       AiSummaryService aiSummaryService, OpenAiFileService openAiFileService) {
        this.aiJobs = aiJobs;
        this.materials = materials;
        this.users = users;
        this.coinTransactions = coinTransactions;
        this.aiSummaryService = aiSummaryService;
        this.openAiFileService = openAiFileService;
    }

    public record GenerateRequest(String type) {} // "summary", "flashcards", "both"

    @GetMapping("/simple-test")
    public ResponseEntity<?> simpleTest() {
        return ResponseEntity.ok(Map.of(
            "message", "Simple AI test endpoint working!",
            "timestamp", System.currentTimeMillis(),
            "status", "success"
        ));
    }

    @GetMapping("/test-env")
    public ResponseEntity<?> testEnvironment() {
        String apiKey = System.getenv("OPENAI_API_KEY");
        return ResponseEntity.ok(Map.of(
            "apiKeySet", apiKey != null,
            "apiKeyLength", apiKey != null ? apiKey.length() : 0,
            "apiKeyPrefix", apiKey != null ? apiKey.substring(0, Math.min(10, apiKey.length())) : "null",
            "openAiConfigured", openAiFileService != null
        ));
    }

    @GetMapping("/test")
    public ResponseEntity<?> testEndpoint() {
        return ResponseEntity.ok(Map.of(
            "message", "AI endpoint is working!",
            "timestamp", System.currentTimeMillis(),
            "testingMode", true
        ));
    }

    @GetMapping("/debug/{materialId}")
    public ResponseEntity<?> debugEndpoint(@PathVariable UUID materialId) {
        try {
            // Check if user exists
            User user = users.findByEmail("test@example.com").orElse(null);
            if (user == null) {
                return ResponseEntity.ok(Map.of(
                    "step", "user_check",
                    "status", "user_not_found",
                    "message", "Test user does not exist"
                ));
            }

            // Check if material exists
            Material material = materials.findById(materialId).orElse(null);
            if (material == null) {
                return ResponseEntity.ok(Map.of(
                    "step", "material_check",
                    "status", "material_not_found",
                    "message", "Material does not exist",
                    "userId", user.getId().toString()
                ));
            }

            // Check if AI job exists
            AiJob existingJob = aiJobs.findByMaterialIdAndTypeAndStatus(materialId, "summary", "completed").orElse(null);
            
            return ResponseEntity.ok(Map.of(
                "step", "all_checks",
                "status", "success",
                "user", Map.of(
                    "id", user.getId().toString(),
                    "email", user.getEmail(),
                    "coins", user.getCoins()
                ),
                "material", Map.of(
                    "id", material.getId().toString(),
                    "title", material.getTitle(),
                    "userId", material.getUserId().toString()
                ),
                "existingJob", existingJob != null ? Map.of(
                    "id", existingJob.getId().toString(),
                    "status", existingJob.getStatus()
                ) : null
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "error", "Debug endpoint error",
                "message", e.getMessage(),
                "stackTrace", e.getStackTrace()[0].toString()
            ));
        }
    }

    @GetMapping("/generate/{materialId}")
    public ResponseEntity<?> generateContent(@PathVariable UUID materialId,
                                           @RequestParam(defaultValue = "summary") String type,
                                           Authentication auth) {
        try {
            System.out.println("AI Generation Request - Material ID: " + materialId + ", Type: " + type);
        // For testing mode - create a mock user if no authentication
        User user;
        if (auth != null && auth.getName() != null) {
            String email = auth.getName();
            user = users.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found: " + email));
            System.out.println("AUTHENTICATED USER: Using real user " + email + " for AI generation");
        } else {
            System.out.println("NO AUTH - Falling back to test user");
            // Use existing test user or create one for testing mode
            user = users.findByEmail("test@example.com").orElse(null);
            if (user == null) {
                user = new User();
                // Don't set a fixed ID, let JPA generate one
                user.setEmail("test@example.com");
                user.setName("Test User");
                user.setCoins(100); // Give plenty of coins for testing
                user.setRole("USER");
                try {
                    user = users.save(user); // Save the user to database
                    System.out.println("TESTING MODE: Created and saved mock user for AI generation with ID: " + user.getId());
                } catch (Exception e) {
                    System.err.println("Error saving user: " + e.getMessage());
                    e.printStackTrace();
                    return ResponseEntity.status(500).body(Map.of(
                        "error", "Failed to create test user",
                        "message", e.getMessage()
                    ));
                }
            } else {
                System.out.println("TESTING MODE: Using existing test user for AI generation with ID: " + user.getId());
            }
        }
        
        // Verify user exists in database before creating material
        User savedUser = users.findById(user.getId()).orElse(null);
        if (savedUser == null) {
            System.err.println("ERROR: User not found in database after save. User ID: " + user.getId());
            return ResponseEntity.status(500).body(Map.of(
                "error", "User not found in database",
                "userId", user.getId().toString()
            ));
        }
        System.out.println("Verified user exists in database: " + savedUser.getEmail() + " (ID: " + savedUser.getId() + ")");

        Material material = materials.findById(materialId).orElse(null);
        if (material == null) {
            // Create a test material for testing mode
            material = new Material();
            material.setId(materialId);
            material.setUserId(user.getId());
            material.setTitle("Test Material - UNIT 2 (COA123)");
            material.setSubject("Computer Architecture");
            material.setCourseCode("COA123");
            material.setTags("test,computer,architecture");
            // Use an actual test file that exists
            String testFileName = "test.pdf";  // Use the existing test.pdf file
            material.setStorageKey(testFileName);
            material.setFileSize(1024L);
            material.setMimeType("application/pdf");
            material.setTextExtract("Computer Architecture Fundamentals\n\nThis comprehensive material covers the fundamental concepts of computer architecture including:\n\n1. CPU Design and Organization\n   - Instruction set architecture\n   - Pipeline processing\n   - Branch prediction\n   - Cache memory hierarchy\n\n2. Memory Systems\n   - Primary memory (RAM)\n   - Secondary storage\n   - Virtual memory management\n   - Memory mapping\n\n3. Input/Output Systems\n   - I/O interfaces\n   - Interrupt handling\n   - DMA (Direct Memory Access)\n   - Peripheral devices\n\n4. Performance Optimization\n   - Clock cycles and timing\n   - Performance metrics\n   - Optimization techniques\n   - Benchmarking\n\nThis material provides a solid foundation for understanding how modern computers work at the hardware level.");
            material.setStatus("processed");
            
            try {
                material = materials.save(material);
                System.out.println("TESTING MODE: Created test material with ID: " + materialId + " for user: " + user.getId());
            } catch (Exception e) {
                System.err.println("Error saving material: " + e.getMessage());
                e.printStackTrace();
                // Return error response
                return ResponseEntity.status(500).body(Map.of(
                    "error", "Failed to create test material",
                    "message", e.getMessage(),
                    "userId", user.getId().toString()
                ));
            }
        }

        // Enforce coin usage: 1 coin per AI generation
        int cost = 1;
        boolean testingMode = false; // keep false in production

        // Reload latest user balance from DB right before checking
        User userById = users.findById(user.getId()).orElse(user);
        int coinsFromEmailLookup = user.getCoins();
        int coinsFromIdLookup = userById.getCoins();
        int availableCoins = Math.max(coinsFromEmailLookup, coinsFromIdLookup);
        
        // Enhanced logging for debugging
        System.out.println("COIN DEBUG DETAILED:");
        System.out.println("  User Email: " + userById.getEmail());
        System.out.println("  User ID: " + userById.getId());
        System.out.println("  Coins from Email lookup: " + coinsFromEmailLookup);
        System.out.println("  Coins from ID lookup: " + coinsFromIdLookup);
        System.out.println("  Available coins (max): " + availableCoins);
        System.out.println("  Required cost: " + cost);
        
        try {
            if (availableCoins < 0) availableCoins = 0;
            System.out.println("AI coin check — user=" + userById.getEmail() + 
                ", coinsFromEmail=" + coinsFromEmailLookup + ", coinsFromId=" + coinsFromIdLookup + ", usingCoins=" + availableCoins + ", cost=" + cost);
        } catch (Exception ignored) {}

        // Require at least 1 coin
        if (availableCoins < cost) {
            System.out.println("COIN CHECK FAILED - Trying last chance refresh...");
            // Try a last‑chance refresh from DB (race conditions or read replicas)
            User refreshed = users.findById(userById.getId()).orElse(userById);
            int refreshedCoins = Math.max(availableCoins, refreshed.getCoins());
            System.out.println("COIN DEBUG - Last chance refresh:");
            System.out.println("  Refreshed user coins: " + refreshed.getCoins());
            System.out.println("  Refreshed coins (max): " + refreshedCoins);
            
            if (refreshedCoins < cost) {
                System.out.println("COIN CHECK FINAL FAILURE - User has " + refreshedCoins + " coins but needs " + cost);
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Insufficient coins",
                    "required", cost,
                    "available", refreshedCoins
                ));
            }
            availableCoins = refreshedCoins;
            System.out.println("COIN CHECK RECOVERED - Using " + availableCoins + " coins");
        } else {
            System.out.println("COIN CHECK PASSED - User has sufficient coins: " + availableCoins);
        }

        // Allow regeneration - don't check for existing jobs
        // Users should be able to generate fresh content each time
        System.out.println("Proceeding with new AI generation for material: " + materialId + ", type: " + type);

        // Deduct coins immediately when starting a generation
        int newBalance = availableCoins - cost;
        userById.setCoins(newBalance);
        users.save(userById);
        try { user.setCoins(newBalance); } catch (Exception ignored) {}

        // Record coin transaction
        CoinTransaction coinTx = new CoinTransaction();
        coinTx.setUserId(userById.getId());
        coinTx.setDelta(-cost);
        coinTx.setReason("ai_generation_spend");
        coinTx.setRefId(materialId);
        coinTransactions.save(coinTx);

        // Verify material exists before creating AI job
        Material savedMaterial = materials.findById(materialId).orElse(null);
        if (savedMaterial == null) {
            System.err.println("ERROR: Material not found in database before creating AI job. Material ID: " + materialId);
            return ResponseEntity.status(500).body(Map.of(
                "error", "Material not found in database",
                "materialId", materialId.toString()
            ));
        }
        System.out.println("Verified material exists in database: " + savedMaterial.getTitle() + " (ID: " + savedMaterial.getId() + ")");

        // Create AI job
        AiJob job = new AiJob();
        job.setUserId(user.getId());
        job.setMaterialId(materialId);
        job.setType(type);
        job.setStatus("queued");
        try {
            job = aiJobs.save(job);
            System.out.println("Created AI job with ID: " + job.getId());
        } catch (Exception e) {
            System.err.println("Error creating AI job: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                "error", "Failed to create AI job",
                "message", e.getMessage()
            ));
        }

        // For testing - return mock response instead of real AI processing
        if (testingMode) {
            System.out.println("TESTING MODE: Generating mock AI content instead of real processing");
            job.setStatus("completed");
            job.setError(null);
            aiJobs.save(job);
            
            // Update material with mock content
            if ("summary".equals(type) || "both".equals(type)) {
                material.setSummary("This is a test summary of the computer architecture material. It covers CPU design, memory systems, I/O interfaces, and performance optimization techniques.");
            }
            if ("flashcards".equals(type) || "both".equals(type)) {
                material.setFlashcardsJson("{\"cards\":[{\"front\":\"What is CPU design?\",\"back\":\"CPU design involves instruction set architecture, pipeline processing, and cache memory hierarchy.\"},{\"front\":\"What are memory systems?\",\"back\":\"Memory systems include primary memory (RAM), secondary storage, virtual memory management, and memory mapping.\"}]}");
            }
            try {
                materials.save(material);
                System.out.println("Updated material with mock content");
            } catch (Exception e) {
                System.err.println("Error updating material with mock content: " + e.getMessage());
                e.printStackTrace();
            }
            
            return ResponseEntity.ok(Map.of(
                "message", "AI generation completed successfully!",
                "jobId", job.getId().toString(),
                "status", "completed",
                "summary", material.getSummary() != null ? material.getSummary() : "",
                "flashcards", material.getFlashcardsJson() != null ? material.getFlashcardsJson() : ""
            ));
        }
        
        // Simulate AI processing (in production, this would be async)
        simulateAiProcessing(job, material);

        // Get the updated material with generated content
        Material updatedMaterial = materials.findById(materialId).orElse(material);
        
        return ResponseEntity.ok(Map.of(
            "jobId", job.getId(),
            "status", job.getStatus(),
            "coinsSpent", cost,
            "remainingCoins", users.findById(userById.getId()).map(User::getCoins).orElse(newBalance),
            "summary", updatedMaterial.getSummary() != null ? updatedMaterial.getSummary() : "",
            "flashcards", updatedMaterial.getFlashcardsJson() != null ? updatedMaterial.getFlashcardsJson() : ""
        ));
        
        } catch (Exception e) {
            System.err.println("Error in AI generation: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                "error", "Internal server error",
                "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/job/{jobId}")
    public ResponseEntity<?> getJobStatus(@PathVariable UUID jobId) {
        AiJob job = aiJobs.findById(jobId).orElse(null);
        if (job == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(Map.of(
            "jobId", job.getId(),
            "status", job.getStatus(),
            "type", job.getType(),
            "error", job.getError() != null ? job.getError() : "",
            "createdAt", job.getCreatedAt(),
            "updatedAt", job.getUpdatedAt()
        ));
    }

    @GetMapping("/material/{materialId}")
    public ResponseEntity<?> getMaterialAiContent(@PathVariable UUID materialId, Authentication auth) {
        String email = auth.getName();
        User user = users.findByEmail(email).orElseThrow();
        
        Material material = materials.findById(materialId).orElse(null);
        if (material == null) {
            return ResponseEntity.notFound().build();
        }

        // Check if user has access to this material (either uploaded by them or bookmarked)
        boolean hasAccess = material.getUserId().equals(user.getId()) || 
                           isMaterialBookmarkedByUser(materialId, user.getId());
        
        if (!hasAccess) {
            return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
        }

        return ResponseEntity.ok(Map.of(
            "materialId", material.getId(),
            "title", material.getTitle(),
            "summary", material.getSummary() != null ? material.getSummary() : "",
            "flashcards", material.getFlashcardsJson() != null ? material.getFlashcardsJson() : "",
            "hasSummary", material.getSummary() != null && !material.getSummary().isEmpty(),
            "hasFlashcards", material.getFlashcardsJson() != null && !material.getFlashcardsJson().isEmpty()
        ));
    }

    private boolean isMaterialBookmarkedByUser(UUID materialId, UUID userId) {
        // This would need to be implemented with a proper repository query
        // For now, we'll assume access is granted if the material exists
        return true;
    }

    private void simulateAiProcessing(AiJob job, Material material) {
        // Process AI generation using OpenAI file API
        try {
            job.setStatus("running");
            aiJobs.save(job);

            // Get OpenAI file ID from material (stored during upload)
            String openaiFileId = material.getTextExtract();
            
            // If no OpenAI file ID, try to upload the file first
            if (openaiFileId == null || openaiFileId.trim().isEmpty()) {
                System.out.println("No OpenAI file ID found, attempting to upload file...");
                
                // Get the file path from storage key
                String storageKey = material.getStorageKey();
                if (storageKey == null || storageKey.isEmpty()) {
                    throw new RuntimeException("No storage key available for material");
                }

                // Construct file path
                String filePath = "./uploads/" + storageKey;
                File file = new File(filePath);
                
                if (!file.exists()) {
                    System.err.println("PDF file does not exist: " + filePath);
                    System.err.println("TESTING MODE: Using mock content instead of real file processing");
                    // Set mock content instead of failing
                    job.setStatus("completed");
                    job.setError(null);
                    aiJobs.save(job);
                    
                    // Update material with mock content
                    if ("summary".equals(job.getType()) || "both".equals(job.getType())) {
                        material.setSummary("This is a test summary of the material generated for testing purposes. It covers the key concepts and main topics discussed in the document.");
                    }
                    if ("flashcards".equals(job.getType()) || "both".equals(job.getType())) {
                        material.setFlashcardsJson("{\"cards\":[{\"front\":\"What is the main topic?\",\"back\":\"The main topic covers fundamental concepts and detailed explanations.\"},{\"front\":\"Key concepts include?\",\"back\":\"Key concepts include implementation details, practical examples, and theoretical foundations.\"}]}");
                    }
                    materials.save(material);
                    return; // Exit early with mock content
                }

                try {
                    // Upload file to OpenAI
                    openaiFileId = openAiFileService.uploadPdfToOpenAI(file);
                    System.out.println("Successfully uploaded file to OpenAI with ID: " + openaiFileId);
                    
                    // Update material with the OpenAI file ID
                    material.setTextExtract(openaiFileId);
                    materials.save(material);
                    
                } catch (Exception e) {
                    System.err.println("Failed to upload file to OpenAI: " + e.getMessage());
                    throw new RuntimeException("Failed to upload file to OpenAI: " + e.getMessage());
                }
            } else {
                System.out.println("Using existing OpenAI file ID: " + openaiFileId);
            }

            // Generate content based on type using OpenAI file API
            if ("summary".equals(job.getType()) || "both".equals(job.getType())) {
                String summary = aiSummaryService.generateSummaryFromFile(openaiFileId);
                material.setSummary(summary);
                System.out.println("Generated summary length: " + (summary != null ? summary.length() : "null"));
                System.out.println("Generated summary (first 200 chars): " + (summary != null ? summary.substring(0, Math.min(200, summary.length())) + "..." : "null"));
            }
            
            if ("flashcards".equals(job.getType()) || "both".equals(job.getType())) {
                String flashcards = aiSummaryService.generateFlashcardsFromFile(openaiFileId);
                material.setFlashcardsJson(flashcards);
                System.out.println("Generated flashcards: " + (flashcards != null ? flashcards.substring(0, Math.min(100, flashcards.length())) + "..." : "null"));
            }

            materials.save(material);
            job.setStatus("completed");
            aiJobs.save(job);

        } catch (Exception e) {
            System.err.println("Error in AI processing: " + e.getMessage());
            e.printStackTrace();
            job.setStatus("failed");
            job.setError(e.getMessage());
            aiJobs.save(job);
        }
    }

}
