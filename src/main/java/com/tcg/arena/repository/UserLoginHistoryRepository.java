package com.tcg.arena.repository;

import com.tcg.arena.model.User;
import com.tcg.arena.model.UserLoginHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserLoginHistoryRepository extends JpaRepository<UserLoginHistory, Long> {
    
    boolean existsByUserAndDeviceFingerprint(User user, String deviceFingerprint);
}
