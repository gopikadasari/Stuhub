package com.ffenf.app.materials;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.Authentication;

import jakarta.servlet.http.HttpServletRequest;

import com.ffenf.app.ai.OpenAiFileService;
import com.ffenf.app.domain.CoinTransaction;
import com.ffenf.app.domain.Material;
import com.ffenf.app.domain.User;
import com.ffenf.app.repo.CoinTransactionRepository;
import com.ffenf.app.repo.MaterialBookmarkRepository;
import com.ffenf.app.repo.MaterialRepository;
import com.ffenf.app.repo.UserRepository;
import com.ffenf.app.storage.FileStorageService;


@RestController
@RequestMapping("/materials")
public class MaterialsController {

    private final MaterialRepository materials;
    private final UserRepository users;
    private final CoinTransactionRepository coinTransactions;
    private final MaterialBookmarkRepository materialBookmarks;
    private final FileStorageService fileStorageService;
    private final OpenAiFileService openAiFileService;

    public MaterialsController(MaterialRepository materials, UserRepository users, CoinTransactionRepository coinTransactions, MaterialBookmarkRepository materialBookmarks, FileStorageService fileStorageService, OpenAiFileService openAiFileService) {
        this.materials = materials;
        this.users = users;
        this.coinTransactions = coinTransactions;
        this.materialBookmarks = materialBookmarks;
        this.fileStorageService = fileStorageService;
        this.openAiFileService = openAiFileService;
    }

    public record SearchRequest(String q, String subject, String courseCode, int page, int size) {}

