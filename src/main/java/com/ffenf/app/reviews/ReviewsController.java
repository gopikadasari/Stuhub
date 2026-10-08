package com.ffenf.app.reviews;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ffenf.app.domain.Material;
import com.ffenf.app.domain.Review;
import com.ffenf.app.domain.User;
import com.ffenf.app.repo.MaterialRepository;
import com.ffenf.app.repo.ReviewRepository;
import com.ffenf.app.repo.UserRepository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/reviews")
public class ReviewsController {

    private final ReviewRepository reviews;
    private final MaterialRepository materials;
    private final UserRepository users;

    public ReviewsController(ReviewRepository reviews, MaterialRepository materials, UserRepository users) {
        this.reviews = reviews;
        this.materials = materials;
        this.users = users;
    }

    public record ReviewRequest(@Min(1) @Max(5) int rating, String comment) {}

    @PostMapping("/{materialId}")
    public ResponseEntity<?> createReview(@PathVariable UUID materialId, 
                                        @Valid @RequestBody ReviewRequest req, 
                                        Authentication auth) {
        String email = auth.getName();
        User user = users.findByEmail(email).orElseThrow(() -> 
            new RuntimeException("User not found: " + email));
        
        Material material = materials.findById(materialId).orElse(null);
        if (material == null) {
            return ResponseEntity.notFound().build();
        }

        // Check if user already reviewed this material
        Review existingReview = reviews.findByUserIdAndMaterialId(user.getId(), materialId).orElse(null);
        
        Review review;
        if (existingReview != null) {
            // Update existing review
            existingReview.setRating(req.rating());
            existingReview.setComment(req.comment());
            review = reviews.save(existingReview);
        } else {
            // Create new review
            review = new Review();
            review.setUserId(user.getId());
            review.setMaterialId(materialId);
            review.setRating(req.rating());
            review.setComment(req.comment());
            review = reviews.save(review);
        }

        // Update material's average rating
        updateMaterialRating(materialId);

        Map<String, Object> response = new java.util.HashMap<>();
        response.put("reviewId", review.getId());
        response.put("rating", review.getRating());
        response.put("comment", review.getComment() != null ? review.getComment() : "");
        response.put("createdAt", review.getCreatedAt());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{materialId}")
    public ResponseEntity<?> getReviews(@PathVariable UUID materialId,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "10") int size) {
        
        List<Review> reviewList = reviews.findByMaterialId(materialId);
        
        List<Map<String, Object>> reviewsData = reviewList.stream()
            .map(r -> {
                User reviewer = users.findById(r.getUserId()).orElse(null);
                Map<String, Object> map = new java.util.HashMap<>();
                map.put("id", r.getId());
                map.put("rating", r.getRating());
                map.put("comment", r.getComment() != null ? r.getComment() : "");
                map.put("reviewerName", reviewer != null ? reviewer.getName() : "Anonymous");
                map.put("createdAt", r.getCreatedAt());
                return map;
            })
            .toList();

        Map<String, Object> response = new java.util.HashMap<>();
        response.put("reviews", reviewsData);
        response.put("totalReviews", reviewList.size());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{materialId}/my")
    public ResponseEntity<?> getMyReview(@PathVariable UUID materialId, Authentication auth) {
        String email = auth.getName();
        User user = users.findByEmail(email).orElseThrow();
        
        Review review = reviews.findByUserIdAndMaterialId(user.getId(), materialId).orElse(null);
        
        if (review == null) {
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("review", null);
            return ResponseEntity.ok(response);
        }

        Map<String, Object> reviewData = new java.util.HashMap<>();
        reviewData.put("id", review.getId());
        reviewData.put("rating", review.getRating());
        reviewData.put("comment", review.getComment() != null ? review.getComment() : "");
        reviewData.put("createdAt", review.getCreatedAt());
        
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("review", reviewData);
        return ResponseEntity.ok(response);
    }

    private void updateMaterialRating(UUID materialId) {
        List<Review> materialReviews = reviews.findByMaterialId(materialId);
        
        if (materialReviews.isEmpty()) {
            return;
        }

        double avgRating = materialReviews.stream()
            .mapToInt(Review::getRating)
            .average()
            .orElse(0.0);

        Material material = materials.findById(materialId).orElse(null);
        if (material != null) {
            material.setAvgRating(avgRating);
            material.setRatingsCount(materialReviews.size());
            materials.save(material);
        }
    }
}
