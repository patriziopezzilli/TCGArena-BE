package com.example.tcgbackend.controller;

import com.example.tcgbackend.model.TCGType;
import com.example.tcgbackend.service.BatchService;
import com.example.tcgbackend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin", description = "Administrative API for managing batch operations and system maintenance")
public class AdminController {

    @Autowired
    private BatchService batchService;

    @Autowired
    private UserService userService;

    @PostMapping("/trigger-batch/{tcgType}")
    @Operation(summary = "Trigger manual batch import", description = "Manually triggers the batch import process for a specific TCG type. Only merchants can trigger batch imports.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Batch import triggered successfully"),
        @ApiResponse(responseCode = "403", description = "Only merchants can trigger batch imports"),
        @ApiResponse(responseCode = "400", description = "Invalid TCG type provided")
    })
    public ResponseEntity<String> triggerBatchImport(@Parameter(description = "TCG type to import (POKEMON, MAGIC, YUGIOH, etc.)") @PathVariable TCGType tcgType) {
        // Check if current user is a merchant
        Optional<com.example.tcgbackend.model.User> currentUser = userService.getCurrentUser();
        if (currentUser.isEmpty() || !Boolean.TRUE.equals(currentUser.get().getIsMerchant())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only merchants can trigger batch imports");
        }

        try {
            batchService.triggerBatchImport(tcgType);
            return ResponseEntity.ok("Batch import triggered successfully for " + tcgType);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to trigger batch import: " + e.getMessage());
        }
    }
}