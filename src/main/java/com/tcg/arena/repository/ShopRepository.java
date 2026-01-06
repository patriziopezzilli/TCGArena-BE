package com.tcg.arena.repository;

import com.tcg.arena.model.Shop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShopRepository extends JpaRepository<Shop, Long> {
    List<Shop> findByActiveTrue();

    Optional<Shop> findByOwnerId(Long ownerId);

    List<Shop> findByNameContainingIgnoreCaseAndIsVerifiedFalseAndOwnerIdIsNull(String name);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE Shop s SET s.active = :active")
    void updateAllActive(boolean active);
}