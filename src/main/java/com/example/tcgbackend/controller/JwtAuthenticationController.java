package com.example.tcgbackend.controller;

import com.example.tcgbackend.model.User;
import com.example.tcgbackend.security.JwtTokenUtil;
import com.example.tcgbackend.security.JwtUserDetailsService;
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
    private PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<?> createAuthenticationToken(@RequestBody JwtRequest authenticationRequest) throws Exception {
        authenticate(authenticationRequest.getUsername(), authenticationRequest.getPassword());

        final UserDetails userDetails = userDetailsService.loadUserByUsername(authenticationRequest.getUsername());
        final String token = jwtTokenUtil.generateToken(userDetails);

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("user", userService.getUserByUsername(authenticationRequest.getUsername()).orElse(null));

        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody User user) throws Exception {
        if (userService.getUserByUsername(user.getUsername()).isPresent() ||
                userService.getUserByEmail(user.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body("Username or email already exists");
        }

        System.out.println("DEBUG: Registering user: " + user.getUsername());
        System.out.println("DEBUG: Original password length: "
                + (user.getPassword() != null ? user.getPassword().length() : "null"));
        String encodedPassword = passwordEncoder.encode(user.getPassword());
        System.out.println("DEBUG: Encoded password: " + encodedPassword);
        user.setPassword(encodedPassword);
        user.setDateJoined(LocalDateTime.now());
        user.setDisplayName(user.getUsername());
        User savedUser = userService.saveUser(user);
        System.out.println("DEBUG: User saved with password: " + savedUser.getPassword());

        final UserDetails userDetails = userDetailsService.loadUserByUsername(savedUser.getUsername());
        final String token = jwtTokenUtil.generateToken(userDetails);

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("user", savedUser);

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