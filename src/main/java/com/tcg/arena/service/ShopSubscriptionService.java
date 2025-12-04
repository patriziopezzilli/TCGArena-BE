package com.tcg.arena.service;

import com.tcg.arena.model.ShopSubscription;
import com.tcg.arena.model.User;
import com.tcg.arena.repository.ShopSubscriptionRepository;
import com.tcg.arena.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ShopSubscriptionService {

    @Autowired
    private ShopSubscriptionRepository subscriptionRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public ShopSubscription subscribeToShop(Long userId, Long shopId) {
        // Check if already subscribed
        Optional<ShopSubscription> existingSubscription = subscriptionRepository
            .findByUserIdAndShopIdAndIsActiveTrue(userId, shopId);

        if (existingSubscription.isPresent()) {
            return existingSubscription.get();
        }

        // Create new subscription
        ShopSubscription subscription = new ShopSubscription();
        subscription.setUserId(userId);
        subscription.setShopId(shopId);
        subscription.setSubscribedAt(LocalDateTime.now());
        subscription.setIsActive(true);

        ShopSubscription savedSubscription = subscriptionRepository.save(subscription);

        // Note: Welcome notification can be sent from controller layer if needed

        return savedSubscription;
    }

    @Transactional
    public boolean unsubscribeFromShop(Long userId, Long shopId) {
        Optional<ShopSubscription> subscription = subscriptionRepository
            .findByUserIdAndShopIdAndIsActiveTrue(userId, shopId);

        if (subscription.isPresent()) {
            ShopSubscription sub = subscription.get();
            sub.setIsActive(false);
            subscriptionRepository.save(sub);

            // Note: Unsubscribe notification can be sent from controller layer if needed

            return true;
        }

        return false;
    }

    public boolean isUserSubscribedToShop(Long userId, Long shopId) {
        return subscriptionRepository.existsByUserIdAndShopIdAndIsActiveTrue(userId, shopId);
    }

    public List<ShopSubscription> getUserSubscriptions(Long userId) {
        return subscriptionRepository.findByUserIdAndIsActiveTrue(userId);
    }

    public List<ShopSubscription> getShopSubscribers(Long shopId) {
        return subscriptionRepository.findByShopIdAndIsActiveTrue(shopId);
    }

    public Long getSubscriberCount(Long shopId) {
        return subscriptionRepository.countActiveSubscriptionsByShopId(shopId);
    }

    public List<User> getShopSubscriberUsers(Long shopId) {
        List<ShopSubscription> subscriptions = getShopSubscribers(shopId);
        return subscriptions.stream()
            .map(sub -> userRepository.findById(sub.getUserId()))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .toList();
    }
}