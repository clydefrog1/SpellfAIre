package com.spellfaire.spellfairebackend.auth.service;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import com.spellfaire.spellfairebackend.auth.dto.AuthResponse;
import com.spellfaire.spellfairebackend.auth.dto.LoginRequest;
import com.spellfaire.spellfairebackend.auth.dto.RegisterRequest;
import com.spellfaire.spellfairebackend.auth.dto.UpdateProfileRequest;
import com.spellfaire.spellfairebackend.auth.dto.UserResponse;
import com.spellfaire.spellfairebackend.auth.model.User;
import com.spellfaire.spellfairebackend.auth.repo.UserRepository;

@Service
public class AuthService {
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final RefreshTokenService refreshTokenService;

	public AuthService(
			UserRepository userRepository,
			PasswordEncoder passwordEncoder,
			JwtService jwtService,
			RefreshTokenService refreshTokenService) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
		this.refreshTokenService = refreshTokenService;
	}

	public AuthResult register(RegisterRequest request) {
		String email = request.getEmail().trim().toLowerCase();
		if (userRepository.existsByEmail(email)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
		}

		User user = new User();
		user.setEmail(email);
		user.setUsername(request.getUsername().trim());
		user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
		user.setCreatedAt(Instant.now());
		userRepository.save(user);

		return issueTokens(user);
	}

	public AuthResult login(LoginRequest request) {
		String email = request.getEmail().trim().toLowerCase();
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

		if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
		}

		return issueTokens(user);
	}

	public AuthResult refresh(String rawRefreshToken) {
		if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing refresh token");
		}

		RefreshTokenService.IssuedRefreshToken rotated = refreshTokenService.rotate(rawRefreshToken);
		User user = rotated.persisted().getUser();

		String accessToken = jwtService.createAccessToken(user);
		return new AuthResult(new AuthResponse(accessToken, toUserResponse(user)), rotated.rawToken());
	}

	public void logout(String rawRefreshToken) {
		refreshTokenService.revokeIfPresent(rawRefreshToken);
	}

	public UserResponse updateProfile(UUID userId, UpdateProfileRequest request) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

		if (request.getUsername() != null && !request.getUsername().isBlank()) {
			user.setUsername(request.getUsername().trim());
		}

		if (request.getNewPassword() != null && !request.getNewPassword().isBlank()) {
			if (request.getCurrentPassword() == null
					|| !passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
			}
			user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
		}

		if (request.getAvatarBase64() != null) {
			user.setAvatarBase64(request.getAvatarBase64());
		}

		userRepository.save(user);
		return toUserResponse(user);
	}

	private AuthResult issueTokens(User user) {
		String accessToken = jwtService.createAccessToken(user);
		RefreshTokenService.IssuedRefreshToken refresh = refreshTokenService.issueForUser(user);
		return new AuthResult(new AuthResponse(accessToken, toUserResponse(user)), refresh.rawToken());
	}

	private static UserResponse toUserResponse(User user) {
		return new UserResponse(user.getId().toString(), user.getEmail(), user.getUsername(),
				user.getAvatarBase64(), user.getRating());
	}

	public record AuthResult(AuthResponse response, String refreshTokenValue) {
	}
}
