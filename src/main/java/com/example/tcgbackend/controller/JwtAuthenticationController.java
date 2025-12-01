package com.example.tcgbackend.controller;

import com.example.tcgbackend.dto.MerchantRegistrationRequestDTO;
import com.example.tcgbackend.dto.MerchantRegistrationResponseDTO;
import com.example.tcgbackend.dto.RefreshTokenRequest;
import com.example.tcgbackend.dto.RefreshTokenResponse;
import com.example.tcgbackend.dto.RegisterRequestDTO;
import com.example.tcgbackend.model.Shop;
import com.example.tcgbackend.model.ShopType;
import com.example.tcgbackend.model.User;
import com.example.tcgbackend.security.JwtTokenUtil;
import com.example.tcgbackend.security.JwtUserDetailsService;
import com.example.tcgbackend.service.ShopService;
import com.example.tcgbackend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class JwtAuthenticationController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private JwtUserDetailsService userDetailsService;

    @Autowired
    private UserService userService;

    @Autowired
    private ShopService shopService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<?> createAuthenticationToken(@RequestBody JwtRequest authenticationRequest) throws Exception {
        authenticate(authenticationRequest.getUsername(), authenticationRequest.getPassword());

        final UserDetails userDetails = userDetailsService.loadUserByUsername(authenticationRequest.getUsername());
        final String token = jwtTokenUtil.generateToken(userDetails);
        final String refreshToken = jwtTokenUtil.generateRefreshToken(userDetails);

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("refreshToken", refreshToken);
        response.put("user", userService.getUserByUsername(authenticationRequest.getUsername()).orElse(null));

        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody RegisterRequestDTO registerRequest) throws Exception {
        if (userService.getUserByUsername(registerRequest.getUsername()).isPresent() ||
                userService.getUserByEmail(registerRequest.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body("Username or email already exists");
        }

        // Create User from DTO
        User user = new User();
        user.setEmail(registerRequest.getEmail());
        user.setUsername(registerRequest.getUsername());
        user.setDisplayName(registerRequest.getDisplayName() != null ? registerRequest.getDisplayName() : registerRequest.getUsername());
        user.setPassword(registerRequest.getPassword());
        user.setDateJoined(LocalDateTime.now());
        
        // Set favorite TCGs from the list
        if (registerRequest.getFavoriteGames() != null && !registerRequest.getFavoriteGames().isEmpty()) {
            user.setFavoriteTCGTypes(registerRequest.getFavoriteGames());
        }

        System.out.println("DEBUG: Registering user: " + user.getUsername());
        System.out.println("DEBUG: Original password length: "
                + (user.getPassword() != null ? user.getPassword().length() : "null"));
        String encodedPassword = passwordEncoder.encode(user.getPassword());
        System.out.println("DEBUG: Encoded password: " + encodedPassword);
        user.setPassword(encodedPassword);
        
        User savedUser = userService.saveUser(user);
        System.out.println("DEBUG: User saved with password: " + savedUser.getPassword());

        final UserDetails userDetails = userDetailsService.loadUserByUsername(savedUser.getUsername());
        final String token = jwtTokenUtil.generateToken(userDetails);
        final String refreshToken = jwtTokenUtil.generateRefreshToken(userDetails);

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("refreshToken", refreshToken);
        response.put("user", savedUser);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@RequestBody RefreshTokenRequest refreshTokenRequest) throws Exception {
        String refreshToken = refreshTokenRequest.getRefreshToken();
        if (refreshToken == null || refreshToken.isEmpty()) {
            return ResponseEntity.badRequest().body("Refresh token is required");
        }

        String username = jwtTokenUtil.getUsernameFromToken(refreshToken);
        if (username == null) {
            return ResponseEntity.badRequest().body("Invalid refresh token");
        }

        final UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        if (!jwtTokenUtil.validateRefreshToken(refreshToken, userDetails)) {
            return ResponseEntity.badRequest().body("Invalid or expired refresh token");
        }

        final String newAccessToken = jwtTokenUtil.generateToken(userDetails);
        final String newRefreshToken = jwtTokenUtil.generateRefreshToken(userDetails);

        RefreshTokenResponse response = new RefreshTokenResponse(newAccessToken, newRefreshToken);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register-merchant")
    public ResponseEntity<?> registerMerchant(@RequestBody MerchantRegistrationRequestDTO request) throws Exception {
        // Check if username or email already exists
        if (userService.getUserByUsername(request.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body("Username already exists");
        }
        if (userService.getUserByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body("Email already exists");
        }

        // Create User
        User user = new User();
        user.setEmail(request.getEmail());
        user.setUsername(request.getUsername());
        user.setDisplayName(request.getDisplayName() != null ? request.getDisplayName() : request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setDateJoined(LocalDateTime.now());
        user.setIsMerchant(true);
        user.setIsPremium(false);
        
        User savedUser = userService.saveUser(user);

        // Create Shop
        Shop shop = new Shop();
        shop.setName(request.getShopName());
        shop.setDescription(request.getDescription());
        shop.setAddress(request.getAddress() + ", " + request.getCity() + " " + request.getZipCode());
        shop.setPhoneNumber(request.getPhone());
        shop.setType(ShopType.LOCAL_STORE);
        shop.setIsVerified(false);
        shop.setOwnerId(savedUser.getId());
        
        Shop savedShop = shopService.saveShop(shop);

        // Update user with shopId
        savedUser.setShopId(savedShop.getId());
        savedUser = userService.saveUser(savedUser);

        // Generate JWT token
        final UserDetails userDetails = userDetailsService.loadUserByUsername(savedUser.getUsername());
        final String token = jwtTokenUtil.generateToken(userDetails);

        // Create response
        MerchantRegistrationResponseDTO response = new MerchantRegistrationResponseDTO(savedUser, savedShop, token);
        return ResponseEntity.ok(response);
    }

    private void authenticate(String username, String password) throws Exception {
        try {
            System.out.println("DEBUG: Attempting to authenticate user: " + username);
            System.out.println("DEBUG: Password length: " + (password != null ? password.length() : "null"));
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
            System.out.println("DEBUG: Authentication successful for user: " + username);
        } catch (DisabledException e) {
            System.out.println("DEBUG: User disabled: " + username);
            throw new Exception("USER_DISABLED", e);
        } catch (BadCredentialsException e) {
            System.out.println("DEBUG: Bad credentials for user: " + username);
            throw new Exception("INVALID_CREDENTIALS", e);
        }
    }
}