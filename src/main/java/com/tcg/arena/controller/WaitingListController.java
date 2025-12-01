package com.tcg.arena.controller;

import com.tcg.arena.dto.WaitingListRequestDTO;
import com.tcg.arena.dto.WaitingListResponseDTO;
import com.tcg.arena.model.WaitingListEntry;
import com.tcg.arena.service.WaitingListService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/waiting-list")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class WaitingListController {
    
    private final WaitingListService waitingListService;
    
    @PostMapping("/join")
    public ResponseEntity<WaitingListResponseDTO> joinWaitingList(
            @Valid @RequestBody WaitingListRequestDTO request) {
        WaitingListResponseDTO response = waitingListService.addToWaitingList(request);
        return ResponseEntity.ok(response);
    }
    
    // Admin endpoints
    @GetMapping("/all")
    public ResponseEntity<List<WaitingListEntry>> getAllEntries() {
        return ResponseEntity.ok(waitingListService.getAllEntries());
    }
    
    @GetMapping("/uncontacted")
    public ResponseEntity<List<WaitingListEntry>> getUncontactedEntries() {
        return ResponseEntity.ok(waitingListService.getUncontactedEntries());
    }
    
    @PutMapping("/{id}/contacted")
    public ResponseEntity<Void> markAsContacted(@PathVariable Long id) {
        waitingListService.markAsContacted(id);
        return ResponseEntity.ok().build();
    }
}
