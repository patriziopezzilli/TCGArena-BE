package com.tcg.arena.controller;

import com.tcg.arena.model.TCGType;
import com.tcg.arena.service.BatchService;
import com.tcg.arena.service.JustTCGApiClient;
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
    private BatchService batchService;

    @Autowired
    private JustTCGApiClient justTCGApiClient;

    @Autowired
    private com.tcg.arena.service.AsyncImportService asyncImportService;

    @PostMapping("/import/{tcgType}")
    @Operation(summary = "Trigger legacy batch import", description = "Triggers the legacy batch import for a specific TCG type")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Import started successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid TCG type")
    })
    public ResponseEntity<Map<String, Object>> triggerBatchImport(
            @Parameter(description = "TCG type to import") @PathVariable String tcgType,
            @RequestParam(required = false, defaultValue = "-99") int startIndex,
            @RequestParam(required = false, defaultValue = "-99") int endIndex) {

        Map<String, Object> response = new HashMap<>();

        try {
            TCGType type = TCGType.valueOf(tcgType.toUpperCase());
            batchService.triggerBatchImport(type, startIndex, endIndex);

            response.put("success", true);
            response.put("message", "Batch import started for " + type.getDisplayName());
            response.put("tcgType", type.name());

            logger.info("Legacy batch import triggered for {}", type);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", "Invalid TCG type: " + tcgType);
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error starting import: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/justtcg/{tcgType}")
    @Operation(summary = "Trigger JustTCG import", description = "Triggers a JustTCG API import for a specific TCG type with real-time pricing (Async)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "JustTCG import started successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid or unsupported TCG type")
    })
    public ResponseEntity<Map<String, Object>> triggerJustTCGImport(
            @Parameter(description = "TCG type to import") @PathVariable String tcgType) {

        Map<String, Object> response = new HashMap<>();

        try {
            TCGType type = TCGType.valueOf(tcgType.toUpperCase());

            if (!justTCGApiClient.isTCGSupported(type)) {
                response.put("success", false);
                response.put("message", type.getDisplayName() + " is not supported by JustTCG API");
                response.put("supportedTypes", justTCGApiClient.getSupportedTCGTypes());
                return ResponseEntity.badRequest().body(response);
            }

            // Start async job
            com.tcg.arena.model.ImportJob job = asyncImportService.triggerJustTCGImportAsync(type);

            response.put("success", true);
            response.put("message", "JustTCG import started async for " + type.getDisplayName());
            response.put("tcgType", type.name());
            response.put("jobId", job.getId()); // Return Job ID for polling

            logger.info("JustTCG import triggered async for {}, Job ID: {}", type, job.getId());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", "Invalid TCG type: " + tcgType);
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            logger.error("Error triggering JustTCG import: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Error starting JustTCG import: " + e.getMessage());
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

    @GetMapping("/justtcg/supported")
    @Operation(summary = "Get supported TCG types", description = "Returns list of TCG types supported by JustTCG API")
    @ApiResponse(responseCode = "200", description = "List of supported TCG types")
    public ResponseEntity<Map<String, Object>> getSupportedTCGTypes() {
        Map<String, Object> response = new HashMap<>();

        List<TCGType> supportedTypes = justTCGApiClient.getSupportedTCGTypes();
        response.put("supportedTypes", supportedTypes);
        response.put("count", supportedTypes.size());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/justtcg/games")
    @Operation(summary = "Get available JustTCG games", description = "Fetches the list of available games from JustTCG API to verify game IDs")
    @ApiResponse(responseCode = "200", description = "List of available games from JustTCG")
    public ResponseEntity<Map<String, Object>> getJustTCGGames() {
        Map<String, Object> response = new HashMap<>();

        try {
            List<?> games = justTCGApiClient.getGames().block();
            response.put("success", true);
            response.put("games", games);
            response.put("count", games != null ? games.size() : 0);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error fetching JustTCG games: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
