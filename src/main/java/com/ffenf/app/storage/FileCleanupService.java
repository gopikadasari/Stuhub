package com.ffenf.app.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.CompletableFuture;

@Service
public class FileCleanupService {
    
    @Value("${app.storage.upload-dir:uploads}")
    private String uploadDir;
    
    @Value("${app.storage.askhub-dir:askhub}")
    private String askhubDir;
    
    /**
     * Clean all uploaded files while keeping directory structure
     */
    public CompletableFuture<Void> cleanupAllFiles() {
        return CompletableFuture.runAsync(() -> {
            try {
                // Clean uploads directory
                cleanupDirectory(uploadDir);
                
                // Clean askhub directory  
                cleanupDirectory(askhubDir);
                
                System.out.println("File cleanup completed successfully");
                
            } catch (Exception e) {
                System.err.println("Error during file cleanup: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    private void cleanupDirectory(String dirPath) throws IOException {
        Path directory = Paths.get(dirPath);
        
        if (!Files.exists(directory)) {
            System.out.println("Directory " + dirPath + " does not exist, skipping");
            return;
        }
        
        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                try {
                    Files.delete(file);
                    System.out.println("Deleted file: " + file);
                } catch (IOException e) {
                    System.err.println("Failed to delete file " + file + ": " + e.getMessage());
                }
                return FileVisitResult.CONTINUE;
            }
            
            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                // Don't delete the root directories, just their contents
                if (!dir.equals(directory)) {
                    try {
                        Files.delete(dir);
                        System.out.println("Deleted directory: " + dir);
                    } catch (IOException e) {
                        System.err.println("Failed to delete directory " + dir + ": " + e.getMessage());
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });
        
        System.out.println("Cleaned directory: " + dirPath);
    }
}
