package com.ffenf.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

import com.ffenf.app.auth.JwtAuthFilter;
import com.ffenf.app.config.RequestLoggingFilter;

@Configuration
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthFilter jwtAuthFilter, RequestLoggingFilter requestLoggingFilter, CorsConfigurationSource corsConfigurationSource) throws Exception {
		http
			.csrf(csrf -> csrf.disable())
			.cors(cors -> cors.configurationSource(corsConfigurationSource))
			.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/index.html", "/static/**", "/assets/**").permitAll()
				.requestMatchers("/auth/**").permitAll()
					.requestMatchers("/health").permitAll()
					.requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/materials/search").permitAll()
                .requestMatchers("/materials/upload").permitAll()
                .requestMatchers("/materials/upload-new").permitAll()
                .requestMatchers("/materials/test-static").permitAll()
                .requestMatchers("/askhub/questions").permitAll()  // Allow reading questions without auth
				.requestMatchers("/askhub/questions/{id}").permitAll()
				.requestMatchers("/askhub/questions/search").permitAll()
				.requestMatchers("/askhub/generate").permitAll()
                .requestMatchers("/ai/debug/**").permitAll()  // Only debug endpoints are public
                .requestMatchers("/ai/**").authenticated()  // AI generation requires auth
                .requestMatchers("/admin/cleanup/**").permitAll()
                .anyRequest().permitAll()
			)
            .addFilterBefore(requestLoggingFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .httpBasic(AbstractHttpConfigurer::disable);
		return http.build();
	}

	@Bean
	AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
		return configuration.getAuthenticationManager();
	}
}


