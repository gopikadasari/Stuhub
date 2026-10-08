package com.ffenf.app.repo;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ffenf.app.domain.AiJob;

public interface AiJobRepository extends JpaRepository<AiJob, UUID> {
    Optional<AiJob> findByMaterialIdAndTypeAndStatus(UUID materialId, String type, String status);
}
