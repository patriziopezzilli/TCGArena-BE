package com.tcg.arena.dto;

public class SocialAuthRequest {
    private String idToken;
    private String provider;
    private String locale;

    public SocialAuthRequest() {
    }

    public SocialAuthRequest(String idToken, String provider) {
        this.idToken = idToken;
        this.provider = provider;
    }

    public SocialAuthRequest(String idToken, String provider, String locale) {
        this.idToken = idToken;
        this.provider = provider;
        this.locale = locale;
    }

    public String getIdToken() {
        return idToken;
    }

    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }
}
