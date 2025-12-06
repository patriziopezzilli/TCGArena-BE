package com.tcg.arena.controller;

import com.tcg.arena.model.Partner;
import com.tcg.arena.service.PartnerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/partners")
@Tag(name = "Partners", description = "API for managing reward partners")
public class PartnerController {

    @Autowired
    private PartnerService partnerService;

    @GetMapping
    @Operation(summary = "Get all active partners", description = "Retrieves a list of all active partners")
    public List<Partner> getAllActivePartners() {
        return partnerService.getAllActivePartners();
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all partners (Admin)", description = "Retrieves a list of all partners including inactive ones")
    public List<Partner> getAllPartners() {
        return partnerService.getAllPartners();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get partner by ID", description = "Retrieves a specific partner by its unique ID")
    public ResponseEntity<Partner> getPartnerById(
            @Parameter(description = "Unique identifier of the partner") @PathVariable Long id) {
        return partnerService.getPartnerById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create/Update a partner", description = "Creates or updates a partner (Admin only)")
    public Partner savePartner(@RequestBody Partner partner) {
        return partnerService.savePartner(partner);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a partner", description = "Deletes a partner (Admin only)")
    public ResponseEntity<Void> deletePartner(@PathVariable Long id) {
        partnerService.deletePartner(id);
        return ResponseEntity.ok().build();
    }
}
