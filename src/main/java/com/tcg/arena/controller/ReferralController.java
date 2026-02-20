package com.tcg.arena.controller;

import com.tcg.arena.dto.ReferralStatusDTO;
import com.tcg.arena.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users/referrals")
public class ReferralController {

    @Autowired
    private UserService userService;

    @Operation(summary = "Apply a referral code for the current user", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/apply-code")
    public ResponseEntity<?> applyReferralCode(@RequestBody java.util.Map<String, String> body) {
        String code = body.get("code");
        if (code == null || code.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", "Referral code is required"));
        }

        return userService.getCurrentUser().map(currentUser -> {
            // Check if user already used a referral code (prevent abuse)
            if (currentUser.getReferredBy() != null) {
                return ResponseEntity.badRequest()
                        .body(java.util.Map.of("message", "You have already used a referral code"));
            }

            // Prevent self-referral
            String userInvitationCode = userService.getOrCreateInvitationCode(currentUser);
            if (code.trim().equalsIgnoreCase(userInvitationCode)) {
                return ResponseEntity.badRequest()
                        .body(java.util.Map.of("message", "You cannot use your own referral code"));
            }

            try {
                userService.processReferralCode(code.trim(), currentUser.getUsername());
                // Mark user as having used a referral code
                currentUser.setReferredBy(code.trim());
                userService.saveUser(currentUser);
                return ResponseEntity.ok(java.util.Map.of("message", "Referral code applied successfully"));
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(java.util.Map.of("message", "Invalid referral code"));
            }
        }).orElse(ResponseEntity.status(401).build());
    }

    @Operation(summary = "Get current user referral status", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/me")
    public ResponseEntity<ReferralStatusDTO> getMyReferralStatus() {
        return userService.getCurrentUser().map(user -> {
            String invitationCode = userService.getOrCreateInvitationCode(user);
            return ResponseEntity.ok(new ReferralStatusDTO(
                    user.getUsername(),
                    invitationCode,
                    user.getReferralsCount() != null ? user.getReferralsCount() : 0));
        }).orElse(ResponseEntity.status(401).build());
    }

    @Operation(summary = "Get global referral leaderboard", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/leaderboard")
    public ResponseEntity<List<ReferralStatusDTO>> getReferralLeaderboard() {
        return ResponseEntity.ok(userService.getReferralLeaderboard());
    }
}
