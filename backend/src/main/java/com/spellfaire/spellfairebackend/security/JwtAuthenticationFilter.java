package com.spellfaire.spellfairebackend.security;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.spellfaire.spellfairebackend.auth.service.JwtService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
	private final JwtService jwtService;

	public JwtAuthenticationFilter(JwtService jwtService) {
		this.jwtService = jwtService;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String path = request.getRequestURI();
		if (shouldSkip(path, request.getMethod())) {
			filterChain.doFilter(request, response);
			return;
		}

		String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (authHeader == null || !authHeader.startsWith("Bearer ")) {
			filterChain.doFilter(request, response);
			return;
		}

		String token = authHeader.substring("Bearer ".length()).trim();
		try {
			DecodedJWT jwt = jwtService.verify(token);
			String userId = jwt.getSubject();

			UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
					userId,
					null,
					List.of(new SimpleGrantedAuthority("ROLE_USER")));
			authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
			SecurityContextHolder.getContext().setAuthentication(authentication);
		} catch (JWTVerificationException ignored) {
			// Invalid token: treat as anonymous.
		}

		filterChain.doFilter(request, response);
	}

	private static boolean shouldSkip(String path, String method) {
		if (!"POST".equalsIgnoreCase(method)) {
			return false;
		}
		return "/api/auth/register".equals(path)
				|| "/api/auth/login".equals(path)
				|| "/api/auth/refresh".equals(path)
				|| "/api/auth/logout".equals(path);
	}
}
