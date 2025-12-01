package com.example.tcgbackend.controller;

import com.example.tcgbackend.model.Shop;
import com.example.tcgbackend.model.User;
import com.example.tcgbackend.service.ShopService;
import com.example.tcgbackend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/merchant")
public class MerchantBackofficeController {

    @Autowired
    private UserService userService;

    @Autowired
    private ShopService shopService;

    /**
     * Get merchant shop status
     * Returns shop info and active status for the logged merchant
     */
    @GetMapping("/shop/status")
    public ResponseEntity<?> getShopStatus() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        Optional<User> userOpt = userService.getUserByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("User not found");
        }

        User user = userOpt.get();
        if (!user.getIsMerchant()) {
            return ResponseEntity.badRequest().body("User is not a merchant");
        }

        if (user.getShopId() == null) {
            return ResponseEntity.badRequest().body("No shop associated with this merchant");
        }

        Optional<Shop> shopOpt = shopService.getShopById(user.getShopId());
        if (shopOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Shop not found");
        }

        Shop shop = shopOpt.get();
        Map<String, Object> response = new HashMap<>();
        response.put("shop", shop);
        response.put("active", shop.getActive());
        response.put("verified", shop.getIsVerified());
        response.put("user", user);

        return ResponseEntity.ok(response);
    }

    /**
     * Get merchant profile
     */
    @GetMapping("/profile")
    public ResponseEntity<?> getMerchantProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        Optional<User> userOpt = userService.getUserByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("User not found");
        }

        User user = userOpt.get();
        if (!user.getIsMerchant()) {
            return ResponseEntity.badRequest().body("User is not a merchant");
        }

        return ResponseEntity.ok(user);
    }
}
