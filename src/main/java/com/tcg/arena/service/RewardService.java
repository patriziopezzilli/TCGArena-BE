package com.tcg.arena.service;

import com.tcg.arena.model.Reward;
import com.tcg.arena.model.RewardTransaction;
import com.tcg.arena.model.User;
import com.tcg.arena.repository.RewardRepository;
import com.tcg.arena.repository.RewardTransactionRepository;
import com.tcg.arena.repository.UserRepository;
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

    public List<Reward> getRewardsByPartner(Long partnerId) {
        return rewardRepository.findByPartnerId(partnerId);
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
                        com.tcg.arena.model.ActivityType.REWARD_REDEEMED,
                        "Riscattato premio: " + reward.getName());

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
                    com.tcg.arena.model.ActivityType.POINTS_EARNED,
                    "Guadagnati " + points + " punti: " + description);
        }
    }

    public List<RewardTransaction> getUserTransactionHistory(Long userId) {
        return transactionRepository.findByUserIdOrderByTimestampDesc(userId);
    }

    public Integer getUserPoints(Long userId) {
        Optional<User> user = userRepository.findById(userId);
        return user.map(User::getPoints).orElse(0);
    }

    // ========== ADMIN: Transaction Management Methods ==========

    public List<RewardTransaction> getAllRewardTransactions() {
        return transactionRepository.findAllByOrderByTimestampDesc();
    }

    public List<RewardTransaction> getPendingFulfillments() {
        return transactionRepository.findByRewardIdIsNotNullAndStatusIn(
                List.of(
                        RewardTransaction.RewardFulfillmentStatus.PENDING,
                        RewardTransaction.RewardFulfillmentStatus.PROCESSING));
    }

    public RewardTransaction updateTransaction(Long transactionId, String status, String voucherCode,
            String trackingNumber) {
        RewardTransaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        if (status != null && !status.isEmpty()) {
            transaction.setStatus(RewardTransaction.RewardFulfillmentStatus.valueOf(status));
        }
        if (voucherCode != null) {
            transaction.setVoucherCode(voucherCode);
        }
        if (trackingNumber != null) {
            transaction.setTrackingNumber(trackingNumber);
        }

        return transactionRepository.save(transaction);
    }
}