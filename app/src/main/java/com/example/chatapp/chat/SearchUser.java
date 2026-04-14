package com.example.chatapp.chat;

public class SearchUser {
    private Long id;
    private String account;
    private String nickname;
    private String avatar;

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SearchUser(Long id, String account, String nickname, String avatar) {
        this.id = id;
        this.account = account;
        this.nickname = nickname;
        this.avatar = avatar;
    }
    public SearchUser(){

    }
}
