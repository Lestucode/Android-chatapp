package com.example.chatapp;

public class User {
    private String email;
    private String nickname;
    private String token;
    private String userId;
    private String avatar_url;
    private String signature;



    public User(String email, String nickname, String userId, String token) {
        this.email = email;
        this.nickname = nickname;
        this.userId = userId;
        this.token = token;
    }

    public User(String email, String nickname, String token, String userId, String avatar_url, String signature) {
        this.email = email;
        this.nickname = nickname;
        this.token = token;
        this.userId = userId;
        this.avatar_url = avatar_url;
        this.signature = signature;
    }

    public User(){}

    public String getAvatar_url() {
        return avatar_url;
    }

    public void setAvatar_url(String avatar_url) {
        this.avatar_url = avatar_url;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    @Override
    public String toString() {
        return "User{" +
                "email='" + email + '\'' +
                ", nickname='" + nickname + '\'' +
                ", userId=" + userId +
                ", token='" + token + '\'' +
                ", avatar_url='" + avatar_url + '\'' +
                ", signature='" + signature + '\'' +
                '}';
    }
}
