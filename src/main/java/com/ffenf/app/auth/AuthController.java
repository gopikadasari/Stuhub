package com.ffenf.app.auth;

import com.ffenf.app.domain.User;
import com.ffenf.app.repo.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
public class AuthController {

	private final UserRepository users;
	private final JwtService jwtService;
	private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

	public AuthController(UserRepository users, JwtService jwtService) {
		this.users = users;
		this.jwtService = jwtService;
	}

	public record RegisterRequest(@Email String email, String name, @NotBlank String password) {}
	public record LoginRequest(@Email String email, @NotBlank String password) {}

	@PostMapping("/register")
	public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
		if (users.findByEmail(req.email()).isPresent()) {
			return ResponseEntity.badRequest().body(Map.of("error", "Email already registered"));
		}
		User u = new User();
		u.setId(UUID.randomUUID());
		u.setEmail(req.email());
		u.setName(req.name());
		u.setPasswordHash(passwordEncoder.encode(req.password()));
		u.setRole("USER");
		u = users.save(u);
		String token = jwtService.issueToken(u.getEmail(), Map.of("role", u.getRole(), "uid", u.getId().toString()));
		return ResponseEntity.ok(Map.of("token", token));
	}

	@PostMapping("/login")
	public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
		User u = users.findByEmail(req.email()).orElse(null);
		if (u == null || u.getPasswordHash() == null || !passwordEncoder.matches(req.password(), u.getPasswordHash())) {
			return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
		}
		String token = jwtService.issueToken(u.getEmail(), Map.of("role", u.getRole(), "uid", u.getId().toString()));
		return ResponseEntity.ok(Map.of("token", token));
	}
}


