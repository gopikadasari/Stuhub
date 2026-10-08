package com.ffenf.app.repo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ffenf.app.domain.CourseBookmark;

public interface CourseBookmarkRepository extends JpaRepository<CourseBookmark, UUID> {
    Optional<CourseBookmark> findByUserIdAndCourseCode(UUID userId, String courseCode);
    List<CourseBookmark> findByUserId(UUID userId);
    long countByUserId(UUID userId);
    boolean existsByUserIdAndCourseCode(UUID userId, String courseCode);
}
