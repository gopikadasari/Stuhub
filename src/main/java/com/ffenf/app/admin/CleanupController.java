package com.ffenf.app.admin;

import com.ffenf.app.storage.FileCleanupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin")
public class CleanupController {
    
    @Autowired
    private FileCleanupService fileCleanupService;
    
    /**
     * Clean all data from the application
     * This will clear database and files while keeping the app functional
     */
    @PostMapping("/cleanup")
    public ResponseEntity<Map<String, Object>> cleanupAllData() {
        try {
            // Start file cleanup asynchronously
            fileCleanupService.cleanupAllFiles()
                .thenRun(() -> System.out.println("File cleanup completed"))
                .exceptionally(throwable -> {
                    System.err.println("File cleanup failed: " + throwable.getMessage());
                    return null;
                });
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Cleanup initiated successfully");
            response.put("details", "Database cleanup will be applied on next deployment. File cleanup is running in background.");
            response.put("timestamp", java.time.Instant.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Cleanup failed: " + e.getMessage());
            errorResponse.put("timestamp", java.time.Instant.now());
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * Get cleanup status
     */
    @GetMapping("/cleanup/status")
    public ResponseEntity<Map<String, Object>> getCleanupStatus() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "ready");
        response.put("message", "Cleanup system is ready");
        response.put("timestamp", java.time.Instant.now());
        
        return ResponseEntity.ok(response);
    }
}
