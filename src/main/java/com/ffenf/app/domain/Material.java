package com.ffenf.app.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "materials")
@Getter
@Setter
@NoArgsConstructor
public class Material {

	@Id
	private UUID id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(nullable = false, length = 255)
	private String title;

	@Column(length = 255)
	private String subject;

	@Column(name = "course_code", length = 64)
	private String courseCode;

	@Column(length = 1024)
	private String tags; // comma-separated

	@Column(name = "storage_key", nullable = false, length = 512)
	private String storageKey;

	@Column(name = "file_size")
	private Long fileSize;

	@Column(name = "mime_type", length = 128)
	private String mimeType;

	@Column(name = "text_extract", columnDefinition = "TEXT")
	private String textExtract;

	@Column(columnDefinition = "TEXT")
	private String summary;

	@Column(name = "flashcards_json", columnDefinition = "TEXT")
	private String flashcardsJson;

	@Column(name = "avg_rating", nullable = false)
	private double avgRating = 0;

	@Column(name = "ratings_count", nullable = false)
	private int ratingsCount = 0;

	@Column(name = "downloads_count", nullable = false)
	private int downloadsCount = 0;

	@Column(nullable = false, length = 32)
	private String status;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@PrePersist
	void prePersist() {
		if (id == null) id = UUID.randomUUID();
		Instant now = Instant.now();
		createdAt = now;
		updatedAt = now;
	}

	@PreUpdate
	void preUpdate() {
		updatedAt = Instant.now();
	}
}


