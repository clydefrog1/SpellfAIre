package com.spellfaire.spellfairebackend.auth;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.spellfaire.spellfairebackend.auth.dto.UpdateProfileRequest;
import com.spellfaire.spellfairebackend.auth.dto.UserResponse;
import com.spellfaire.spellfairebackend.auth.service.AuthService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
public class UserController {

	private final AuthService authService;

	public UserController(AuthService authService) {
		this.authService = authService;
	}

	@PatchMapping("/me")
	public ResponseEntity<UserResponse> updateProfile(
			Authentication authentication,
			@Valid @RequestBody UpdateProfileRequest request) {
		UUID userId = UUID.fromString((String) authentication.getPrincipal());
		UserResponse updated = authService.updateProfile(userId, request);
		return ResponseEntity.ok(updated);
	}
}
