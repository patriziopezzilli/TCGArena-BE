package com.tcg.arena.controller;

import com.tcg.arena.dto.MerchantRegistrationRequestDTO;
import com.tcg.arena.dto.MerchantRegistrationResponseDTO;
import com.tcg.arena.dto.RefreshTokenRequest;
import com.tcg.arena.dto.RefreshTokenResponse;
import com.tcg.arena.dto.RegisterRequestDTO;
import com.tcg.arena.model.PasswordResetToken;
import com.tcg.arena.model.Shop;
import com.tcg.arena.model.ShopType;
import com.tcg.arena.model.User;
import com.tcg.arena.repository.PasswordResetTokenRepository;
import com.tcg.arena.security.JwtTokenUtil;
import com.tcg.arena.security.JwtUserDetailsService;
import com.tcg.arena.service.DeckService;
import com.tcg.arena.service.EmailService;
import com.tcg.arena.service.ShopService;
import com.tcg.arena.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class JwtAuthenticationController {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationController.class);

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

    @Autowired
    private DeckService deckService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private com.tcg.arena.service.SecurityAlertService securityAlertService;

    @Autowired
    private com.tcg.arena.service.EmailVerificationService emailVerificationService;

    @PostMapping("/login")
    public ResponseEntity<?> createAuthenticationToken(@RequestBody JwtRequest authenticationRequest, 
                                                       jakarta.servlet.http.HttpServletRequest request) throws Exception {
        authenticate(authenticationRequest.getUsername(), authenticationRequest.getPassword());

        final UserDetails userDetails = userDetailsService.loadUserByUsername(authenticationRequest.getUsername());
        final String token = jwtTokenUtil.generateToken(userDetails);
        final String refreshToken = jwtTokenUtil.generateRefreshToken(userDetails);

        // Track login for security alerts
        User user = userService.getUserByUsername(authenticationRequest.getUsername()).orElse(null);
        if (user != null) {
            try {
                securityAlertService.trackLoginAndNotify(user, request);
            } catch (Exception e) {
                logger.error("Failed to track login for user: {}", user.getUsername(), e);
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("refreshToken", refreshToken);
        response.put("user", user);

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
        user.setDisplayName(registerRequest.getDisplayName() != null ? registerRequest.getDisplayName()
                : registerRequest.getUsername());
        user.setPassword(registerRequest.getPassword());
        user.setDateJoined(LocalDateTime.now());

        // Set favorite TCGs from the list
        if (registerRequest.getFavoriteGames() != null && !registerRequest.getFavoriteGames().isEmpty()) {
            user.setFavoriteTCGTypes(registerRequest.getFavoriteGames());
        }

        // Set location from registration (GPS data from onboarding)
        if (registerRequest.getLatitude() != null && registerRequest.getLongitude() != null) {
            com.tcg.arena.model.UserLocation location = new com.tcg.arena.model.UserLocation();
            location.setLatitude(registerRequest.getLatitude());
            location.setLongitude(registerRequest.getLongitude());
            location.setCity(registerRequest.getCity());
            location.setCountry(registerRequest.getCountry());
            user.setLocation(location);
            logger.debug("Setting user location: {}, {} ({}, {})",
                    registerRequest.getCity(), registerRequest.getCountry(),
                    registerRequest.getLatitude(), registerRequest.getLongitude());
        }

        logger.debug("Registering user: {}", user.getUsername());
        logger.debug("Original password length: {}", user.getPassword() != null ? user.getPassword().length() : "null");
        String encodedPassword = passwordEncoder.encode(user.getPassword());
        logger.debug("Encoded password: {}", encodedPassword);
        user.setPassword(encodedPassword);

        User savedUser = userService.saveUser(user);
        logger.debug("User saved with ID: {}", savedUser.getId());

        // Send welcome email
        try {
            emailService.sendWelcomeEmail(savedUser);
            logger.info("Welcome email sent to: {}", savedUser.getEmail());
        } catch (Exception e) {
            logger.error("Failed to send welcome email to: {}", savedUser.getEmail(), e);
            // Don't fail registration if email fails
        }

        // Create starter decks for favorite TCG types
        if (registerRequest.getFavoriteGames() != null && !registerRequest.getFavoriteGames().isEmpty()) {
            deckService.createStarterDecksForUser(savedUser.getId(), registerRequest.getFavoriteGames());
        }

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

        Shop shopToClaim = null;
        if (request.getExistingShopId() != null) {
            // Verify shop is claimable
            Shop existingShop = shopService.getShopById(request.getExistingShopId())
                    .orElseThrow(() -> new Exception("Shop not found"));

            if (Boolean.TRUE.equals(existingShop.getIsVerified())) {
                return ResponseEntity.badRequest().body("Shop is already verified and cannot be claimed");
            }
            if (existingShop.getOwnerId() != null) {
                return ResponseEntity.badRequest().body("Shop is already claimed");
            }
            shopToClaim = existingShop;
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

        Shop savedShop;
        if (shopToClaim != null) {
            // Claim existing shop
            shopToClaim.setOwnerId(savedUser.getId());
            // Optionally update contact info if provided
            if (request.getPhone() != null && !request.getPhone().isEmpty()) {
                shopToClaim.setPhoneNumber(request.getPhone());
            }
            // Keep verification as false
            savedShop = shopService.saveShop(shopToClaim);
        } else {
            // Create New Shop
            Shop shop = new Shop();
            shop.setName(request.getShopName());
            shop.setDescription(request.getDescription());
            shop.setAddress(request.getAddress() + ", " + request.getCity() + " " + request.getZipCode());
            shop.setPhoneNumber(request.getPhone());
            shop.setType(ShopType.LOCAL_STORE);
            shop.setIsVerified(false);
            shop.setOwnerId(savedUser.getId());
            savedShop = shopService.saveShop(shop);
        }

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

    @PostMapping("/create-admin")
    public ResponseEntity<?> createAdminUser(@RequestBody Map<String, String> adminRequest) throws Exception {
        String username = adminRequest.get("username");
        String password = adminRequest.get("password");
        String email = adminRequest.get("email");

        if (username == null || password == null || email == null) {
            return ResponseEntity.badRequest().body("Username, password, and email are required");
        }

        if (userService.getUserByUsername(username).isPresent()) {
            return ResponseEntity.badRequest().body("Username already exists");
        }

        // Create admin user
        User adminUser = new User();
        adminUser.setUsername(username);
        adminUser.setEmail(email);
        adminUser.setDisplayName(username);
        adminUser.setPassword(passwordEncoder.encode(password));
        adminUser.setDateJoined(LocalDateTime.now());
        adminUser.setIsAdmin(true);
        adminUser.setIsMerchant(false);
        adminUser.setPoints(0);

        User savedUser = userService.saveUser(adminUser);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Admin user created successfully");
        response.put("user", savedUser);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        
        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email is required"));
        }

        Optional<User> userOpt = userService.getUserByEmail(email);
        if (!userOpt.isPresent()) {
            // Per sicurezza, non rivelare se l'email esiste o meno
            return ResponseEntity.ok(Map.of("message", "If the email exists, an OTP has been sent"));
        }

        try {
            // Genera OTP a 6 cifre
            String otp = generateOTP();
            
            // Elimina eventuali token precedenti per questa email
            passwordResetTokenRepository.deleteByEmail(email);
            
            // Salva nuovo token
            PasswordResetToken token = new PasswordResetToken(email, otp);
            passwordResetTokenRepository.save(token);
            
            // Invia email
            emailService.sendOtpEmail(email, otp);
            
            logger.info("Password reset OTP sent to: {}", email);
            return ResponseEntity.ok(Map.of("message", "OTP sent to your email"));
        } catch (Exception e) {
            logger.error("Error sending password reset OTP", e);
            return ResponseEntity.internalServerError().body(Map.of("message", "Failed to send OTP"));
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String otp = request.get("otp");

        if (email == null || otp == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email and OTP are required"));
        }

        Optional<PasswordResetToken> tokenOpt = passwordResetTokenRepository.findByEmailAndOtpAndUsedFalse(email, otp);
        
        if (!tokenOpt.isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid or expired OTP"));
        }

        PasswordResetToken token = tokenOpt.get();
        if (token.isExpired()) {
            return ResponseEntity.badRequest().body(Map.of("message", "OTP has expired"));
        }

        return ResponseEntity.ok(Map.of("message", "OTP verified successfully"));
    }

    @PostMapping("/reset-password")
    @Transactional
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String otp = request.get("otp");
        String newPassword = request.get("newPassword");

        if (email == null || otp == null || newPassword == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email, OTP and new password are required"));
        }

        if (newPassword.length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("message", "Password must be at least 6 characters"));
        }

        Optional<PasswordResetToken> tokenOpt = passwordResetTokenRepository.findByEmailAndOtpAndUsedFalse(email, otp);
        
        if (!tokenOpt.isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid or expired OTP"));
        }

        PasswordResetToken token = tokenOpt.get();
        if (token.isExpired()) {
            return ResponseEntity.badRequest().body(Map.of("message", "OTP has expired"));
        }

        Optional<User> userOpt = userService.getUserByEmail(email);
        if (!userOpt.isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("message", "User not found"));
        }

        User user = userOpt.get();
        user.setPassword(passwordEncoder.encode(newPassword));
        userService.saveUser(user);

        // Marca il token come usato
        token.setUsed(true);
        passwordResetTokenRepository.save(token);

        logger.info("Password reset successful for user: {}", email);
        return ResponseEntity.ok(Map.of("message", "Password reset successfully"));
    }

    /**
     * Verify email using token
     */
    @PostMapping("/verify-email")
    @Operation(summary = "Verify email", description = "Verifies user email using token from verification link")
    public ResponseEntity<?> verifyEmail(@RequestParam String token) {
        boolean verified = emailVerificationService.verifyEmail(token);
        if (verified) {
            return ResponseEntity.ok(Map.of("message", "Email verified successfully", "success", true));
        }
        return ResponseEntity.badRequest().body(Map.of("message", "Invalid or expired verification token", "success", false));
    }

    /**
     * Resend email verification
     */
    @PostMapping("/resend-verification")
    @Operation(summary = "Resend verification email", description = "Resends verification email to user")
    public ResponseEntity<?> resendVerification(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email is required"));
        }

        Optional<User> userOpt = userService.getUserByEmail(email);
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "User not found"));
        }

        try {
            emailVerificationService.resendVerificationEmail(userOpt.get());
            return ResponseEntity.ok(Map.of("message", "Verification email sent successfully"));
        } catch (Exception e) {
            logger.error("Failed to resend verification email to: {}", email, e);
            return ResponseEntity.status(500).body(Map.of("message", "Failed to send verification email"));
        }
    }

    private String generateOTP() {
        SecureRandom random = new SecureRandom();
        int otp = 100000 + random.nextInt(900000); // Genera numero tra 100000 e 999999
        return String.valueOf(otp);
    }

    private void authenticate(String username, String password) throws Exception {
        try {
            logger.debug("Attempting to authenticate user: {}", username);
            logger.debug("Password length: {}", password != null ? password.length() : "null");
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
            logger.debug("Authentication successful for user: {}", username);
        } catch (DisabledException e) {
            logger.debug("User disabled: {}", username);
            throw new Exception("USER_DISABLED", e);
        } catch (BadCredentialsException e) {
            logger.debug("Bad credentials for user: {}", username);
            throw new Exception("INVALID_CREDENTIALS", e);
        }
    }
}