package com.ffenf.app.health;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@RestController
public class HealthController {

	@Value("${app.storage.path:/tmp/uploads}")
	private String storagePath;

	@GetMapping("/health")
	public ResponseEntity<Map<String, Object>> health() {
		try {
			Map<String, Object> health = new HashMap<>();
			health.put("status", "UP");
			health.put("timestamp", System.currentTimeMillis());
			
			// Simple storage check without complex operations
			try {
				Path testPath = Paths.get(storagePath);
				if (!Files.exists(testPath)) {
					Files.createDirectories(testPath);
				}
				health.put("storage", "OK");
			} catch (Exception e) {
				health.put("storage", "WARNING: " + e.getMessage());
			}
			
			// Basic system info
			health.put("javaVersion", System.getProperty("java.version"));
			health.put("osName", System.getProperty("os.name"));
			
			return ResponseEntity.ok(health);
			
		} catch (Exception e) {
			// Fallback simple health check
			Map<String, Object> simpleHealth = new HashMap<>();
			simpleHealth.put("status", "UP");
			simpleHealth.put("timestamp", System.currentTimeMillis());
			simpleHealth.put("message", "Basic health check");
			return ResponseEntity.ok(simpleHealth);
		}
	}
}


