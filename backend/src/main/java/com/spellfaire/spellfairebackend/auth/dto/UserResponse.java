package com.spellfaire.spellfairebackend.auth.dto;

public class UserResponse {
	private final String id;
	private final String email;
	private final String username;
	private final String avatarBase64;
	private final int rating;

	public UserResponse(String id, String email, String username, String avatarBase64, int rating) {
		this.id = id;
		this.email = email;
		this.username = username;
		this.avatarBase64 = avatarBase64;
		this.rating = rating;
	}

	public String getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

	public String getUsername() {
		return username;
	}

	public String getAvatarBase64() {
		return avatarBase64;
	}

	public int getRating() {
		return rating;
	}
}
