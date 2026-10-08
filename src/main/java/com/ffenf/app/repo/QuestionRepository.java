package com.ffenf.app.repo;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ffenf.app.domain.Question;

@Repository
public interface QuestionRepository extends JpaRepository<Question, UUID> {

	List<Question> findByUserIdOrderByCreatedAtDesc(UUID userId);

	List<Question> findByCourseCodeOrderByCreatedAtDesc(String courseCode);

	List<Question> findBySubjectOrderByCreatedAtDesc(String subject);

	@Query("SELECT q FROM Question q WHERE q.status = :status ORDER BY q.createdAt DESC")
	Page<Question> findByStatusOrderByCreatedAtDesc(@Param("status") String status, Pageable pageable);

	@Query("SELECT q FROM Question q WHERE " +
			"(LOWER(q.title) LIKE %:query% OR q.description LIKE %:query% OR LOWER(q.tags) LIKE %:query%) " +
			"ORDER BY q.createdAt DESC")
	Page<Question> searchByText(@Param("query") String query, Pageable pageable);

	@Query("SELECT q FROM Question q WHERE " +
			"(LOWER(q.title) LIKE %:keyword% OR q.description LIKE %:keyword%) " +
			"ORDER BY q.createdAt DESC")
	Page<Question> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

	@Query("SELECT q FROM Question q WHERE " +
			"q.courseCode = :courseCode AND " +
			"(LOWER(q.title) LIKE %:keyword% OR q.description LIKE %:keyword%) " +
			"ORDER BY q.createdAt DESC")
	Page<Question> searchByCourseAndKeyword(@Param("courseCode") String courseCode, 
										   @Param("keyword") String keyword, 
										   Pageable pageable);

	List<Question> findByPriorityOrderByCreatedAtDesc(String priority);

	@Query("SELECT q FROM Question q WHERE q.answersCount > 0 ORDER BY q.createdAt DESC")
	List<Question> findQuestionsWithAnswers();

	@Query("SELECT q FROM Question q WHERE q.answersCount = 0 ORDER BY q.createdAt DESC")
	List<Question> findUnansweredQuestions();

	long countByCourseCode(String courseCode);

	long countBySubject(String subject);

	long countByStatus(String status);
}
