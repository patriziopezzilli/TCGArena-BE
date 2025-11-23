package com.example.tcgbackend.controller;

import com.example.tcgbackend.dto.UserActivityDTO;
import com.example.tcgbackend.service.UserActivityService;
import com.example.tcgbackend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/user-activities")
public class UserActivityController {

    @Autowired
    private UserActivityService userActivityService;

    @Autowired
    private UserService userService;

    @GetMapping
    public ResponseEntity<List<UserActivityDTO>> getCurrentUserActivities(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "20") int limit) {

        Optional<com.example.tcgbackend.model.User> currentUser = userService.getCurrentUser();
        if (currentUser.isPresent()) {
            List<UserActivityDTO> activities = userActivityService.getRecentUserActivities(currentUser.get().getId(), limit);
            return ResponseEntity.ok(activities);
        } else {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<UserActivityDTO>> getUserActivities(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "20") int limit) {

        List<UserActivityDTO> activities = userActivityService.getRecentUserActivities(userId, limit);
        return ResponseEntity.ok(activities);
    }

    @GetMapping("/recent/global")
    public ResponseEntity<List<UserActivityDTO>> getRecentGlobalActivities(
            @RequestParam(defaultValue = "50") int limit) {

        List<UserActivityDTO> activities = userActivityService.getRecentGlobalActivities(limit);
        return ResponseEntity.ok(activities);
    }
}