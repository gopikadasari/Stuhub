package com.ffenf.app.auth;

import java.time.Instant;
import java.util.Date;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

	@Value("${app.jwt.secret:1B0D1704AA029EEB285A4B7857AEEF48FDC2A2782E33CD7375BF67FCAA5A138D}")
	private String secretBase64;

	@Value("${app.jwt.ttlSeconds:3600}")
	private long ttlSeconds;

	private javax.crypto.SecretKey getSigningKey() {
		System.out.println("JwtService - secretBase64: " + (secretBase64 != null ? secretBase64.substring(0, Math.min(10, secretBase64.length())) + "..." : "NULL"));
		System.out.println("JwtService - Environment APP_JWT_SECRET: " + System.getenv("APP_JWT_SECRET"));
		System.out.println("JwtService - Environment app.jwt.secret: " + System.getProperty("app.jwt.secret"));
		
		String jwtSecret = secretBase64;
		if (jwtSecret == null || jwtSecret.isEmpty()) {
			// Fallback to direct environment variable access
			jwtSecret = System.getenv("APP_JWT_SECRET");
			System.out.println("JwtService - Using environment variable: " + (jwtSecret != null ? jwtSecret.substring(0, Math.min(10, jwtSecret.length())) + "..." : "NULL"));
		}
		
		if (jwtSecret == null || jwtSecret.isEmpty()) {
			// Final fallback
			jwtSecret = "1B0D1704AA029EEB285A4B7857AEEF48FDC2A2782E33CD7375BF67FCAA5A138D";
			System.out.println("JwtService - Using hardcoded fallback");
		}
		
		byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
		return Keys.hmacShaKeyFor(keyBytes);
	}

	public String issueToken(String subject, Map<String, Object> claims) {
		Instant now = Instant.now();
		return Jwts.builder()
			.setSubject(subject)
			.addClaims(claims)
			.setIssuedAt(Date.from(now))
			.setExpiration(Date.from(now.plusSeconds(ttlSeconds)))
			.signWith(getSigningKey())
			.compact();
	}

	public Claims parse(String jwt) {
		return Jwts.parser()
			.verifyWith(getSigningKey())
			.build()
			.parseSignedClaims(jwt)
			.getPayload();
	}
}


