package com.tcg.arena.service;

import com.tcg.arena.model.Achievement;
import com.tcg.arena.model.UserAchievement;
import com.tcg.arena.repository.AchievementRepository;
import com.tcg.arena.repository.UserAchievementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AchievementService {

    @Autowired
    private AchievementRepository achievementRepository;

    @Autowired
    private UserAchievementRepository userAchievementRepository;

    @Autowired
    private RewardService rewardService;

    public List<Achievement> getAllActiveAchievements() {
        return achievementRepository.findByIsActiveTrue();
    }

    public Optional<Achievement> getAchievementById(Long id) {
        return achievementRepository.findById(id);
    }

    public Achievement saveAchievement(Achievement achievement) {
        if (achievement.getCreatedAt() == null) {
            achievement.setCreatedAt(LocalDateTime.now());
        }
        return achievementRepository.save(achievement);
    }

    public List<UserAchievement> getUserAchievements(Long userId) {
        return userAchievementRepository.findByUserId(userId);
    }

    public boolean unlockAchievement(Long userId, Long achievementId) {
        if (userAchievementRepository.existsByUserIdAndAchievementId(userId, achievementId)) {
            return false; // Already unlocked
        }

        Optional<Achievement> achievementOpt = achievementRepository.findById(achievementId);
        if (achievementOpt.isPresent()) {
            Achievement achievement = achievementOpt.get();

            UserAchievement userAchievement = new UserAchievement();
            userAchievement.setUserId(userId);
            userAchievement.setAchievementId(achievementId);
            userAchievement.setUnlockedAt(LocalDateTime.now());
            userAchievementRepository.save(userAchievement);

            // Award points
            rewardService.earnPoints(userId, achievement.getPointsReward(),
                "Achievement unlocked: " + achievement.getName());

            return true;
        }
        return false;
    }

    // Method to check and unlock achievements based on user actions
    public void checkAchievements(Long userId, String action) {
        // This would be called from other services when certain actions happen
        // For example, when a user wins a tournament, call checkAchievements(userId, "tournament_win")
        // Then check criteria and unlock if met
        // For now, placeholder
    }
}