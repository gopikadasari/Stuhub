package com.ffenf.app.repo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ffenf.app.domain.Review;

public interface ReviewRepository extends JpaRepository<Review, UUID> {
    Optional<Review> findByUserIdAndMaterialId(UUID userId, UUID materialId);
    List<Review> findByMaterialId(UUID materialId);
    List<Review> findByUserId(UUID userId);
    long countByUserId(UUID userId);
}
