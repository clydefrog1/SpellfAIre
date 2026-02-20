package com.spellfaire.spellfairebackend.auth.dto;

import jakarta.validation.constraints.Size;

public class UpdateProfileRequest {

	@Size(min = 3, max = 30)
	private String username;

	private String currentPassword;

	@Size(min = 8, max = 72)
	private String newPassword;

	/** Base64-encoded data URL of the avatar image, e.g. "data:image/png;base64,..." */
	private String avatarBase64;

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getCurrentPassword() {
		return currentPassword;
	}

	public void setCurrentPassword(String currentPassword) {
		this.currentPassword = currentPassword;
	}

	public String getNewPassword() {
		return newPassword;
	}

	public void setNewPassword(String newPassword) {
		this.newPassword = newPassword;
	}

	public String getAvatarBase64() {
		return avatarBase64;
	}

	public void setAvatarBase64(String avatarBase64) {
		this.avatarBase64 = avatarBase64;
	}
}
