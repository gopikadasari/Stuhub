package com.ffenf.app.repo;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ffenf.app.domain.Answer;

@Repository
public interface AnswerRepository extends JpaRepository<Answer, UUID> {

	List<Answer> findByQuestionIdOrderByCreatedAtAsc(UUID questionId);

	List<Answer> findByUserIdOrderByCreatedAtDesc(UUID userId);

	List<Answer> findByIsAcceptedTrueAndQuestionId(UUID questionId);

	List<Answer> findByIsAcceptedFalseAndQuestionId(UUID questionId);

	@Query("SELECT a FROM Answer a WHERE a.questionId = :questionId " +
			"ORDER BY a.votesUp - a.votesDown DESC, a.createdAt ASC")
	List<Answer> findByQuestionIdOrderByVotes(@Param("questionId") UUID questionId);

	@Query("SELECT a FROM Answer a WHERE a.userId = :userId ORDER BY a.createdAt DESC")
	Page<Answer> findByUserIdOrderByCreatedAtDesc(@Param("userId") UUID userId, Pageable pageable);

	void deleteByQuestionId(UUID questionId);

	long countByQuestionId(UUID questionId);

	long countByUserId(UUID userId);
}
