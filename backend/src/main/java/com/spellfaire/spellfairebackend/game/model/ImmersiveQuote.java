package com.spellfaire.spellfairebackend.game.model;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A short immersive quote shown on the Home page to reinforce the fantasy tone.
 * Seeded from JSON at backend startup.
 */
@Entity
@Table(name = "immersive_quotes")
public class ImmersiveQuote {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(columnDefinition = "BINARY(16)")
	private UUID id;

	@Column(length = 512, nullable = false, unique = true)
	private String text;

	public ImmersiveQuote() {
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}
}
