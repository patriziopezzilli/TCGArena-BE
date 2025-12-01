package com.tcg.arena.dto;

import com.tcg.arena.model.Shop;
import com.tcg.arena.model.User;

public class MerchantRegistrationResponseDTO {
    private User user;
    private Shop shop;
    private String token;

    public MerchantRegistrationResponseDTO() {}

    public MerchantRegistrationResponseDTO(User user, Shop shop, String token) {
        this.user = user;
        this.shop = shop;
        this.token = token;
    }

    // Getters and Setters
    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Shop getShop() {
        return shop;
    }

    public void setShop(Shop shop) {
        this.shop = shop;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
