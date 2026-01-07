package com.tcg.arena.service;

import com.tcg.arena.dto.UserActivityDTO;
import com.tcg.arena.model.ActivityType;
import com.tcg.arena.model.User;
import com.tcg.arena.model.UserActivity;
import com.tcg.arena.repository.UserActivityRepository;
import com.tcg.arena.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserActivityService {

    @Autowired
    private UserActivityRepository userActivityRepository;

    @Autowired
    private UserRepository userRepository;

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
        Map<Long, User> userMap = getUserMap(activities);
        return activities.stream()
                .map(a -> convertToDTO(a, userMap))
                .collect(Collectors.toList());
    }

    public List<UserActivityDTO> getRecentUserActivities(Long userId, int limit) {
        List<UserActivity> activities = userActivityRepository.findByUserIdOrderByTimestampDesc(userId);
        Map<Long, User> userMap = getUserMap(activities);
        return activities.stream()
                .limit(limit)
                .map(a -> convertToDTO(a, userMap))
                .collect(Collectors.toList());
    }

    public List<UserActivityDTO> getRecentGlobalActivities(int limit) {
        List<UserActivity> activities = userActivityRepository.findAllByOrderByTimestampDesc();
        List<UserActivity> limitedActivities = activities.stream()
                .filter(activity -> activity.getActivityType() != ActivityType.USER_PREFERENCES_UPDATED)
                .limit(limit)
                .collect(Collectors.toList());
        Map<Long, User> userMap = getUserMap(limitedActivities);
        return limitedActivities.stream()
                .map(a -> convertToDTO(a, userMap))
                .collect(Collectors.toList());
    }

    public List<UserActivityDTO> getUserActivitiesSince(Long userId, LocalDateTime since) {
        List<UserActivity> activities = userActivityRepository.findByUserIdAndTimestampAfterOrderByTimestampDesc(userId,
                since);
        Map<Long, User> userMap = getUserMap(activities);
        return activities.stream()
                .map(a -> convertToDTO(a, userMap))
                .collect(Collectors.toList());
    }

    private Map<Long, User> getUserMap(List<UserActivity> activities) {
        List<Long> userIds = activities.stream()
                .map(UserActivity::getUserId)
                .distinct()
                .collect(Collectors.toList());
        return userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
    }

    private UserActivityDTO convertToDTO(UserActivity activity, Map<Long, User> userMap) {
        User user = userMap.get(activity.getUserId());
        String username = user != null ? user.getUsername() : "unknown";
        String displayName = user != null ? (user.getDisplayName() != null ? user.getDisplayName() : user.getUsername())
                : "Unknown";

        return new UserActivityDTO(
                activity.getId(),
                activity.getUserId(),
                username,
                displayName,
                activity.getActivityType(),
                activity.getDescription(),
                activity.getTimestamp(),
                activity.getMetadata());
    }
}