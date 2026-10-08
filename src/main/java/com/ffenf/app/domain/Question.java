package com.ffenf.app.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "questions")
@Getter
@Setter
@NoArgsConstructor
public class Question {

	@Id
	private UUID id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(nullable = false, length = 500)
	private String title;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String description;

	@Column(name = "course_code", length = 64)
	private String courseCode;

	@Column(name = "subject", length = 255)
	private String subject;

	@Column(name = "tags", length = 1024)
	private String tags; // comma-separated

	@Column(name = "image_url", length = 512)
	private String imageUrl; // URL to uploaded image

	@Column(name = "storage_key", length = 512)
	private String storageKey; // Storage key for the question image

	@Column(name = "status", nullable = false, length = 32)
	private String status = "OPEN"; // OPEN, CLOSED, RESOLVED

	@Column(name = "answers_count", nullable = false)
	private int answersCount = 0;

	@Column(name = "views_count", nullable = false)
	private int viewsCount = 0;

	@Column(name = "priority", length = 32)
	private String priority = "NORMAL"; // LOW, NORMAL, HIGH, URGENT

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
