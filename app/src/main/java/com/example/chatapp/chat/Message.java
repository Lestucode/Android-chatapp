package com.example.chatapp.chat;

public class Message {
    private String content;
    private boolean isMe;
    private boolean isFriendRequest;
    private String nickname;

    public Message(String content, boolean isMe, boolean isFriendRequest, String nickname) {
        this.content = content;
        this.isMe = isMe;
        this.isFriendRequest = isFriendRequest;
        this.nickname = nickname;
    }

    public boolean isFriendRequest() {
        return isFriendRequest;
    }

    public void setFriendRequest(boolean friendRequest) {
        isFriendRequest = friendRequest;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean isMe() {
        return isMe;
    }

    public void setMe(boolean me) {
        isMe = me;
    }
}