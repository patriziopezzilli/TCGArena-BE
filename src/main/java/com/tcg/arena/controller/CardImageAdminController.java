package com.tcg.arena.controller;

import com.tcg.arena.service.CardImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/images")
public class CardImageAdminController {

    @Autowired
    private CardImageService cardImageService;

    @PostMapping("/sync")
    public ResponseEntity<String> triggerSync() {
        cardImageService.syncImages();
        return ResponseEntity.ok("Image sync started in background.");
    }

    @GetMapping("/status")
    public ResponseEntity<String> getStatus() {
        return ResponseEntity.ok(cardImageService.getStatus());
    }
}
