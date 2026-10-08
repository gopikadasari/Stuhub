package com.ffenf.app.repo;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.ffenf.app.domain.Material;

public interface MaterialRepository extends JpaRepository<Material, UUID> {
    Page<Material> findByTitleContainingIgnoreCaseOrSubjectContainingIgnoreCaseOrCourseCodeContainingIgnoreCase(
        String title, String subject, String courseCode, Pageable pageable);
    Page<Material> findByUserId(UUID userId, Pageable pageable);
    long countByUserId(UUID userId);
}
