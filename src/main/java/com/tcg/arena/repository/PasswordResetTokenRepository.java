package com.tcg.arena.repository;

import com.tcg.arena.model.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    
    Optional<PasswordResetToken> findByEmailAndOtpAndUsedFalse(String email, String otp);
    
    Optional<PasswordResetToken> findTopByEmailOrderByExpiryDateDesc(String email);
    
    void deleteByExpiryDateBefore(LocalDateTime dateTime);
    
    void deleteByEmail(String email);
}
