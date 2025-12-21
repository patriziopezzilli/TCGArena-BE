package com.tcg.arena.security;

import com.tcg.arena.model.ArenaApiKey;
import com.tcg.arena.repository.ArenaApiKeyRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Filter that validates API keys for Arena API endpoints.
 * Intercepts requests to /api/arena/** and validates the X-Arena-Api-Key
 * header.
 */
@Component
public class ArenaApiKeyFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(ArenaApiKeyFilter.class);

    public static final String API_KEY_HEADER = "X-Arena-Api-Key";
    public static final String ARENA_API_PATH = "/api/arena";

    @Autowired
    private ArenaApiKeyRepository apiKeyRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        // Only filter Arena API endpoints
        if (!path.startsWith(ARENA_API_PATH)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Get API key from header
        String apiKey = request.getHeader(API_KEY_HEADER);

        if (apiKey == null || apiKey.isBlank()) {
            sendError(response, HttpStatus.UNAUTHORIZED,
                    "Missing API key. Please provide the X-Arena-Api-Key header.");
            return;
        }

        // Validate API key
        Optional<ArenaApiKey> keyOpt = apiKeyRepository.findByApiKeyAndActiveTrue(apiKey);

        if (keyOpt.isEmpty()) {
            sendError(response, HttpStatus.UNAUTHORIZED,
                    "Invalid or inactive API key.");
            return;
        }

        ArenaApiKey key = keyOpt.get();

        // Check rate limit
        if (!key.canMakeRequest()) {
            int limit = key.getPlan().getDailyRequestLimit();
            response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
            response.setHeader("X-RateLimit-Remaining", "0");
            response.setHeader("Retry-After", "86400"); // 24 hours in seconds
            sendError(response, HttpStatus.TOO_MANY_REQUESTS,
                    String.format("Rate limit exceeded. Your plan (%s) allows %d requests per day.",
                            key.getPlan().getDisplayName(), limit));
            return;
        }

        // Record the request
        key.recordRequest();
        apiKeyRepository.save(key);

        // Add rate limit headers
        response.setHeader("X-RateLimit-Limit",
                key.getPlan().isUnlimited() ? "unlimited" : String.valueOf(key.getPlan().getDailyRequestLimit()));
        response.setHeader("X-RateLimit-Remaining",
                key.getPlan().isUnlimited() ? "unlimited" : String.valueOf(key.getRemainingRequests()));
        response.setHeader("X-Arena-Plan", key.getPlan().name());

        // Store API key info in request for controller use
        request.setAttribute("arenaApiKey", key);

        logger.debug("Arena API request authenticated: key={}, plan={}, remaining={}",
                key.getName(), key.getPlan(), key.getRemainingRequests());

        filterChain.doFilter(request, response);
    }

    private void sendError(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(String.format(
                "{\"error\": \"%s\", \"status\": %d, \"message\": \"%s\"}",
                status.getReasonPhrase(), status.value(), message));
    }
}
