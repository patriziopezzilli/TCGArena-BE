package com.tcg.arena.controller;

import com.tcg.arena.model.TCGType;
import com.tcg.arena.service.BatchService;
import com.tcg.arena.service.TCGApiClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/batch")
@Tag(name = "Batch Import", description = "API for triggering batch card imports")
public class BatchController {

    private static final Logger logger = LoggerFactory.getLogger(BatchController.class);

    @Autowired
    private TCGApiClient tcgApiClient;

    @Autowired
    private com.tcg.arena.service.AsyncImportService asyncImportService;

    @PostMapping("/tcg/{tcgType}")
    @Operation(summary = "Trigger TCG import", description = "Triggers a TCG API import for a specific TCG type with real-time pricing (Async)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "TCG import started successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid or unsupported TCG type")
    })
    public ResponseEntity<Map<String, Object>> triggerTCGImport(
            @Parameter(description = "TCG type to import") @PathVariable String tcgType) {

        Map<String, Object> response = new HashMap<>();

        try {
            TCGType type = TCGType.valueOf(tcgType.toUpperCase());

            if (!tcgApiClient.isTCGSupported(type)) {
                response.put("success", false);
                response.put("message", type.getDisplayName() + " is not supported by TCG API");
                response.put("supportedTypes", tcgApiClient.getSupportedTCGTypes());
                return ResponseEntity.badRequest().body(response);
            }

            // Start async job
            com.tcg.arena.model.ImportJob job = asyncImportService.triggerTCGImportAsync(type);

            response.put("success", true);
            response.put("message", "TCG import started async for " + type.getDisplayName());
            response.put("tcgType", type.name());
            response.put("jobId", job.getId()); // Return Job ID for polling

            logger.info("TCG import triggered async for {}, Job ID: {}", type, job.getId());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", "Invalid TCG type: " + tcgType);
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            logger.error("Error triggering TCG import: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Error starting TCG import: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/jobs/{jobId}")
    @Operation(summary = "Get import job status", description = "Returns the current status and progress of an async import job")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Job status retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Job not found")
    })
    public ResponseEntity<com.tcg.arena.model.ImportJob> getJobStatus(
            @Parameter(description = "Unique identifier of the job") @PathVariable String jobId) {

        com.tcg.arena.model.ImportJob job = asyncImportService.getJobStatus(jobId);

        if (job != null) {
            return ResponseEntity.ok(job);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/tcg/supported")
    @Operation(summary = "Get supported TCG types", description = "Returns list of TCG types supported by TCG API")
    @ApiResponse(responseCode = "200", description = "List of supported TCG types")
    public ResponseEntity<Map<String, Object>> getSupportedTCGTypes() {
        Map<String, Object> response = new HashMap<>();

        List<TCGType> supportedTypes = tcgApiClient.getSupportedTCGTypes();
        response.put("supportedTypes", supportedTypes);
        response.put("count", supportedTypes.size());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/tcg/games")
    @Operation(summary = "Get available TCG games", description = "Fetches the list of available games from TCG API to verify game IDs")
    @ApiResponse(responseCode = "200", description = "List of available games from TCG")
    public ResponseEntity<Map<String, Object>> getTCGGames() {
        Map<String, Object> response = new HashMap<>();

        try {
            List<?> games = tcgApiClient.getGames().block();
            response.put("success", true);
            response.put("games", games);
            response.put("count", games != null ? games.size() : 0);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error fetching TCG games: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
