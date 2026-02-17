package com.spellfaire.spellfairebackend.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.spellfaire.spellfairebackend.auth.dto.AuthResponse;
import com.spellfaire.spellfairebackend.auth.dto.LoginRequest;
import com.spellfaire.spellfairebackend.auth.dto.RefreshResponse;
import com.spellfaire.spellfairebackend.auth.dto.RegisterRequest;
import com.spellfaire.spellfairebackend.auth.dto.UserResponse;
import com.spellfaire.spellfairebackend.auth.model.User;
import com.spellfaire.spellfairebackend.auth.repo.UserRepository;
import com.spellfaire.spellfairebackend.auth.service.AuthService;

import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
	private final AuthService authService;
	private final UserRepository userRepository;
	private final String refreshCookieName;
	private final boolean refreshCookieSecure;
	private final long refreshTtlSeconds;

	public AuthController(
			AuthService authService,
			UserRepository userRepository,
			@Value("${spellfaire.security.refresh.cookie-name}") String refreshCookieName,
			@Value("${spellfaire.security.refresh.cookie-secure}") boolean refreshCookieSecure,
			@Value("${spellfaire.security.refresh.ttl-seconds}") long refreshTtlSeconds) {
		this.authService = authService;
		this.userRepository = userRepository;
		this.refreshCookieName = refreshCookieName;
		this.refreshCookieSecure = refreshCookieSecure;
		this.refreshTtlSeconds = refreshTtlSeconds;
	}

	@PostMapping("/register")
	public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
		AuthService.AuthResult result = authService.register(request);
		return ResponseEntity.ok()
				.header(HttpHeaders.SET_COOKIE, buildRefreshCookie(result.refreshTokenValue()).toString())
				.body(result.response());
	}

	@PostMapping("/login")
	public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
		AuthService.AuthResult result = authService.login(request);
		return ResponseEntity.ok()
				.header(HttpHeaders.SET_COOKIE, buildRefreshCookie(result.refreshTokenValue()).toString())
				.body(result.response());
	}

	@PostMapping("/refresh")
	public ResponseEntity<RefreshResponse> refresh(HttpServletRequest request) {
		String refreshToken = getCookieValue(request, refreshCookieName);
		AuthService.AuthResult result = authService.refresh(refreshToken);
		return ResponseEntity.ok()
				.header(HttpHeaders.SET_COOKIE, buildRefreshCookie(result.refreshTokenValue()).toString())
				.body(new RefreshResponse(result.response().getAccessToken()));
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout(HttpServletRequest request) {
		String refreshToken = getCookieValue(request, refreshCookieName);
		authService.logout(refreshToken);
		return ResponseEntity.noContent()
				.header(HttpHeaders.SET_COOKIE, clearRefreshCookie().toString())
				.build();
	}

	@GetMapping("/me")
	public UserResponse me(Authentication authentication) {
		String userId = (String) authentication.getPrincipal();
		User user = userRepository.findById(UUID.fromString(userId))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
		return new UserResponse(user.getId().toString(), user.getEmail(), user.getUsername());
	}

	private static String getCookieValue(HttpServletRequest request, String name) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null) {
			return null;
		}
		for (Cookie cookie : cookies) {
			if (name.equals(cookie.getName())) {
				return cookie.getValue();
			}
		}
		return null;
	}

	private ResponseCookie buildRefreshCookie(String rawRefreshToken) {
		return ResponseCookie.from(refreshCookieName, rawRefreshToken)
				.httpOnly(true)
				.secure(refreshCookieSecure)
				.sameSite("Lax")
				.path("/api/auth")
				.maxAge(refreshTtlSeconds)
				.build();
	}

	private ResponseCookie clearRefreshCookie() {
		return ResponseCookie.from(refreshCookieName, "")
				.httpOnly(true)
				.secure(refreshCookieSecure)
				.sameSite("Lax")
				.path("/api/auth")
				.maxAge(0)
				.build();
	}
}
