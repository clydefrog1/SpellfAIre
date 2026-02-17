package com.spellfaire.spellfairebackend.auth.dto;

public class UserResponse {
	private final String id;
	private final String email;
	private final String username;

	public UserResponse(String id, String email, String username) {
		this.id = id;
		this.email = email;
		this.username = username;
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
}
