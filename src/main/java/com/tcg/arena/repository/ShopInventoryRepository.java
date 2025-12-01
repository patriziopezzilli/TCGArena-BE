package com.tcg.arena.repository;

import com.tcg.arena.model.ShopInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShopInventoryRepository extends JpaRepository<ShopInventory, Long> {
    List<ShopInventory> findByShopId(Long shopId);
}