    @GetMapping("/test-static")
    public ResponseEntity<?> testStatic() {
        return ResponseEntity.ok(Map.of(
            "message", "Static files test endpoint working",
            "timestamp", System.currentTimeMillis(),
            "uploadEndpoint", "/materials/upload-new"
        ));
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadMaterialRedirect(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam(value = "subject", required = false) String subject,
            @RequestParam(value = "courseCode", required = false) String courseCode,
            @RequestParam(value = "tags", required = false) List<String> tags,
            HttpServletRequest request) {
        
        System.out.println("=== OLD UPLOAD ENDPOINT CALLED - REDIRECTING TO NEW ===");
        // Redirect to the new endpoint
        return uploadMaterialNew(file, title, subject, courseCode, tags, request);
    }

    @PostMapping("/upload-new")
    public ResponseEntity<?> uploadMaterialNew(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam(value = "subject", required = false) String subject,
            @RequestParam(value = "courseCode", required = false) String courseCode,
            @RequestParam(value = "tags", required = false) List<String> tags,
            HttpServletRequest request) {
        
        System.out.println("=== UPLOAD METHOD CALLED - VERSION 3.0 - NO AUTH PARAMETER ===");
        
        // Debug JWT token
        String authHeader = request.getHeader("Authorization");
        System.out.println("Authorization header: " + (authHeader != null ? authHeader.substring(0, Math.min(20, authHeader.length())) + "..." : "NULL"));
        System.out.println("Header starts with Bearer: " + (authHeader != null && authHeader.startsWith("Bearer ")));
        
        try {
            // Simple authentication check
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                System.out.println("No valid Bearer token found - Header: " + authHeader);
                return ResponseEntity.status(401).body(Map.of("error", "Please log in to upload files"));
            }
            
            String token = authHeader.substring(7);
            System.out.println("Extracted token length: " + token.length());
            String email;
            try {
                System.out.println("Creating JwtService...");
                com.ffenf.app.auth.JwtService jwtService = new com.ffenf.app.auth.JwtService();
                System.out.println("Parsing JWT token...");
                io.jsonwebtoken.Claims claims = jwtService.parse(token);
                System.out.println("JWT parsed successfully!");
                email = claims.getSubject();
                System.out.println("Extracted email: " + email);
            } catch (Exception e) {
                System.err.println("JWT parsing failed: " + e.getMessage());
                e.printStackTrace();
                return ResponseEntity.status(401).body(Map.of("error", "Invalid login token"));
            }
            
            if (email == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Invalid login token"));
            }
            
            User currentUser = users.findByEmail(email).orElseThrow(() -> 
                new RuntimeException("User not found: " + email));

            System.out.println("Upload request - File: " + file.getOriginalFilename() + ", Title: " + title + ", User: " + currentUser.getEmail());
            
            // Enhanced file validation
            if (file.isEmpty()) {
                System.err.println("Upload failed: File is empty");
                return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
            }
            
            String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
            String contentType = file.getContentType();
            boolean looksLikePdf = (contentType != null && contentType.equals("application/pdf")) || originalName.endsWith(".pdf");

            if (!looksLikePdf) {
                System.err.println("Upload failed: Invalid file type - " + contentType + ", filename: " + originalName);
                return ResponseEntity.badRequest().body(Map.of("error", "Only PDF files are supported"));
            }

            // Check file size (100MB limit)
            if (file.getSize() > 100 * 1024 * 1024) {
                System.err.println("Upload failed: File too large - " + file.getSize() + " bytes");
                return ResponseEntity.badRequest().body(Map.of("error", "File size exceeds 100MB limit"));
            }

            // Store the file locally first (more reliable than OpenAI)
            String storageKey;
            try {
                storageKey = fileStorageService.storeFile(file);
                System.out.println("File stored locally with key: " + storageKey);
            } catch (Exception e) {
                System.err.println("Failed to store file locally: " + e.getMessage());
                e.printStackTrace();
                return ResponseEntity.status(500).body(Map.of(
                    "error", "File storage failed",
                    "message", "Unable to store file: " + e.getMessage()
                ));
            }
            
            // Upload PDF to OpenAI and get file ID (optional, don't fail if this fails)
            String openaiFileId = null;
            try {
                openaiFileId = openAiFileService.uploadPdfToOpenAI(file);
                System.out.println("PDF uploaded to OpenAI with file ID: " + openaiFileId);
            } catch (Exception e) {
                System.err.println("Failed to upload PDF to OpenAI (continuing without AI features): " + e.getMessage());
                // Don't fail the entire upload if OpenAI fails
                openaiFileId = "openai_failed_" + System.currentTimeMillis();
            }

            // Create and save material
            Material material = new Material();
            material.setUserId(currentUser.getId());
            material.setTitle(title);
            material.setSubject(subject);
            material.setCourseCode(courseCode);
            material.setTags(tags != null ? String.join(",", tags) : "");
            material.setStorageKey(storageKey);
            material.setFileSize(file.getSize());
            material.setMimeType(file.getContentType());
            material.setStatus("uploaded");
            material.setTextExtract(openaiFileId); // Store OpenAI file ID instead of extracted text

            Material savedMaterial;
            try {
                savedMaterial = materials.save(material);
                System.out.println("Material saved with ID: " + savedMaterial.getId());
            } catch (Exception e) {
                System.err.println("Failed to save material to database: " + e.getMessage());
                e.printStackTrace();
                // Clean up stored file if database save fails
                try {
                    fileStorageService.deleteFile(storageKey);
                } catch (Exception cleanupException) {
                    System.err.println("Failed to cleanup file after database error: " + cleanupException.getMessage());
                }
                return ResponseEntity.status(500).body(Map.of(
                    "error", "Database save failed",
                    "message", "Unable to save material: " + e.getMessage()
                ));
            }

            // Reward user with a coin for uploading
            try {
                currentUser.setCoins(currentUser.getCoins() + 1);
                users.save(currentUser);
                System.out.println("User rewarded 1 coin, new balance: " + currentUser.getCoins());

                CoinTransaction coinTransaction = new CoinTransaction();
                coinTransaction.setUserId(currentUser.getId());
                coinTransaction.setDelta(1);
                coinTransaction.setReason("upload_reward");
                coinTransaction.setRefId(savedMaterial.getId());
                coinTransactions.save(coinTransaction);
                System.out.println("Coin transaction recorded");
            } catch (Exception e) {
                System.err.println("Failed to reward user coins (upload still successful): " + e.getMessage());
                // Don't fail the upload if coin reward fails
            }

            System.out.println("=== UPLOAD SUCCESS ===");
            return ResponseEntity.ok(savedMaterial);
        } catch (Exception e) {
            System.err.println("=== UPLOAD ERROR ===");
            System.err.println("Upload error: " + e.getMessage());
            e.printStackTrace();
            System.err.println("=== UPLOAD ERROR END ===");
            return ResponseEntity.status(500).body(Map.of(
                "error", "Upload failed",
                "message", e.getMessage(),
                "details", e.getClass().getSimpleName()
            ));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> search(@RequestParam(defaultValue = "") String q,
                                   @RequestParam(required = false) String subject,
                                   @RequestParam(required = false) String courseCode,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "20") int size,
                                   @RequestParam(defaultValue = "avgRating") String sortBy,
                                   @RequestParam(defaultValue = "desc") String sortDir,
                                   Authentication auth) {
        
        try {
            System.out.println("Search request - q: " + q + ", subject: " + subject + ", courseCode: " + courseCode + ", page: " + page + ", size: " + size + ", sortBy: " + sortBy + ", sortDir: " + sortDir);
            
            // Create dynamic sorting based on parameters
            Sort sort;
            
            try {
                // Validate and map sortBy field names
                String validSortBy = validateSortField(sortBy);
                System.out.println("Original sortBy: " + sortBy + ", Validated sortBy: " + validSortBy);
                
                if ("desc".equalsIgnoreCase(sortDir)) {
                    sort = Sort.by(validSortBy).descending();
                } else {
                    sort = Sort.by(validSortBy).ascending();
                }
                
                // Add secondary sort by createdAt for consistency
                sort = sort.and(Sort.by("createdAt").descending());
                
            } catch (Exception e) {
                System.err.println("Error creating sort: " + e.getMessage());
                // Fallback to default sorting
                sort = Sort.by("avgRating").descending().and(Sort.by("createdAt").descending());
            }
            
            Pageable pageable = PageRequest.of(page, size, sort);
            
            Page<Material> results;
            
            if (q.isEmpty() && subject == null && courseCode == null) {
                System.out.println("Searching all materials");
                results = materials.findAll(pageable);
            } else {
                // Simple search - in production, use full-text search
                if (!q.isEmpty()) {
                    System.out.println("Searching with query: " + q);
                    results = materials.findByTitleContainingIgnoreCaseOrSubjectContainingIgnoreCaseOrCourseCodeContainingIgnoreCase(
                        q, q, q, pageable);
                } else {
                    System.out.println("Searching all materials (no query)");
                    results = materials.findAll(pageable);
                }
            }

        List<Map<String, Object>> materialsList = results.getContent().stream()
            .map(m -> {
                Map<String, Object> map = new java.util.HashMap<>();
                map.put("id", m.getId());
                map.put("title", m.getTitle());
                map.put("subject", m.getSubject() != null ? m.getSubject() : "");
                map.put("courseCode", m.getCourseCode() != null ? m.getCourseCode() : "");
                map.put("avgRating", m.getAvgRating());
                map.put("ratingsCount", m.getRatingsCount());
                map.put("downloadsCount", m.getDownloadsCount());
                map.put("createdAt", m.getCreatedAt());
                
                // Add user information
                User user = users.findById(m.getUserId()).orElse(null);
                if (user != null) {
                    map.put("user", Map.of(
                        "id", user.getId(),
                        "name", user.getName()
                    ));
                } else {
                    map.put("user", Map.of(
                        "id", m.getUserId(),
                        "name", "Unknown User"
                    ));
                }
                
                // Add bookmark status
                if (auth != null) {
                    String email = auth.getName();
                    User currentUser = users.findByEmail(email).orElse(null);
                    if (currentUser != null) {
                        boolean isBookmarked = materialBookmarks.existsByUserIdAndMaterialId(currentUser.getId(), m.getId());
                        map.put("isBookmarked", isBookmarked);
                    } else {
                        map.put("isBookmarked", false);
                    }
                } else {
                    map.put("isBookmarked", false);
                }
                
                return map;
            })
            .toList();

        Map<String, Object> response = new java.util.HashMap<>();
        response.put("materials", materialsList);
        response.put("totalElements", results.getTotalElements());
        response.put("totalPages", results.getTotalPages());
        response.put("currentPage", page);
        return ResponseEntity.ok(response);
        
        } catch (Exception e) {
            System.err.println("Search error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                "error", "Search failed",
                "message", e.getMessage(),
                "details", e.getClass().getSimpleName()
            ));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getMaterial(@PathVariable UUID id, Authentication auth) {
        Material material = materials.findById(id).orElse(null);
        if (material == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> response = new java.util.HashMap<>();
        response.put("id", material.getId());
        response.put("title", material.getTitle());
        response.put("subject", material.getSubject() != null ? material.getSubject() : "");
        response.put("courseCode", material.getCourseCode() != null ? material.getCourseCode() : "");
        response.put("tags", material.getTags() != null ? material.getTags() : "");
        response.put("avgRating", material.getAvgRating());
        response.put("ratingsCount", material.getRatingsCount());
        response.put("downloadsCount", material.getDownloadsCount());
        response.put("summary", material.getSummary() != null ? material.getSummary() : "");
        response.put("flashcardsJson", material.getFlashcardsJson() != null ? material.getFlashcardsJson() : "");
        response.put("createdAt", material.getCreatedAt());
        
        // Check if material is bookmarked by current user
        if (auth != null) {
            String email = auth.getName();
            User user = users.findByEmail(email).orElse(null);
            if (user != null) {
                boolean isBookmarked = materialBookmarks.existsByUserIdAndMaterialId(user.getId(), id);
                response.put("isBookmarked", isBookmarked);
            } else {
                response.put("isBookmarked", false);
            }
        } else {
            response.put("isBookmarked", false);
        }
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/download")
    public ResponseEntity<?> trackDownload(@PathVariable UUID id, Authentication auth) {
        Material material = materials.findById(id).orElse(null);
        if (material == null) {
            return ResponseEntity.notFound().build();
        }

        // Increment download count
        material.setDownloadsCount(material.getDownloadsCount() + 1);
        materials.save(material);

        // Return download URL
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("downloadUrl", "/materials/" + material.getId() + "/file");
        response.put("fileName", material.getTitle() + ".pdf");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<Resource> downloadFile(@PathVariable UUID id) {
        Material material = materials.findById(id).orElse(null);
        if (material == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            Path filePath = fileStorageService.getFilePath(material.getStorageKey());
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }

            String contentType = material.getMimeType();
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + material.getTitle() + ".pdf\"")
                    .body(resource);

        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    private String validateSortField(String sortBy) {
        // Map frontend field names to actual entity field names
        switch (sortBy) {
            case "avgRating":
                return "avgRating";
            case "downloadsCount":
                return "downloadsCount";
            case "createdAt":
                return "createdAt";
            case "title":
                return "title";
            case "subject":
                return "subject";
            case "courseCode":
                return "courseCode";
            case "ratingsCount":
                return "ratingsCount";
            default:
                System.out.println("Invalid sort field: " + sortBy + ", defaulting to avgRating");
                return "avgRating";
        }
    }
}
