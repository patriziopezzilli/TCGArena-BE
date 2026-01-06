package com.tcg.arena.service;

import com.tcg.arena.model.*;
import com.tcg.arena.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ShopRewardService {

    @Autowired
    private ShopRewardRepository shopRewardRepository;

    @Autowired
    private ShopRewardRedemptionRepository redemptionRepository;

    @Autowired
    private ShopRepository shopRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RewardService rewardService; // For point management

    @Autowired
    private NotificationService notificationService;

    // ==================== MERCHANT OPERATIONS ====================

    @Transactional
    public ShopReward createReward(Long shopId, ShopReward reward) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new RuntimeException("Shop not found"));

        reward.setShop(shop);
        reward.setClaimedCount(0);
        reward.setIsActive(true);

        ShopReward savedReward = shopRewardRepository.save(reward);

        // If this is the shop's first reward, make them a partner
        if (!shop.getIsPartner()) {
            shop.setIsPartner(true);
            shopRepository.save(shop);
        }

        return savedReward;
    }

    public ShopReward updateReward(Long rewardId, Long shopId, ShopReward updatedReward) {
        ShopReward existing = shopRewardRepository.findById(rewardId)
                .orElseThrow(() -> new RuntimeException("Reward not found"));

        // Verify ownership
        if (!existing.getShop().getId().equals(shopId)) {
            throw new RuntimeException("Unauthorized: Reward does not belong to this shop");
        }

        existing.setName(updatedReward.getName());
        existing.setDescription(updatedReward.getDescription());
        existing.setCostPoints(updatedReward.getCostPoints());
        existing.setType(updatedReward.getType());
        existing.setDiscountValue(updatedReward.getDiscountValue());
        existing.setIsPercentage(updatedReward.getIsPercentage());
        existing.setProductName(updatedReward.getProductName());
        existing.setStockQuantity(updatedReward.getStockQuantity());
        existing.setExpiresAt(updatedReward.getExpiresAt());

        return shopRewardRepository.save(existing);
    }

    public void toggleRewardActive(Long rewardId, Long shopId, boolean active) {
        ShopReward reward = shopRewardRepository.findById(rewardId)
                .orElseThrow(() -> new RuntimeException("Reward not found"));

        if (!reward.getShop().getId().equals(shopId)) {
            throw new RuntimeException("Unauthorized");
        }

        reward.setIsActive(active);
        shopRewardRepository.save(reward);
    }

    public void deleteReward(Long rewardId, Long shopId) {
        ShopReward reward = shopRewardRepository.findById(rewardId)
                .orElseThrow(() -> new RuntimeException("Reward not found"));

        if (!reward.getShop().getId().equals(shopId)) {
            throw new RuntimeException("Unauthorized");
        }

        // Soft delete - just deactivate
        reward.setIsActive(false);
        shopRewardRepository.save(reward);
    }

    public List<ShopReward> getShopRewards(Long shopId) {
        return shopRewardRepository.findByShopId(shopId);
    }

    public List<ShopReward> getActiveShopRewards(Long shopId) {
        return shopRewardRepository.findAvailableByShopId(shopId);
    }

    // ==================== USER REDEMPTION ====================

    @Transactional
    public ShopRewardRedemption redeemReward(Long rewardId, Long userId) {
        ShopReward reward = shopRewardRepository.findById(rewardId)
                .orElseThrow(() -> new RuntimeException("Reward not found"));

        if (!reward.isAvailable()) {
            throw new RuntimeException("Reward is not available");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check user points
        int userPoints = rewardService.getUserPoints(userId);
        if (userPoints < reward.getCostPoints()) {
            throw new RuntimeException("Insufficient points");
        }

        // Deduct points
        rewardService.deductPoints(userId, reward.getCostPoints(), 
            "Riscatto premio: " + reward.getName() + " @ " + reward.getShop().getName());

        // Update claimed count
        reward.setClaimedCount(reward.getClaimedCount() + 1);
        shopRewardRepository.save(reward);

        // Create redemption record
        ShopRewardRedemption redemption = new ShopRewardRedemption();
        redemption.setShopReward(reward);
        redemption.setUser(user);
        redemption.setPointsSpent(reward.getCostPoints());
        redemption.setStatus(RedemptionStatus.PENDING);
        redemption.setRedemptionCode(generateRedemptionCode());

        return redemptionRepository.save(redemption);
    }

    public List<ShopRewardRedemption> getUserRedemptions(Long userId) {
        return redemptionRepository.findByUserIdOrderByRedeemedAtDesc(userId);
    }

    // ==================== MERCHANT FULFILLMENT ====================

    public List<ShopRewardRedemption> getShopRedemptions(Long shopId) {
        return redemptionRepository.findByShopId(shopId);
    }

    public List<ShopRewardRedemption> getPendingRedemptions(Long shopId) {
        return redemptionRepository.findByShopIdAndStatus(shopId, RedemptionStatus.PENDING);
    }

    @Transactional
    public ShopRewardRedemption fulfillRedemption(Long redemptionId, Long shopId, 
            String voucherCode, String trackingCode, String notes) {
        ShopRewardRedemption redemption = redemptionRepository.findById(redemptionId)
                .orElseThrow(() -> new RuntimeException("Redemption not found"));

        // Verify ownership
        if (!redemption.getShopReward().getShop().getId().equals(shopId)) {
            throw new RuntimeException("Unauthorized");
        }

        if (redemption.getStatus() != RedemptionStatus.PENDING) {
            throw new RuntimeException("Redemption is not pending");
        }

        redemption.setStatus(RedemptionStatus.FULFILLED);
        redemption.setVoucherCode(voucherCode);
        redemption.setTrackingCode(trackingCode);
        redemption.setMerchantNotes(notes);
        redemption.setFulfilledAt(LocalDateTime.now());

        ShopRewardRedemption saved = redemptionRepository.save(redemption);

        // Send push notification to user
        try {
            notificationService.sendRewardFulfilledNotification(
                redemption.getUser().getId(),
                redemption.getShopReward().getName(),
                redemption.getShopReward().getShop().getName()
            );
        } catch (Exception e) {
            // Log but don't fail the operation
            System.err.println("Failed to send fulfilled notification: " + e.getMessage());
        }

        return saved;
    }

    @Transactional
    public ShopRewardRedemption cancelRedemption(Long redemptionId, Long shopId) {
        ShopRewardRedemption redemption = redemptionRepository.findById(redemptionId)
                .orElseThrow(() -> new RuntimeException("Redemption not found"));

        if (!redemption.getShopReward().getShop().getId().equals(shopId)) {
            throw new RuntimeException("Unauthorized");
        }

        if (redemption.getStatus() != RedemptionStatus.PENDING) {
            throw new RuntimeException("Can only cancel pending redemptions");
        }

        // Refund points
        rewardService.addPoints(redemption.getUser().getId(), redemption.getPointsSpent(),
            "Rimborso premio annullato: " + redemption.getShopReward().getName());

        // Restore stock
        ShopReward reward = redemption.getShopReward();
        reward.setClaimedCount(Math.max(0, reward.getClaimedCount() - 1));
        shopRewardRepository.save(reward);

        redemption.setStatus(RedemptionStatus.CANCELLED);
        ShopRewardRedemption saved = redemptionRepository.save(redemption);

        // Send push notification to user
        try {
            notificationService.sendRewardCancelledNotification(
                redemption.getUser().getId(),
                redemption.getShopReward().getName(),
                redemption.getShopReward().getShop().getName(),
                redemption.getPointsSpent()
            );
        } catch (Exception e) {
            // Log but don't fail the operation
            System.err.println("Failed to send cancelled notification: " + e.getMessage());
        }

        return saved;
    }

    public Optional<ShopRewardRedemption> findByRedemptionCode(String code) {
        return redemptionRepository.findByRedemptionCode(code);
    }

    // ==================== PUBLIC / APP ====================

    public List<ShopReward> getAllAvailableRewards() {
        return shopRewardRepository.findAllAvailable();
    }

    public List<Long> getPartnerShopIds() {
        return shopRewardRepository.findShopIdsWithActiveRewards();
    }

    // ==================== HELPERS ====================

    private String generateRedemptionCode() {
        return "TCG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
