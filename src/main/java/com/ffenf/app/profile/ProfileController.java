package com.ffenf.app.profile;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ffenf.app.domain.CourseBookmark;
import com.ffenf.app.domain.Material;
import com.ffenf.app.domain.MaterialBookmark;
import com.ffenf.app.domain.Review;
import com.ffenf.app.domain.User;
import com.ffenf.app.repo.AnswerRepository;
import com.ffenf.app.repo.CourseBookmarkRepository;
import com.ffenf.app.repo.MaterialBookmarkRepository;
import com.ffenf.app.repo.MaterialRepository;
import com.ffenf.app.repo.ReviewRepository;
import com.ffenf.app.repo.UserRepository;

@RestController
@RequestMapping("/profile")
public class ProfileController {

    private final UserRepository users;
    private final MaterialRepository materials;
    private final ReviewRepository reviews;
    private final AnswerRepository answers;
    private final CourseBookmarkRepository courseBookmarks;
    private final MaterialBookmarkRepository materialBookmarks;

    public ProfileController(UserRepository users, MaterialRepository materials, ReviewRepository reviews, AnswerRepository answers, CourseBookmarkRepository courseBookmarks, MaterialBookmarkRepository materialBookmarks) {
        this.users = users;
        this.materials = materials;
        this.reviews = reviews;
        this.answers = answers;
        this.courseBookmarks = courseBookmarks;
        this.materialBookmarks = materialBookmarks;
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMyProfile(Authentication auth) {
        String email = auth.getName();
        User user = users.findByEmail(email).orElseThrow(() -> 
            new RuntimeException("User not found: " + email));

        // Get real counts
        long totalUploads = materials.countByUserId(user.getId());
        long totalReviews = reviews.countByUserId(user.getId()); // Count material reviews given by user

        Map<String, Object> response = new java.util.HashMap<>();
        response.put("id", user.getId());
        response.put("email", user.getEmail());
        response.put("name", user.getName() != null ? user.getName() : "Test User");
        // Always read fresh coins from DB
        response.put("coins", users.findById(user.getId()).map(User::getCoins).orElse(user.getCoins()));
        response.put("uploadsCount", totalUploads);
        response.put("reviewsCount", totalReviews);
        response.put("createdAt", user.getCreatedAt());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my-uploads")
    public ResponseEntity<?> getMyUploads(Authentication auth,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "10") int size) {
        String email = auth.getName();
        User user = users.findByEmail(email).orElseThrow();

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Material> userMaterials = materials.findByUserId(user.getId(), pageable);

        List<Map<String, Object>> materialsList = userMaterials.getContent().stream()
            .map(m -> {
                Map<String, Object> map = new java.util.HashMap<>();
                map.put("id", m.getId());
                map.put("title", m.getTitle());
                map.put("subject", m.getSubject() != null ? m.getSubject() : "");
                map.put("courseCode", m.getCourseCode() != null ? m.getCourseCode() : "");
                map.put("avgRating", m.getAvgRating());
                map.put("ratingsCount", m.getRatingsCount());
                map.put("downloadsCount", m.getDownloadsCount());
                map.put("status", m.getStatus());
                map.put("createdAt", m.getCreatedAt());
                return map;
            })
            .toList();

        Map<String, Object> response = new java.util.HashMap<>();
        response.put("materials", materialsList);
        response.put("totalElements", userMaterials.getTotalElements());
        response.put("totalPages", userMaterials.getTotalPages());
        response.put("currentPage", page);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my-reviews")
    public ResponseEntity<?> getMyReviews(Authentication auth,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "10") int size) {
        String email = auth.getName();
        User user = users.findByEmail(email).orElseThrow();

        List<Review> userReviews = reviews.findByUserId(user.getId());

        List<Map<String, Object>> reviewsList = userReviews.stream()
            .map(r -> {
                Material material = materials.findById(r.getMaterialId()).orElse(null);
                Map<String, Object> map = new java.util.HashMap<>();
                map.put("id", r.getId());
                map.put("rating", r.getRating());
                map.put("comment", r.getComment() != null ? r.getComment() : "");
                map.put("materialTitle", material != null ? material.getTitle() : "Unknown Material");
                map.put("materialId", r.getMaterialId());
                map.put("createdAt", r.getCreatedAt());
                return map;
            })
            .toList();

        Map<String, Object> response = new java.util.HashMap<>();
        response.put("reviews", reviewsList);
        response.put("totalReviews", reviewsList.size());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getMyStats(Authentication auth) {
        String email = auth.getName();
        User user = users.findByEmail(email).orElseThrow();

        long totalUploads = materials.countByUserId(user.getId());
        long totalReviews = reviews.countByUserId(user.getId());
        long totalDownloads = materials.findByUserId(user.getId(), Pageable.unpaged())
            .getContent().stream()
            .mapToLong(Material::getDownloadsCount)
            .sum();

        Map<String, Object> response = new java.util.HashMap<>();
        response.put("coins", user.getCoins());
        response.put("totalUploads", totalUploads);
        response.put("totalReviews", totalReviews);
        response.put("totalDownloads", totalDownloads);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me/courses")
    public ResponseEntity<List<CourseSummaryResponse>> getMyBookmarkedCourses(Authentication auth) {
        String email = auth.getName();
        User user = users.findByEmail(email).orElseThrow();

        // Get all bookmarked courses for the user
        List<CourseBookmark> bookmarks = courseBookmarks.findByUserId(user.getId());
        
        List<CourseSummaryResponse> courses = bookmarks.stream()
            .map(bookmark -> {
                CourseSummaryResponse course = new CourseSummaryResponse();
                course.setCourseCode(bookmark.getCourseCode());
                
                // Count total materials for this course code across all users
                long materialCount = materials.findAll().stream()
                    .filter(material -> bookmark.getCourseCode().equals(material.getCourseCode()))
                    .count();
                
                course.setTotalMaterials((int) materialCount);
                return course;
            })
            .sorted((a, b) -> a.getCourseCode().compareTo(b.getCourseCode()))
            .collect(Collectors.toList());

        return ResponseEntity.ok(courses);
    }

    @PostMapping("/me/courses/bookmark")
    public ResponseEntity<Map<String, Object>> bookmarkCourse(
            @RequestParam String courseCode,
            Authentication auth) {
        String email = auth.getName();
        User user = users.findByEmail(email).orElseThrow();

        // Check if already bookmarked
        if (courseBookmarks.existsByUserIdAndCourseCode(user.getId(), courseCode)) {
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("message", "Course already bookmarked");
            response.put("bookmarked", true);
            return ResponseEntity.ok(response);
        }

        // Create new bookmark
        CourseBookmark bookmark = new CourseBookmark();
        bookmark.setUser(user);
        bookmark.setCourseCode(courseCode);
        courseBookmarks.save(bookmark);

        Map<String, Object> response = new java.util.HashMap<>();
        response.put("message", "Course bookmarked successfully");
        response.put("bookmarked", true);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/me/courses/unbookmark")
    public ResponseEntity<Map<String, Object>> unbookmarkCourse(
            @RequestParam String courseCode,
            Authentication auth) {
        String email = auth.getName();
        User user = users.findByEmail(email).orElseThrow();

        // Find and delete bookmark
        Optional<CourseBookmark> bookmark = courseBookmarks.findByUserIdAndCourseCode(user.getId(), courseCode);
        if (bookmark.isPresent()) {
            courseBookmarks.delete(bookmark.get());
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("message", "Course unbookmarked successfully");
            response.put("bookmarked", false);
            return ResponseEntity.ok(response);
        } else {
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("message", "Course was not bookmarked");
            response.put("bookmarked", false);
            return ResponseEntity.ok(response);
        }
    }

    @GetMapping("/me/courses/{courseCode}/materials")
    public ResponseEntity<Map<String, Object>> getCourseMaterials(
            @PathVariable String courseCode,
            Authentication auth) {
        String email = auth.getName();
        User user = users.findByEmail(email).orElseThrow();

        // Get bookmarked materials for this course
        List<MaterialBookmark> bookmarkedMaterials = materialBookmarks.findByUserIdAndMaterialCourseCode(user.getId(), courseCode);
        
        // Get all materials for this course code (recommended)
        List<Material> allCourseMaterials = materials.findAll().stream()
            .filter(material -> courseCode.equals(material.getCourseCode()))
            .collect(Collectors.toList());

        // Separate bookmarked and recommended materials
        List<Map<String, Object>> bookmarkedMaterialsList = bookmarkedMaterials.stream()
            .map(mb -> {
                Material m = mb.getMaterial();
                Map<String, Object> map = new java.util.HashMap<>();
                map.put("id", m.getId());
                map.put("title", m.getTitle());
                map.put("subject", m.getSubject() != null ? m.getSubject() : "");
                map.put("courseCode", m.getCourseCode() != null ? m.getCourseCode() : "");
                map.put("avgRating", m.getAvgRating());
                map.put("ratingsCount", m.getRatingsCount());
                map.put("downloadsCount", m.getDownloadsCount());
                map.put("createdAt", m.getCreatedAt());
                map.put("isBookmarked", true);
                map.put("bookmarkedAt", mb.getCreatedAt());
                
                // Fetch user details
                users.findById(m.getUserId()).ifPresent(materialUser -> {
                    map.put("user", Map.of(
                        "id", materialUser.getId(),
                        "name", materialUser.getName()
                    ));
                });
                
                return map;
            })
            .toList();

        // Get recommended materials (not bookmarked by user)
        List<UUID> bookmarkedMaterialIds = bookmarkedMaterials.stream()
            .map(mb -> mb.getMaterial().getId())
            .collect(Collectors.toList());

        List<Map<String, Object>> recommendedMaterialsList = allCourseMaterials.stream()
            .filter(material -> !bookmarkedMaterialIds.contains(material.getId()))
            .map(m -> {
                Map<String, Object> map = new java.util.HashMap<>();
                map.put("id", m.getId());
                map.put("title", m.getTitle());
                map.put("subject", m.getSubject() != null ? m.getSubject() : "");
                map.put("courseCode", m.getCourseCode() != null ? m.getCourseCode() : "");
                map.put("avgRating", m.getAvgRating());
                map.put("ratingsCount", m.getRatingsCount());
                map.put("downloadsCount", m.getDownloadsCount());
                map.put("createdAt", m.getCreatedAt());
                map.put("isBookmarked", false);
                
                // Fetch user details
                users.findById(m.getUserId()).ifPresent(materialUser -> {
                    map.put("user", Map.of(
                        "id", materialUser.getId(),
                        "name", materialUser.getName()
                    ));
                });
                
                return map;
            })
            .toList();

        Map<String, Object> response = new java.util.HashMap<>();
        response.put("courseCode", courseCode);
        response.put("bookmarkedMaterials", bookmarkedMaterialsList);
        response.put("recommendedMaterials", recommendedMaterialsList);
        response.put("totalBookmarked", bookmarkedMaterialsList.size());
        response.put("totalRecommended", recommendedMaterialsList.size());
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/me/materials/{materialId}/bookmark")
    public ResponseEntity<Map<String, Object>> bookmarkMaterial(
            @PathVariable UUID materialId,
            Authentication auth) {
        String email = auth.getName();
        User user = users.findByEmail(email).orElseThrow();

        // Check if material exists
        Optional<Material> materialOpt = materials.findById(materialId);
        if (materialOpt.isEmpty()) {
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("message", "Material not found");
            response.put("bookmarked", false);
            return ResponseEntity.notFound().build();
        }

        // Check if already bookmarked
        if (materialBookmarks.existsByUserIdAndMaterialId(user.getId(), materialId)) {
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("message", "Material already bookmarked");
            response.put("bookmarked", true);
            return ResponseEntity.ok(response);
        }

        // Create new bookmark
        MaterialBookmark bookmark = new MaterialBookmark();
        bookmark.setUser(user);
        bookmark.setMaterial(materialOpt.get());
        materialBookmarks.save(bookmark);

        Map<String, Object> response = new java.util.HashMap<>();
        response.put("message", "Material bookmarked successfully");
        response.put("bookmarked", true);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/me/materials/{materialId}/unbookmark")
    public ResponseEntity<Map<String, Object>> unbookmarkMaterial(
            @PathVariable UUID materialId,
            Authentication auth) {
        String email = auth.getName();
        User user = users.findByEmail(email).orElseThrow();

        // Find and delete bookmark
        Optional<MaterialBookmark> bookmark = materialBookmarks.findByUserIdAndMaterialId(user.getId(), materialId);
        if (bookmark.isPresent()) {
            materialBookmarks.delete(bookmark.get());
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("message", "Material unbookmarked successfully");
            response.put("bookmarked", false);
            return ResponseEntity.ok(response);
        } else {
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("message", "Material was not bookmarked");
            response.put("bookmarked", false);
            return ResponseEntity.ok(response);
        }
    }

    @GetMapping("/me/materials")
    public ResponseEntity<List<Map<String, Object>>> getMyBookmarkedMaterials(Authentication auth) {
        String email = auth.getName();
        User user = users.findByEmail(email).orElseThrow();

        // Get all bookmarked materials for the user
        List<MaterialBookmark> bookmarks = materialBookmarks.findByUserId(user.getId());
        
        List<Map<String, Object>> materials = bookmarks.stream()
            .map(mb -> {
                Material m = mb.getMaterial();
                Map<String, Object> map = new java.util.HashMap<>();
                map.put("id", m.getId());
                map.put("title", m.getTitle());
                map.put("subject", m.getSubject() != null ? m.getSubject() : "");
                map.put("courseCode", m.getCourseCode() != null ? m.getCourseCode() : "");
                map.put("avgRating", m.getAvgRating());
                map.put("ratingsCount", m.getRatingsCount());
                map.put("downloadsCount", m.getDownloadsCount());
                map.put("createdAt", m.getCreatedAt());
                map.put("bookmarkedAt", mb.getCreatedAt());
                
                // Fetch user details
                users.findById(m.getUserId()).ifPresent(materialUser -> {
                    map.put("user", Map.of(
                        "id", materialUser.getId(),
                        "name", materialUser.getName()
                    ));
                });
                
                // Since these are bookmarked materials, set isBookmarked to true
                map.put("isBookmarked", true);
                
                return map;
            })
            .sorted((a, b) -> ((java.time.LocalDateTime) b.get("bookmarkedAt"))
                .compareTo((java.time.LocalDateTime) a.get("bookmarkedAt")))
            .toList();

        return ResponseEntity.ok(materials);
    }

    public static class CourseSummaryResponse {
        private String courseCode;
        private int totalMaterials;

        public String getCourseCode() {
            return courseCode;
        }

        public void setCourseCode(String courseCode) {
            this.courseCode = courseCode;
        }

        public int getTotalMaterials() {
            return totalMaterials;
        }

        public void setTotalMaterials(int totalMaterials) {
            this.totalMaterials = totalMaterials;
        }
    }

}
