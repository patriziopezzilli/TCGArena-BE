package com.tcg.arena.service;

import com.tcg.arena.dto.UserRatingStreakDTO;
import com.tcg.arena.model.User;
import com.tcg.arena.model.UserRatingStreak;
import com.tcg.arena.repository.UserRatingStreakRepository;
import com.tcg.arena.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;

@Service
public class StreakService {

    // Milestone thresholds and bonus points
    private static final Map<Integer, Integer> MILESTONE_BONUSES = Map.of(
            20, 200,
            30, 350,
            40, 500,
            50, 750,
            100, 1500,
            365, 5000);

    private static final int[] MILESTONES = { 20, 30, 40, 50, 100, 365 };

    @Autowired
    private UserRatingStreakRepository streakRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RewardService rewardService;

    /**
     * Records voting activity for a user and updates their streak.
     * Should be called after each vote submission.
     * Returns bonus points awarded if a milestone was reached.
     */
    @Transactional
    public int recordActivity(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserRatingStreak streak = streakRepository.findByUser(user)
                .orElseGet(() -> {
                    UserRatingStreak newStreak = new UserRatingStreak();
                    newStreak.setUser(user);
                    return newStreak;
                });

        LocalDate today = LocalDate.now();
        LocalDate lastRatingDate = streak.getLastRatingDate();

        int bonusPointsAwarded = 0;

        // Increment total votes always
        streak.incrementTotalVotes();

        if (lastRatingDate == null) {
            // First ever rating
            streak.setCurrentStreak(1);
            streak.setLongestStreak(1);
            streak.setTotalRatingDays(1);
            streak.setStreakStartDate(today);
        } else if (lastRatingDate.equals(today)) {
            // Already rated today, no streak change
            // Just increment votes (already done above)
        } else if (lastRatingDate.equals(today.minusDays(1))) {
            // Consecutive day - increment streak
            streak.incrementStreak();
            streak.setTotalRatingDays(streak.getTotalRatingDays() + 1);

            // Check for milestone bonus
            bonusPointsAwarded = checkAndAwardMilestone(streak.getCurrentStreak(), user);
        } else {
            // Streak broken - reset
            streak.resetStreak();
            streak.setCurrentStreak(1);
            streak.setTotalRatingDays(streak.getTotalRatingDays() + 1);
            streak.setStreakStartDate(today);
        }

        streak.setLastRatingDate(today);
        streakRepository.save(streak);

        return bonusPointsAwarded;
    }

    /**
     * Gets streak statistics for a user.
     */
    public UserRatingStreakDTO getStreak(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserRatingStreak streak = streakRepository.findByUser(user).orElse(null);

        if (streak == null) {
            // Return default empty streak
            return new UserRatingStreakDTO(
                    0, 0, 0, 0, 0, null,
                    MILESTONES[0], 0, false);
        }

        LocalDate today = LocalDate.now();
        boolean ratedToday = streak.getLastRatingDate() != null && streak.getLastRatingDate().equals(today);

        // Check if streak is still valid (not broken by missing yesterday)
        int currentStreak = streak.getCurrentStreak();
        if (streak.getLastRatingDate() != null &&
                !streak.getLastRatingDate().equals(today) &&
                !streak.getLastRatingDate().equals(today.minusDays(1))) {
            // Streak is broken but not yet updated in DB
            currentStreak = 0;
        }

        int nextMilestone = calculateNextMilestone(currentStreak);
        int daysToNextMilestone = nextMilestone - currentStreak;

        return new UserRatingStreakDTO(
                currentStreak,
                streak.getLongestStreak(),
                streak.getTotalVotes(),
                streak.getStreakBreaks(),
                streak.getTotalRatingDays(),
                streak.getLastRatingDate(),
                nextMilestone,
                daysToNextMilestone,
                ratedToday);
    }

    /**
     * Checks if user reached a milestone and awards bonus points.
     */
    private int checkAndAwardMilestone(int currentStreak, User user) {
        Integer bonus = MILESTONE_BONUSES.get(currentStreak);
        if (bonus != null) {
            rewardService.earnPoints(
                    user.getId(),
                    bonus,
                    "Rating Streak Milestone: " + currentStreak + " days");
            return bonus;
        }
        return 0;
    }

    /**
     * Calculates the next milestone target for a given streak.
     */
    private int calculateNextMilestone(int currentStreak) {
        for (int milestone : MILESTONES) {
            if (currentStreak < milestone) {
                return milestone;
            }
        }
        // Beyond all milestones, return next 365 interval
        return ((currentStreak / 365) + 1) * 365;
    }
}
