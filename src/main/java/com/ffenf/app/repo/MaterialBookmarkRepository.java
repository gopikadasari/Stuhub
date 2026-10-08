package com.ffenf.app.repo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ffenf.app.domain.MaterialBookmark;

public interface MaterialBookmarkRepository extends JpaRepository<MaterialBookmark, UUID> {
    List<MaterialBookmark> findByUserId(UUID userId);
    Optional<MaterialBookmark> findByUserIdAndMaterialId(UUID userId, UUID materialId);
    boolean existsByUserIdAndMaterialId(UUID userId, UUID materialId);
    
    @Query("SELECT mb FROM MaterialBookmark mb JOIN mb.material m WHERE mb.user.id = :userId AND m.courseCode = :courseCode")
    List<MaterialBookmark> findByUserIdAndMaterialCourseCode(@Param("userId") UUID userId, @Param("courseCode") String courseCode);
}
