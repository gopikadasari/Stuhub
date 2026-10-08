package com.ffenf.app.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reviews",
		uniqueConstraints = @UniqueConstraint(name = "uk_review_user_material", columnNames = {"user_id", "material_id"})
)
@Getter
@Setter
@NoArgsConstructor
public class Review {

	@Id
	private UUID id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(name = "material_id", nullable = false)
	private UUID materialId;

	@Column(nullable = false)
	private int rating;

	@Column(columnDefinition = "TEXT")
	private String comment;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@PrePersist
	void prePersist() {
		if (id == null) id = UUID.randomUUID();
		createdAt = Instant.now();
	}
}


