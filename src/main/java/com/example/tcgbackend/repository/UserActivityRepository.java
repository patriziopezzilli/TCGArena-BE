package com.example.tcgbackend.repository;

import com.example.tcgbackend.model.UserActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserActivityRepository extends JpaRepository<UserActivity, Long> {

    List<UserActivity> findByUserIdOrderByTimestampDesc(Long userId);

    List<UserActivity> findByUserIdAndTimestampAfterOrderByTimestampDesc(Long userId, LocalDateTime since);

    List<UserActivity> findAllByOrderByTimestampDesc();

    @Query("SELECT ua FROM UserActivity ua WHERE ua.userId = :userId ORDER BY ua.timestamp DESC")
    List<UserActivity> findRecentActivities(@Param("userId") Long userId);

    @Query("SELECT ua FROM UserActivity ua WHERE ua.userId = :userId AND ua.timestamp >= :since ORDER BY ua.timestamp DESC")
    List<UserActivity> findActivitiesSince(@Param("userId") Long userId, @Param("since") LocalDateTime since);
}