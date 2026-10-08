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
@Table(name = "answers")
@Getter
@Setter
@NoArgsConstructor
public class Answer {

	@Id
	private UUID id;

	@Column(name = "question_id", nullable = false)
	private UUID questionId;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String content;

	@Column(name = "image_url", length = 512)
	private String imageUrl; // URL to uploaded image

	@Column(name = "storage_key", length = 512)
	private String storageKey; // Storage key for the answer image

	@Column(name = "is_accepted", nullable = false)
	private boolean isAccepted = false;

	@Column(name = "votes_up", nullable = false)
	private int votesUp = 0;

	@Column(name = "votes_down", nullable = false)
	private int votesDown = 0;

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
