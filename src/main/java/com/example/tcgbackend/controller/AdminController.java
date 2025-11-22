package com.example.tcgbackend.controller;

import com.example.tcgbackend.model.TCGType;
import com.example.tcgbackend.service.BatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin", description = "Administrative API for managing batch operations and system maintenance")
public class AdminController {

    @Autowired
    private BatchService batchService;

    @PostMapping("/import/{tcgType}")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Trigger batch import for specific TCG type", description = "Starts a batch job to import cards for the specified TCG type")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Batch import triggered successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid TCG type provided")
    })
    public ResponseEntity<String> triggerBatchImport(
            @Parameter(description = "TCG type to import (POKEMON, MAGIC, YUGIOH, etc.)") @PathVariable TCGType tcgType,
            @Parameter(description = "Starting index for import (-99 to import all)") @RequestParam(defaultValue = "-99") int startIndex,
            @Parameter(description = "Ending index for import (-99 to import until end)") @RequestParam(defaultValue = "-99") int endIndex) {
        try {
            batchService.triggerBatchImport(tcgType, startIndex, endIndex);
            String message;
            if (startIndex == -99 && endIndex == -99) {
                message = "Batch import triggered successfully for " + tcgType;
            } else if (endIndex == -99) {
                message = "Batch import triggered successfully for " + tcgType + " starting from index " + startIndex;
            } else {
                message = "Batch import triggered successfully for " + tcgType + " from index " + startIndex + " to " + endIndex;
            }
            return ResponseEntity.ok(message);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to trigger batch import: " + e.getMessage());
        }
    }
}