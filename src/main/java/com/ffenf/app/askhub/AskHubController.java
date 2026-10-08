package com.ffenf.app.askhub;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ffenf.app.auth.JwtService;
import com.ffenf.app.domain.Answer;
import com.ffenf.app.domain.Question;
import com.ffenf.app.domain.User;
import com.ffenf.app.repo.AnswerRepository;
import com.ffenf.app.repo.QuestionRepository;
import com.ffenf.app.repo.UserRepository;
import com.ffenf.app.storage.FileStorageService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/askhub")
@RequiredArgsConstructor
public class AskHubController {
	
	private final QuestionRepository questionRepository;
	private final AnswerRepository answerRepository;
	private final FileStorageService fileStorageService;
	private final UserRepository userRepository;
	private final JwtService jwtService;
	
	private String extractTokenFromRequest(HttpServletRequest request) {
		String bearerToken = request.getHeader("Authorization");
		if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
			return bearerToken.substring(7);
		}
		return null;
	}

	// Question endpoints
	@GetMapping("/questions")
	public ResponseEntity<Page<Question>> getAllQuestions(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size,
			@RequestParam(defaultValue = "createdAt") String sortBy,
			@RequestParam(defaultValue = "desc") String sortDir) {
		
		Sort sort = sortDir.equalsIgnoreCase("desc") ? 
			Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
		Pageable pageable = PageRequest.of(page, size, sort);
		
		Page<Question> questions = questionRepository.findAll(pageable);
		return ResponseEntity.ok(questions);
	}

	// Debug endpoint to test basic question creation
	@PostMapping(path = "/debug-question", consumes = {MediaType.APPLICATION_JSON_VALUE})
	public ResponseEntity<String> debugQuestion(@RequestBody Map<String, String> request) {
		try {
			String token = request.get("token");
			if (token == null || token.isEmpty()) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No token provided");
			}
			
			// Extract email from JWT token
			String email = jwtService.parse(token).getSubject();
			User user = userRepository.findByEmail(email).orElse(null);
			if (user == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found for email: " + email);
			}
			
			return ResponseEntity.ok("Authentication successful for user: " + user.getName());
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
		}
	}

	@PostMapping(path = "/questions", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
	public ResponseEntity<Question> createQuestion(
			@RequestParam("title") String title,
			@RequestParam("description") String description,
			@RequestParam(value = "courseCode", required = false) String courseCode,
			@RequestParam(value = "subject", required = false) String subject,
			@RequestParam(value = "tags", required = false) String tags,
			@RequestParam(value = "priority", defaultValue = "NORMAL") String priority,
			@RequestParam(value = "image", required = false) MultipartFile image,
			HttpServletRequest request) {
		
		try {
			String token = extractTokenFromRequest(request);
			if (token == null || token.isEmpty()) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
			}
			
			// Extract email from JWT token
			String email = jwtService.parse(token).getSubject();
			User user = userRepository.findByEmail(email).orElseThrow(() -> 
				new RuntimeException("User not found: " + email));
			
			Question question = new Question();
			question.setUserId(user.getId());
			question.setTitle(title);
			question.setDescription(description);
			question.setCourseCode(courseCode);
			question.setSubject(subject);
			question.setTags(tags);
			question.setPriority(priority);
			
			// Handle image upload if provided
			if (image != null && !image.isEmpty()) {
				String fileName = fileStorageService.storeQuestionImage(image, user.getId());
				question.setStorageKey(fileName);
				question.setImageUrl("/askhub/images/" + fileName);
			}
			
			Question savedQuestion = questionRepository.save(question);
			
			// Award coins for asking good questions
			// This could be moved to a service layer for better organization
			
			return ResponseEntity.status(HttpStatus.CREATED).body(savedQuestion);
			
		} catch (Exception e) {
			System.err.println("Error creating question: " + e.getMessage());
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
		}
	}

	@GetMapping("/questions/{id}")
	public ResponseEntity<Map<String, Object>> getQuestion(@PathVariable UUID id) {
		Question question = questionRepository.findById(id).orElse(null);
		if (question == null) {
			return ResponseEntity.notFound().build();
		}
		
		// Increment view count
		question.setViewsCount(question.getViewsCount() + 1);
		questionRepository.save(question);
		
		// Get answers with user information
		List<Answer> answers = answerRepository.findByQuestionIdOrderByVotes(id);
		List<Map<String, Object>> answersWithUsers = answers.stream().map(answer -> {
			Map<String, Object> answerMap = new HashMap<>();
			answerMap.put("id", answer.getId());
			answerMap.put("content", answer.getContent());
			answerMap.put("createdAt", answer.getCreatedAt());
			answerMap.put("votesUp", answer.getVotesUp());
			answerMap.put("votesDown", answer.getVotesDown());
			answerMap.put("isAccepted", answer.isAccepted());
			
			// Get user information
			User user = userRepository.findById(answer.getUserId()).orElse(null);
			if (user != null) {
				answerMap.put("authorName", user.getName());
				answerMap.put("authorEmail", user.getEmail());
			} else {
				answerMap.put("authorName", "Unknown User");
				answerMap.put("authorEmail", "unknown@example.com");
			}
			
			return answerMap;
		}).toList();
		
		Map<String, Object> response = new HashMap<>();
		response.put("question", question);
		response.put("answers", answersWithUsers);
		
		return ResponseEntity.ok(response);
	}

	@GetMapping("/questions/search")
	public ResponseEntity<Page<Question>> searchQuestions(
			@RequestParam String query,
			@RequestParam(value = "courseCode", required = false) String courseCode,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size) {
		
		Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
		Page<Question> questions;
		
		if (courseCode != null && !courseCode.isEmpty()) {
			questions = questionRepository.searchByCourseAndKeyword(courseCode, query, pageable);
		} else {
			questions = questionRepository.searchByText(query, pageable);
		}
		
		return ResponseEntity.ok(questions);
	}

	@GetMapping("/questions/unanswered")
	public ResponseEntity<List<Question>> getUnansweredQuestions() {
		List<Question> questions = questionRepository.findUnansweredQuestions();
		return ResponseEntity.ok(questions);
	}

	// Answer endpoints
	@PostMapping(path = "/questions/{questionId}/answers", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
	public ResponseEntity<Map<String, Object>> createAnswer(
			@PathVariable UUID questionId,
			@RequestParam("content") String content,
			@RequestParam(value = "image", required = false) MultipartFile image,
			@RequestParam(value = "displayName", defaultValue = "Anonymous") String displayName,
			HttpServletRequest request) {
		
		try {
			String token = extractTokenFromRequest(request);
			if (token == null || token.isEmpty()) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
			}
			
			// Extract email from JWT token
			String email = jwtService.parse(token).getSubject();
			User user = userRepository.findByEmail(email).orElseThrow(() -> 
				new RuntimeException("User not found: " + email));
			
			Question question = questionRepository.findById(questionId).orElse(null);
			if (question == null) {
				return ResponseEntity.notFound().build();
			}
			
			Answer answer = new Answer();
			answer.setQuestionId(questionId);
			answer.setUserId(user.getId());
			answer.setContent(content);
			
			// Handle image upload if provided
			if (image != null && !image.isEmpty()) {
				String fileName = fileStorageService.storeAnswerImage(image, user.getId());
				answer.setStorageKey(fileName);
				answer.setImageUrl("/askhub/images/" + fileName);
			}
			
			Answer savedAnswer = answerRepository.save(answer);
			
			// Update question answer count
			question.setAnswersCount(question.getAnswersCount() + 1);
			questionRepository.save(question);
			
			// Return answer with display name
			Map<String, Object> response = new HashMap<>();
			response.put("id", savedAnswer.getId());
			response.put("content", savedAnswer.getContent());
			response.put("createdAt", savedAnswer.getCreatedAt());
			response.put("authorName", displayName);
			
			return ResponseEntity.status(HttpStatus.CREATED).body(response);
			
		} catch (Exception e) {
			System.err.println("Error creating answer: " + e.getMessage());
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
		}
	}

	@PostMapping("/answers/{answerId}/accept")
	public ResponseEntity<Void> acceptAnswer(@PathVariable UUID answerId, HttpServletRequest request) {
		try {
			Authentication auth = (Authentication) request.getAttribute("auth");
			UUID userId = (UUID) auth.getPrincipal();
			
			Answer answer = answerRepository.findById(answerId).orElse(null);
			if (answer == null) {
				return ResponseEntity.notFound().build();
			}
			
			Question question = questionRepository.findById(answer.getQuestionId()).orElse(null);
			if (question == null || !question.getUserId().equals(userId)) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
			}
			
			// Unaccept any previously accepted answers for this question
			List<Answer> existingAnswers = answerRepository.findByIsAcceptedTrueAndQuestionId(answer.getQuestionId());
			for (Answer existing : existingAnswers) {
				existing.setAccepted(false);
				answerRepository.save(existing);
			}
			
			// Accept this answer
			answer.setAccepted(true);
			answerRepository.save(answer);
			
			// Close the question
			question.setStatus("RESOLVED");
			questionRepository.save(question);
			
			return ResponseEntity.ok().build();
			
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
		}
	}

	@PostMapping("/answers/{answerId}/vote-up")
	public ResponseEntity<Void> voteUpAnswer(@PathVariable UUID answerId, HttpServletRequest request) {
		return voteAnswer(answerId, true, request);
	}

	@PostMapping("/answers/{answerId}/vote-down")
	public ResponseEntity<Void> voteDownAnswer(@PathVariable UUID answerId, HttpServletRequest request) {
		return voteAnswer(answerId, false, request);
	}

	private ResponseEntity<Void> voteAnswer(UUID answerId, boolean upVote, HttpServletRequest request) {
		try {
			Authentication auth = (Authentication) request.getAttribute("auth");
			UUID userId = (UUID) auth.getPrincipal();
			
			Answer answer = answerRepository.findById(answerId).orElse(null);
			if (answer == null) {
				return ResponseEntity.notFound().build();
			}
			
			// Don't allow voting on own answers
			if (answer.getUserId().equals(userId)) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
			}
			
			if (upVote) {
				answer.setVotesUp(answer.getVotesUp() + 1);
			} else {
				answer.setVotesDown(answer.getVotesDown() + 1);
			}
			
			answerRepository.save(answer);
			return ResponseEntity.ok().build();
			
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
		}
	}

	@GetMapping("/questions/course/{courseCode}")
	public ResponseEntity<List<Question>> getQuestionsByCourse(@PathVariable String courseCode) {
		List<Question> questions = questionRepository.findByCourseCodeOrderByCreatedAtDesc(courseCode);
		return ResponseEntity.ok(questions);
	}

	@GetMapping("/questions/user/{userId}")
	public ResponseEntity<List<Question>> getUserQuestions(@PathVariable UUID userId) {
		List<Question> questions = questionRepository.findByUserIdOrderByCreatedAtDesc(userId);
		return ResponseEntity.ok(questions);
	}

	@GetMapping("/images/**")
	public ResponseEntity<byte[]> getImage(HttpServletRequest request) {
		try {
			String requestPath = request.getRequestURI();
			String imagePath = requestPath.substring(requestPath.indexOf("/images/") + 8); // Remove "/images/"
			
			System.err.println("Requesting image path: " + imagePath);
			byte[] imageBytes = fileStorageService.getImage(imagePath);
			String contentType = fileStorageService.getImageContentType(imagePath);
			
			return ResponseEntity.ok()
				.contentType(MediaType.valueOf(contentType))
				.body(imageBytes);
				
		} catch (Exception e) {
			System.err.println("Error serving image: " + e.getMessage());
			e.printStackTrace();
			return ResponseEntity.notFound().build();
		}
	}
}
