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
    public ResponseEntity<String> triggerSync(
            @org.springframework.web.bind.annotation.RequestParam(required = false) String tcgType,
            @org.springframework.web.bind.annotation.RequestParam(required = false) Integer year) {
        cardImageService.syncImages(tcgType, year);
        return ResponseEntity.ok("Image sync started in background.");
    }

    @GetMapping("/status")
    public ResponseEntity<String> getStatus() {
        return ResponseEntity.ok(cardImageService.getStatus());
    }
}
