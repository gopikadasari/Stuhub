package com.ffenf.app.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "coin_transactions")
@Getter
@Setter
@NoArgsConstructor
public class CoinTransaction {

	@Id
	private UUID id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(nullable = false)
	private int delta;

	@Column(nullable = false, length = 64)
	private String reason;

	@Column(name = "ref_id")
	private UUID refId;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@PrePersist
	void prePersist() {
		if (id == null) id = UUID.randomUUID();
		createdAt = Instant.now();
	}
}


