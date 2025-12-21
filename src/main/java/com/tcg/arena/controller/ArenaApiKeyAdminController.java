package com.tcg.arena.controller;

import com.tcg.arena.model.ArenaApiKey;
import com.tcg.arena.model.ArenaApiPlan;
import com.tcg.arena.repository.ArenaApiKeyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Admin controller for managing Arena API keys.
 * Protected by standard JWT authentication (admin endpoints).
 */
@RestController
@RequestMapping("/api/admin/arena-keys")
public class ArenaApiKeyAdminController {

    @Autowired
    private ArenaApiKeyRepository apiKeyRepository;

    /**
     * GET /api/admin/arena-keys - List all API keys
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> listApiKeys() {
        List<ArenaApiKey> keys = apiKeyRepository.findAll();

        List<Map<String, Object>> keyList = keys.stream().map(this::toDTO).collect(Collectors.toList());

        // Statistics
        Map<String, Long> planStats = keys.stream()
                .collect(Collectors.groupingBy(k -> k.getPlan().name(), Collectors.counting()));

        return ResponseEntity.ok(Map.of(
                "keys", keyList,
                "total", keys.size(),
                "active", keys.stream().filter(ArenaApiKey::isActive).count(),
                "byPlan", planStats));
    }

    /**
     * GET /api/admin/arena-keys/{id} - Get API key details
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getApiKey(@PathVariable Long id) {
        return apiKeyRepository.findById(id)
                .map(key -> ResponseEntity.ok(toFullDTO(key)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * POST /api/admin/arena-keys - Create new API key
     */
    @PostMapping
    public ResponseEntity<?> createApiKey(@RequestBody CreateKeyRequest request) {
        if (request.name == null || request.email == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Name and email are required"));
        }

        ArenaApiPlan plan = ArenaApiPlan.FREE;
        if (request.plan != null) {
            try {
                plan = ArenaApiPlan.valueOf(request.plan.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Invalid plan: " + request.plan,
                        "validPlans", Arrays.stream(ArenaApiPlan.values()).map(Enum::name).toList()));
            }
        }

        ArenaApiKey key = new ArenaApiKey(request.name, request.email, plan);
        key.setDescription(request.description);
        key = apiKeyRepository.save(key);

        return ResponseEntity.ok(Map.of(
                "message", "API key created successfully",
                "apiKey", key.getApiKey(),
                "id", key.getId(),
                "plan", key.getPlan().name()));
    }

    /**
     * PUT /api/admin/arena-keys/{id} - Update API key
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateApiKey(@PathVariable Long id, @RequestBody UpdateKeyRequest request) {
        return apiKeyRepository.findById(id)
                .map(key -> {
                    if (request.name != null)
                        key.setName(request.name);
                    if (request.email != null)
                        key.setEmail(request.email);
                    if (request.description != null)
                        key.setDescription(request.description);
                    if (request.active != null)
                        key.setActive(request.active);

                    if (request.plan != null) {
                        try {
                            key.setPlan(ArenaApiPlan.valueOf(request.plan.toUpperCase()));
                        } catch (IllegalArgumentException ignored) {
                        }
                    }

                    apiKeyRepository.save(key);
                    return ResponseEntity.ok(Map.of("message", "API key updated", "key", toDTO(key)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * POST /api/admin/arena-keys/{id}/regenerate - Regenerate API key
     */
    @PostMapping("/{id}/regenerate")
    public ResponseEntity<?> regenerateApiKey(@PathVariable Long id) {
        return apiKeyRepository.findById(id)
                .map(key -> {
                    key.regenerateKey();
                    apiKeyRepository.save(key);
                    return ResponseEntity.ok(Map.of(
                            "message", "API key regenerated",
                            "newApiKey", key.getApiKey()));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * DELETE /api/admin/arena-keys/{id} - Delete API key
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteApiKey(@PathVariable Long id) {
        if (!apiKeyRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        apiKeyRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "API key deleted"));
    }

    /**
     * POST /api/admin/arena-keys/{id}/toggle - Toggle active status
     */
    @PostMapping("/{id}/toggle")
    public ResponseEntity<?> toggleApiKey(@PathVariable Long id) {
        return apiKeyRepository.findById(id)
                .map(key -> {
                    key.setActive(!key.isActive());
                    apiKeyRepository.save(key);
                    return ResponseEntity.ok(Map.of(
                            "message", key.isActive() ? "API key activated" : "API key deactivated",
                            "active", key.isActive()));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ==================== DTOs ====================

    private Map<String, Object> toDTO(ArenaApiKey key) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", key.getId());
        dto.put("name", key.getName());
        dto.put("email", key.getEmail());
        dto.put("plan", key.getPlan().name());
        dto.put("planDisplay", key.getPlan().getDisplayName());
        dto.put("active", key.isActive());
        dto.put("requestsToday", key.getRequestsToday());
        dto.put("dailyLimit", key.getPlan().getFormattedLimit());
        dto.put("createdAt", key.getCreatedAt());
        dto.put("lastUsedAt", key.getLastUsedAt());
        return dto;
    }

    private Map<String, Object> toFullDTO(ArenaApiKey key) {
        Map<String, Object> dto = toDTO(key);
        dto.put("apiKey", key.getApiKey()); // Only show full key in detail view
        dto.put("description", key.getDescription());
        dto.put("maxBatchSize", key.getPlan().getMaxBatchSize());
        dto.put("remainingRequests", key.getRemainingRequests());
        return dto;
    }

    // ==================== Request DTOs ====================

    public static class CreateKeyRequest {
        public String name;
        public String email;
        public String description;
        public String plan;
    }

    public static class UpdateKeyRequest {
        public String name;
        public String email;
        public String description;
        public String plan;
        public Boolean active;
    }
}
