package com.tcg.arena.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/avatars")
@Tag(name = "Avatars", description = "API for retrieving avatar presets")
public class AvatarController {

    @GetMapping("/presets")
    @Operation(summary = "Get avatar presets", description = "Returns a list of curated pixel-art avatar URLs")
    public ResponseEntity<List<String>> getPresets() {
        List<String> presets = new ArrayList<>();
        String baseUrl = "https://api.dicebear.com/9.x/pixel-art/png?seed=";

        // Curated list of seeds that generate good-looking pixel art avatars
        String[] seeds = {
                "Felix", "Bubba", "Spooky", "Misty", "Shadow",
                "Sparky", "Rocky", "Luna", "Sunny", "Buddy",
                "Rex", "Spot", "Patch", "Tiger", "Leo",
                "Max", "Bella", "Charlie", "Simba", "Coco"
        };

        for (String seed : seeds) {
            presets.add(baseUrl + seed);
        }

        return ResponseEntity.ok(presets);
    }
}
