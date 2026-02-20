package com.tcg.arena.controller;

import com.tcg.arena.dto.ReferralStatusDTO;
import com.tcg.arena.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users/referrals")
public class ReferralController {

    @Autowired
    private UserService userService;

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
