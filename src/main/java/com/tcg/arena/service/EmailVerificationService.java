package com.tcg.arena.service;

import com.tcg.arena.model.EmailVerificationToken;
import com.tcg.arena.model.User;
import com.tcg.arena.repository.EmailVerificationTokenRepository;
import com.tcg.arena.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

@Service
public class EmailVerificationService {

    private static final Logger logger = LoggerFactory.getLogger(EmailVerificationService.class);
    
    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    
    @Value("${app.frontend.url:https://tcgarena.it}")
    private String frontendUrl;

    public EmailVerificationService(EmailVerificationTokenRepository tokenRepository,
                                    UserRepository userRepository,
                                    EmailService emailService) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    /**
     * Create and send email verification token
     */
    @Transactional
    public void sendVerificationEmail(User user) {
        // Generate verification code (6 digits)
        String code = String.format("%06d", new Random().nextInt(999999));
        
        // Generate unique token for link
        String token = UUID.randomUUID().toString();
        
        // Create verification token entity
        EmailVerificationToken verificationToken = new EmailVerificationToken();
        verificationToken.setUser(user);
        verificationToken.setToken(token);
        verificationToken.setExpiryDate(LocalDateTime.now().plusHours(24));
        verificationToken.setVerified(false);
        
        tokenRepository.save(verificationToken);
        
        // Build verification link
        String verificationLink = frontendUrl + "/verify-email?token=" + token;
        
        // Send email
        emailService.sendEmailVerification(user.getEmail(), user.getUsername(), code, verificationLink);
        
        logger.info("Verification email sent to user: {}", user.getUsername());
    }

    /**
     * Verify email using token
     */
    @Transactional
    public boolean verifyEmail(String token) {
        Optional<EmailVerificationToken> optionalToken = tokenRepository.findByToken(token);
        
        if (optionalToken.isEmpty()) {
            logger.warn("Verification token not found: {}", token);
            return false;
        }
        
        EmailVerificationToken verificationToken = optionalToken.get();
        
        // Check if already verified
        if (verificationToken.isVerified()) {
            logger.info("Token already verified: {}", token);
            return true;
        }
        
        // Check if expired
        if (verificationToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            logger.warn("Verification token expired: {}", token);
            return false;
        }
        
        // Mark as verified
        verificationToken.setVerified(true);
        tokenRepository.save(verificationToken);
        
        // Update user email verified status (assuming User has emailVerified field)
        User user = verificationToken.getUser();
        // user.setEmailVerified(true); // Uncomment if User entity has this field
        userRepository.save(user);
        
        logger.info("Email verified for user: {}", user.getUsername());
        return true;
    }

    /**
     * Resend verification email
     */
    @Transactional
    public void resendVerificationEmail(User user) {
        // Invalidate old tokens
        tokenRepository.deleteByUserAndVerifiedFalse(user);
        
        // Send new verification email
        sendVerificationEmail(user);
    }
}
