package com.tcg.arena.service;

import com.tcg.arena.dto.UserActivityDTO;
import com.tcg.arena.model.ActivityType;
import com.tcg.arena.model.UserActivity;
import com.tcg.arena.repository.UserActivityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserActivityService {

    @Autowired
    private UserActivityRepository userActivityRepository;

    @Transactional
    public void logActivity(Long userId, ActivityType activityType, String description) {
        logActivity(userId, activityType, description, null);
    }

    @Transactional
    public void logActivity(Long userId, ActivityType activityType, String description, String metadata) {
        UserActivity activity = new UserActivity();
        activity.setUserId(userId);
        activity.setActivityType(activityType);
        activity.setDescription(description);
        activity.setTimestamp(LocalDateTime.now());
        activity.setMetadata(metadata);

        userActivityRepository.save(activity);
    }

    public List<UserActivityDTO> getUserActivities(Long userId) {
        List<UserActivity> activities = userActivityRepository.findByUserIdOrderByTimestampDesc(userId);
        return activities.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<UserActivityDTO> getRecentUserActivities(Long userId, int limit) {
        List<UserActivity> activities = userActivityRepository.findByUserIdOrderByTimestampDesc(userId);
        return activities.stream()
                .limit(limit)
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<UserActivityDTO> getRecentGlobalActivities(int limit) {
        List<UserActivity> activities = userActivityRepository.findAllByOrderByTimestampDesc();
        return activities.stream()
                .limit(limit)
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<UserActivityDTO> getUserActivitiesSince(Long userId, LocalDateTime since) {
        List<UserActivity> activities = userActivityRepository.findByUserIdAndTimestampAfterOrderByTimestampDesc(userId, since);
        return activities.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private UserActivityDTO convertToDTO(UserActivity activity) {
        return new UserActivityDTO(
                activity.getId(),
                activity.getActivityType(),
                activity.getDescription(),
                activity.getTimestamp(),
                activity.getMetadata()
        );
    }
}