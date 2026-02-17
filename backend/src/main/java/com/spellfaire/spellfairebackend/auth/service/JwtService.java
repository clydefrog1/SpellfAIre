package com.spellfaire.spellfairebackend.auth.service;

import java.time.Instant;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.spellfaire.spellfairebackend.auth.model.User;

@Service
public class JwtService {
	private final Algorithm algorithm;
	private final JWTVerifier verifier;
	private final String issuer;
	private final long accessTokenTtlSeconds;

	public JwtService(
			@Value("${spellfaire.security.jwt.secret}") String secret,
			@Value("${spellfaire.security.jwt.issuer}") String issuer,
			@Value("${spellfaire.security.jwt.access-token-ttl-seconds}") long accessTokenTtlSeconds) {
		this.algorithm = Algorithm.HMAC256(secret);
		this.issuer = issuer;
		this.accessTokenTtlSeconds = accessTokenTtlSeconds;
		this.verifier = JWT.require(this.algorithm).withIssuer(this.issuer).build();
	}

	public String createAccessToken(User user) {
		Instant now = Instant.now();
		Instant expiresAt = now.plusSeconds(accessTokenTtlSeconds);

		return JWT.create()
				.withIssuer(issuer)
				.withSubject(user.getId())
				.withIssuedAt(Date.from(now))
				.withExpiresAt(Date.from(expiresAt))
				.withClaim("email", user.getEmail())
				.withClaim("username", user.getUsername())
				.sign(algorithm);
	}

	public DecodedJWT verify(String token) throws JWTVerificationException {
		return verifier.verify(token);
	}
}
