package com.example.tcgbackend.service;

import com.example.tcgbackend.model.Reward;
import com.example.tcgbackend.model.RewardTransaction;
import com.example.tcgbackend.model.User;
import com.example.tcgbackend.repository.RewardRepository;
import com.example.tcgbackend.repository.RewardTransactionRepository;
import com.example.tcgbackend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class RewardService {

    @Autowired
    private RewardRepository rewardRepository;

    @Autowired
    private RewardTransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserActivityService userActivityService;

    public List<Reward> getAllActiveRewards() {
        return rewardRepository.findByIsActiveTrue();
    }

    public Optional<Reward> getRewardById(Long id) {
        return rewardRepository.findById(id);
    }

    public Reward saveReward(Reward reward) {
        if (reward.getCreatedAt() == null) {
            reward.setCreatedAt(LocalDateTime.now());
        }
        return rewardRepository.save(reward);
    }

    public boolean redeemReward(Long userId, Long rewardId) {
        Optional<User> userOpt = userRepository.findById(userId);
        Optional<Reward> rewardOpt = rewardRepository.findById(rewardId);

        if (userOpt.isPresent() && rewardOpt.isPresent()) {
            User user = userOpt.get();
            Reward reward = rewardOpt.get();

            if (user.getPoints() >= reward.getCostPoints()) {
                // Deduct points
                user.setPoints(user.getPoints() - reward.getCostPoints());
                userRepository.save(user);

                // Log transaction
                RewardTransaction transaction = new RewardTransaction();
                transaction.setUserId(userId);
                transaction.setPointsChange(-reward.getCostPoints());
                transaction.setDescription("Redeemed: " + reward.getName());
                transaction.setRewardId(rewardId);
                transaction.setTimestamp(LocalDateTime.now());
                transactionRepository.save(transaction);

                // Log activity
                userActivityService.logActivity(userId,
                    com.example.tcgbackend.model.ActivityType.REWARD_REDEEMED,
                    "Redeemed reward: " + reward.getName());

                return true;
            }
        }
        return false;
    }

    public void earnPoints(Long userId, Integer points, String description) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setPoints(user.getPoints() + points);
            userRepository.save(user);

            // Log transaction
            RewardTransaction transaction = new RewardTransaction();
            transaction.setUserId(userId);
            transaction.setPointsChange(points);
            transaction.setDescription(description);
            transaction.setTimestamp(LocalDateTime.now());
            transactionRepository.save(transaction);

            // Log activity
            userActivityService.logActivity(userId,
                com.example.tcgbackend.model.ActivityType.POINTS_EARNED,
                "Earned " + points + " points: " + description);
        }
    }

    public List<RewardTransaction> getUserTransactionHistory(Long userId) {
        return transactionRepository.findByUserIdOrderByTimestampDesc(userId);
    }

    public Integer getUserPoints(Long userId) {
        Optional<User> user = userRepository.findById(userId);
        return user.map(User::getPoints).orElse(0);
    }
}